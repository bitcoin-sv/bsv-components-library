package com.nchain.jcl.tools.thread;

import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Collectors;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
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
            thread.setName("JclNetworkStreams");
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
            thread.setPriority(Thread.NORM_PRIORITY);
            thread.setDaemon(true);
            thread.setName("JclEventBus");
            return thread;
        }
    }

    static class EventBusThreadFactoryHighPriority implements ThreadFactory {
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setPriority(Thread.MAX_PRIORITY);
            thread.setDaemon(true);
            thread.setName("JclEventBusHighPriority");
            return thread;
        }
    }

    public static ExecutorService EVENT_BUS_EXECUTOR_HIGH_PRIORITY = Executors.newFixedThreadPool(100, new EventBusThreadFactoryHighPriority());

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
                thread.setName(name + ":" + thread.getId());
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
        return Executors.newSingleThreadScheduledExecutor(getThreadFactory(threadName, Thread.MAX_PRIORITY, true));
    }
    // Convenience method to create a Scheduled Executor Service
    public static ScheduledExecutorService getScheduledExecutorService(String threadName) {
        return Executors.newScheduledThreadPool(1, (getThreadFactory(threadName, Thread.MAX_PRIORITY, true)));
    }
    public static ScheduledExecutorService getScheduledExecutorService(String threadName, int maxThreads) {
        return Executors.newScheduledThreadPool(maxThreads, (getThreadFactory(threadName, Thread.MAX_PRIORITY, true)));
    }
    public static ExecutorService getFixedThreadExecutorService(String threadName, int maxThreads) {
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
    public static ExecutorService SYSTEM_EXECUTOR = Executors.newSingleThreadExecutor(getThreadFactory("PeerConnectionExecutor", Thread.MAX_PRIORITY, true));


    public static String getThreadsInfo() {
        // First we get the root threadGroup
        ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
        while (threadGroup.getParent() != null) {
            threadGroup = threadGroup.getParent();
        }
        // Now we loop over all the threads belonging to this group and we group them by Name:
        String[] separators = new String[] {"-", " ", ":"};
        Map<String, Integer> numThreadsByPreffixMap = new HashMap<>();
        Thread[] threads = new Thread[Thread.activeCount()];
        int numThreads = threadGroup.enumerate(threads);
        for (int i = 0; i < numThreads; i++) {
            String threadName = threads[i].getName();
            String threadNamePreffix = threadName;
            for (String separator : separators) {
                if (threadName.indexOf(separator) > 0) {
                    threadNamePreffix = threadName.substring(0, threadName.indexOf(separator));
                    break;
                }
            }
            numThreadsByPreffixMap.merge(threadNamePreffix, 1, (o, n) -> o + 1);
        } // for...

        // Now we order them by number of Threads...using Guava
        Ordering<Map.Entry<String, Integer>> byMapValues = new Ordering<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> left, Map.Entry<String, Integer> right) {
                return left.getValue().compareTo(right.getValue());
            }
        };
        List<Map.Entry<String,Integer>> numThreadsByPreffixList = Lists.newArrayList(numThreadsByPreffixMap.entrySet());
        Collections.sort(numThreadsByPreffixList, byMapValues.reverse());

        String result = numThreadsByPreffixList.stream()
                .filter(e -> e.getKey().startsWith("Jcl"))
                .map(e -> e.getKey() + ":" + e.getValue() + ", ")
                .limit(30)
                .collect(Collectors.joining());
        return Thread.activeCount() + " Threads. JCL distribution: " + result;
    }
}
