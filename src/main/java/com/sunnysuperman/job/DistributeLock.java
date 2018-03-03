package com.sunnysuperman.job;

public interface DistributeLock {

    boolean doInLock(String name, Runnable runnable) throws Exception;

}
