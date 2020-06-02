package com.nchain.jcl.tools.concurrent;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 *
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2019-07-10
 *
 * Utility regarding Thread Management
 */
@Slf4j
// TODO: What about Thread Priorities, different depending on the Executor?
public class ThreadUtils {

    /**
     * A ThreadFactory that returns Daemon Threads. This is the ThreadFactory used when running the
     * Listeners in the Applications
     */
    static class DaemonThreadFactory implements ThreadFactory {
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setPriority(Thread.MAX_PRIORITY);
            thread.setDaemon(true);
            return thread;
        }
    }


    /**
     * Returns a Executor for use in the Network Component
     * NOTE: In the Network Component, we can only use a Single Thread, since the listners MUST be called in
     * sequence (otherwise we wuld be propagating Serialization processes that try to serialize bytes in the
     * wrong order)
     * */
    public static ExecutorService newNetListenersExecutor() {
        return Executors.newSingleThreadExecutor(new DaemonThreadFactory());
    }


    /**
     * Returns a Executor for use in the Protocol Components.
     * NOTE: The listeners in the Protocol Component can be executed in multiple Threads, since the order of the
     * incoming messages is not important.
     */
    public static ExecutorService newProtocolListenersExecutor() {
        return Executors.newCachedThreadPool(new DaemonThreadFactory());
    }


    /**
     * Executor used to run tasks in a "System" thread executor. Intended to be used for different logic than
     * the listeners.
     */
    // TODO: Another Factory needed fro this?
    public static ExecutorService SYSTEM_EXECUTOR = Executors.newSingleThreadExecutor();

    /**
     * Executes a task (could be anything, as long as it does NOT have a return value) in a different Thread.
     * This method is useful when we are running code that we don't want to block the rest of the system.
     * The different between blocking or no blocking is in the Executor specified.
     *
     * @param task      Task to run (without returning any value, just void)
     * @param executor  Executor used to create Threads to run this Task.
     */
    public static void executeInThread(Runnable task, Executor executor) {
        try {
            executor.execute(task);
        } catch (Exception e) {
            log.trace("Listener Task rejected (Service shutting down)");
        }

    }

}
