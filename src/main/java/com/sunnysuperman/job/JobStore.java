package com.sunnysuperman.job;

import java.util.Date;

public interface JobStore {

    void addJob(String id) throws Exception;

    Date getJobLastCompletedTime(String id) throws Exception;

    void onJobDone(String id, boolean success) throws Exception;

}
