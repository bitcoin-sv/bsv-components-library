package com.nchain.jcl.base.tools.thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2019-07-10 11:37
 *
 * An utility class that can run a Task that might potentially take a long time to finish, but interrupt it
 * if it takes longer than a threshold specified.
 */

public class TimeoutTask {

    private Logger logger = LoggerFactory.getLogger(TimeoutTask.class);

    // Timeout Threshold
    int timeoutMillisecs;
    // Task to run (happy path)
    Runnable task;
    // Task to run if the timeout is reached (sad path)
    Runnable timeoutTask;

    // Constructor
    protected TimeoutTask(Runnable task, Runnable timeoutTask, int timeoutMillisecs) {
        this.task = task;
        this.timeoutTask = timeoutTask;
        this.timeoutMillisecs = timeoutMillisecs;
    }

    /**
     * Executes the task. If the execution takes loner than the threshold specified, then it gets
     * interrupted and the alternate "timeoutTask" is executed instead. In case no timeoutTask is
     * specified, then we just finish.
     */
    public void execute() {
        try {
            // We reuse an EXECUTOR already defined, so we do NOT shut it down afterwards, so it's still
            // available for other tasks in the future:
            ExecutorService service = ThreadUtils.SYSTEM_EXECUTOR;
            Future<?> taskResult = service.submit(task);
            taskResult.get(timeoutMillisecs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException te) {
            // Timeout triggered. So we run the timeoutTask, if any...
            if (timeoutTask != null) {
                try {
                    timeoutTask.run();
                } catch (Exception e) {
                    logger.error("Error executing the timeout fallback", e);
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error executing a TimeoutMonitorTask", e);
        }
    }
}
