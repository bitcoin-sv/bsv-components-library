package com.nchain.jcl.tools.concurrent;

import java.util.concurrent.ExecutorService;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2019-09-10 14:16
 *
 * A Base class that implements utilities for executing the code inside Listeners. Since all the
 * asynchronous behaviour of the Project depends on the execution of multiple Listeners to
 * propagate the information through the multiple layers, managing the execution of these listeners
 * is very important.
 *
 * The Listenres can be executed in blocking or No-blocking Mode. In Blocking Mode, they are executed
 * in the same Thread. In no-blocking Mode they are executed in a separate Thread.
 *
 */
public class ListenerExecutor {

    // The Executor Service that wil be used to run the listeners in No-blockingMode
    private ExecutorService executor;

    // Indicates if the listeners are running in Blocking Mode. In blocking Mode, the listeners are running in
    // the same Thread as the code that has created them, so they might be ablle to block the system.
    private boolean blockingMode = false;

    /** Constructor */
    public ListenerExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    // setter
    public void setBlockingMode()   { this.blockingMode = true; }

    /** Executes a task without throwing any exception in case something's wrong */
    protected void wrap(Runnable task) {
        try {
            task.run();} catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Executes the task, in a different Thread or not depending on the BlockingMode */
    protected void runTask(Runnable task) {
        if (blockingMode) task.run();
        else ThreadUtils.executeInThread(task, executor);
    }

    protected void runBlockingTask(Runnable task) {
        task.run();
    }


}
