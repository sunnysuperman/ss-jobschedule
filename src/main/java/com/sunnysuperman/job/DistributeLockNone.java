package com.sunnysuperman.job;

public class DistributeLockNone implements DistributeLock {
    private static final DistributeLockNone INSTANCE = new DistributeLockNone();

    @Override
    public boolean doInLock(String name, Runnable runnable) throws Exception {
        runnable.run();
        return true;
    }

    public static DistributeLockNone getInstance() {
        return INSTANCE;
    }

}
