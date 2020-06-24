package com.nchain.jcl.tools.thread;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * @author j.bloggs@nchain.com
 * Copyright (c) 2009-2010 Satoshi Nakamoto
 * Copyright (c) 2009-2016 The Bitcoin Core developers
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-23 10:07
 */
public class ThreadUtils {


    /**
     * A ThreadFactory that returns Daemon Threads. This is the ThreadFactory used when running the
     * Listeners in the Applications
     */
    static class NetworkStreamThreadFactory implements ThreadFactory {
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setPriority(Thread.MAX_PRIORITY);
            thread.setDaemon(true);
            thread.setName("Network Streams Thread");
            return thread;
        }
    }

    /**
     * A ThreadFactory that returns Daemon Threads. This is the ThreadFactory used when running the
     * Listeners in the Applications
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

    public static ExecutorService PROTOCOL_EVENT_BUS_EXECUTOR = Executors.newSingleThreadExecutor(new EventBusThreadFactory());
    public static ExecutorService NETWORK_STREAM_EXECUTOR = Executors.newSingleThreadExecutor(new NetworkStreamThreadFactory());


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

    public static ExecutorService getThreadPoolExecutorService(String threadName) {
        return Executors.newCachedThreadPool(getThreadFactory(threadName, Thread.MAX_PRIORITY, true));
    }

    public static ExecutorService getSingleThreadExecutorService(String threadName) {
        return Executors.newCachedThreadPool(getThreadFactory(threadName, Thread.MAX_PRIORITY, true));
    }

    /**
     * Executor used to run tasks in a "System" thread executor. Intended to be used for different logic than
     * the listeners.
     */
    // TODO: Another Factory needed for this?
    public static ExecutorService SYSTEM_EXECUTOR = Executors.newSingleThreadExecutor();
}
