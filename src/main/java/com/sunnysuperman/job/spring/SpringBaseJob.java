package com.sunnysuperman.job.spring;

import org.springframework.context.ApplicationContext;

import com.sunnysuperman.job.Job;
import com.sunnysuperman.job.Scheduler;

public abstract class SpringBaseJob implements Job {
    private String applicationContextKey;

    public SpringBaseJob(String applicationContextKey) {
        super();
        this.applicationContextKey = applicationContextKey;
    }

    @Override
    public final void execute(Scheduler scheduler) throws Exception {
        ApplicationContextSetter.set(this, (ApplicationContext) scheduler.getContext().get(applicationContextKey));
        doExecute(scheduler);
    }

    protected abstract void doExecute(Scheduler scheduler) throws Exception;

}
