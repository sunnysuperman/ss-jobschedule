package com.sunnysuperman.job;

public interface Job {

    void execute(Scheduler scheduler) throws Exception;

    void terminate(Scheduler scheduler) throws Exception;

}
