package com.sunnysuperman.job;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Scheduler {
    private static final Logger LOG = LoggerFactory.getLogger(Scheduler.class);

    private static class ScheduleJob {
        private final String id;
        private final Class<?> jobClass;
        private final CronExpression crontab;

        public ScheduleJob(String id, Class<?> jobClass, CronExpression crontab) {
            super();
            this.id = id;
            this.jobClass = jobClass;
            this.crontab = crontab;
        }

    }

    private class ScheduleInLock implements Runnable {
        private ScheduleJob job;
        private Job jobDetail;

        public ScheduleInLock(ScheduleJob job) {
            super();
            this.job = job;
        }

        @Override
        public void run() {
            boolean success = false;
            try {
                jobDetail = (Job) job.jobClass.newInstance();
                if (LOG.isInfoEnabled()) {
                    LOG.info("============[" + job.jobClass.getSimpleName() + "] executing......");
                }
                jobDetail.execute(Scheduler.this);
                success = true;
            } catch (Exception e) {
                LOG.error(null, e);
            } finally {
                try {
                    store.onJobDone(job.id, success);
                } catch (Throwable t) {
                    LOG.error(null, t);
                }
            }
        }

        public void terminate() throws Exception {
            if (jobDetail != null) {
                jobDetail.terminate(Scheduler.this);
            }
        }

    }

    private class ExecuteThread extends Thread {
        private ScheduleJob job;
        private ScheduleInLock lockJob;

        public ExecuteThread(ScheduleJob job) {
            super();
            this.job = job;
        }

        private void log(String msg) {
            LOG.info("============[" + job.jobClass.getSimpleName() + "] " + msg);
        }

        private void waitUntilScheduled(Date afterTime) {
            Date scheduleTime = job.crontab.getNextValidTimeAfter(afterTime);
            long delay = scheduleTime.getTime() - System.currentTimeMillis();
            if (LOG.isInfoEnabled()) {
                log("schedule at " + scheduleTime + ", delay " + TimeUnit.MILLISECONDS.toSeconds(delay) + "s");
            }
            if (delay <= 0) {
                return;
            }
            synchronized (this) {
                try {
                    wait(delay);
                } catch (InterruptedException e) {
                    LOG.error(null, e);
                }
            }
        }

        @Override
        public void run() {
            while (!stopped) {
                try {
                    Date lastCompletedTime = store.getJobLastCompletedTime(job.id);
                    if (lastCompletedTime == null) {
                        lastCompletedTime = new Date();
                    }
                    waitUntilScheduled(lastCompletedTime);
                    lockJob = new ScheduleInLock(job);
                    boolean lockAcquired = locker.doInLock(LOCK_PREFIX + job.id, lockJob);
                    lockJob = null;
                    if (!lockAcquired) {
                        // 没获取到锁，以当前时间重新计算下次调度时间
                        if (LOG.isInfoEnabled()) {
                            log("could not acquire lock, will schedule later");
                        }
                        waitUntilScheduled(new Date());
                    }
                } catch (Throwable e) {
                    LOG.error(null, e);
                    try {
                        Thread.sleep(1000);
                    } catch (Throwable t) {
                        // ignore
                    }
                }
            }
        }

        public void shutdown() {
            synchronized (this) {
                try {
                    notify();
                } catch (Exception e) {
                    LOG.error(null, e);
                }
            }
            if (lockJob != null) {
                try {
                    lockJob.terminate();
                } catch (Exception e) {
                    LOG.error(null, e);
                }
            }
            try {
                join();
            } catch (Exception e) {
                LOG.error(null, e);
            }
        }

    }

    private final String LOCK_PREFIX = "job.";
    private JobStore store;
    private DistributeLock locker;
    private Map<String, Object> context;
    private volatile boolean stopped;
    private List<ExecuteThread> threads = new LinkedList<>();

    public Scheduler(JobStore store, DistributeLock locker, Map<String, Object> context) {
        super();
        this.store = store;
        this.locker = locker;
        this.context = context;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public void schedule(String key, String jobClassName, String expression) throws Exception {
        synchronized (threads) {
            if (stopped) {
                throw new RuntimeException("Already stopped");
            }
            Class<?> jobClass = Class.forName(jobClassName);
            CronExpression crontab = new CronExpression(expression);
            ScheduleJob job = new ScheduleJob(key, jobClass, crontab);
            store.addJob(key);
            ExecuteThread thread = new ExecuteThread(job);
            threads.add(thread);
            thread.start();
        }
    }

    public void shutdown() {
        synchronized (threads) {
            stopped = true;
            for (ExecuteThread thread : threads) {
                thread.shutdown();
            }
        }
    }
}
