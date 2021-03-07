package com.nchain.jcl.tools.thread;

/**
 *
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A byteArray for instances of {@link TimeoutTask}
 */
public class TimeoutTaskBuilder {

    private Runnable task;
    private Runnable timeoutTask;
    private int timeoutMillisecs;

    // Constructor
    private TimeoutTaskBuilder() {}

    /** Specifies the main Task to execute */
    public TimeoutTaskBuilder execute(Runnable task) {
        this.task = task;
        return this;
    }

    /** Specified the alternative Task to execute if the threshold timeout is reached */
    public TimeoutTaskBuilder ifTimeoutThenExecute(Runnable timeoutTask) {
        this.timeoutTask = timeoutTask;
        return this;
    }

    /** Specified the threshold timeout for the main task */
    public TimeoutTaskBuilder waitFor(int timeoutMillisecs) {
        this.timeoutMillisecs = timeoutMillisecs;
        return this;
    }

    /** Returns an instance of {@link TimeoutTask} */
    public TimeoutTask build() {
        return new TimeoutTask(task, timeoutTask, timeoutMillisecs);
    }

    /** Creates a new Builder */
    public static TimeoutTaskBuilder newTask() {
        return new TimeoutTaskBuilder();
    }
}
