package com.nchain.jcl.base.tools.thread;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-23 10:07
 *
 * An utility Class for MultiThread purposes.
 */
public class ThreadUtils {


    /**
     * A ThreadFactory that returns Daemon Threads. This is the ThreadFactory used when running the callbacks
     * triggered by the data received/sent by the Peers.
     */
    static class PeerStreamThreadFactory implements ThreadFactory {
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setPriority(Thread.MAX_PRIORITY);
            thread.setDaemon(true);
            thread.setName("Network Streams Thread");
            return thread;
        }
    }

    /**
     * A ThreadFactory that returns Daemon Threads. This is the ThreadFactory used when evetns/Requests are triggered
     * and captured among the different Handlers i the Network and Protocol packages.
     */
    static class EventBusThreadFactory implements ThreadFactory {
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setPriority(Thread.MAX_PRIORITY);
            thread.setDaemon(true);
            thread.setName("EventBus Thread");
            return thread;
        }
    }

    // A built-in Executor used for the EventBus shared by all the Network and Protocol Handlers
    public static ExecutorService EVENT_BUS_EXECUTOR = Executors.newCachedThreadPool(new EventBusThreadFactory());

    // A built-in Executor for the Streams connected to the Remote Peers. This Stream needs to be Single-thread,
    // otherwise the order of the bytes coming in/out from the Peer cannot be guaranteed
    public static ExecutorService PEER_STREAM_EXECUTOR = Executors.newSingleThreadExecutor(new PeerStreamThreadFactory());

    // Convenience method to create a ThreadPoolFactory with the name given and other parameters. Useful for testing
    private static ThreadFactory getThreadFactory(String name, int priority, boolean daemon) {
        return new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setPriority(priority);
                thread.setDaemon(daemon);
                thread.setName(name + thread.getId());
                return thread;
            }
        };
    }

    // Convenience method to create a Thread Pool Executor Service
    public static ExecutorService getThreadPoolExecutorService(String threadName) {
        return Executors.newCachedThreadPool(getThreadFactory(threadName, Thread.MAX_PRIORITY, true));
    }
    // Convenience method to create a Thread Pool Executor Service
    public static ExecutorService getSingleThreadExecutorService(String threadName) {
        return Executors.newCachedThreadPool(getThreadFactory(threadName, Thread.MAX_PRIORITY, true));
    }
    public static ExecutorService getSingleThreadExecutorService(String threadName, int maxThreads) {
        return Executors.newFixedThreadPool(maxThreads, getThreadFactory(threadName, Thread.MAX_PRIORITY, true));
    }

    // Convenience method to create a Thread Pool ScheduledExecutor Service
    public static ScheduledExecutorService getSingleThreadScheduledExecutorService(String threadName) {
        return Executors.newSingleThreadScheduledExecutor(getThreadFactory(threadName, Thread.MAX_PRIORITY, true));
    }

    /**
     * Executor used to run tasks in a "System" thread executor. Intended to be used for different logic than
     * the listeners.
     */
    // TODO: Another Factory needed for this?
    public static ExecutorService SYSTEM_EXECUTOR = Executors.newSingleThreadExecutor();
}
