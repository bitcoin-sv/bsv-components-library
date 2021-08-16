package com.nchain.jcl.tools.thread;

import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An utility Class for MultiThread purposes.
 */
public class ThreadUtils {


    /**
     * ThreadFactory used by the EventBus that control the events in the Streams assigned to the remote Peers.
     * This EventBus will publish the vents related to data being deserialized (incoming) or Serialized (outgoing).
     */
    static class PeerStreamThreadFactory implements ThreadFactory {
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setPriority(Thread.NORM_PRIORITY);
            thread.setDaemon(true);
            thread.setName("JclNetworkStreams");
            return thread;
        }
    }

    /**
     * A built-in Executor for the Streams connected to the Remote Peers. This Stream needs to be Single-thread,
     * otherwise the order of the bytes coming in/out from the Peer cannot be guaranteed
     */
    public static ExecutorService PEER_STREAM_EXECUTOR = Executors.newSingleThreadExecutor(new PeerStreamThreadFactory());

    /** Convenience method to create a ThreadPoolFactory with the name given and other parameters.*/
    public static ThreadFactory getThreadFactory(String name, int priority, boolean daemon) {
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

    /** Returns a new SingleThread executor with MAX Priority */
    public static ExecutorService getSingleThreadExecutorService(String threadName) {
        return Executors.newSingleThreadExecutor(getThreadFactory(threadName, Thread.MAX_PRIORITY, true));
    }
    /** Returns a new scheduled executor with MAX priority */
    public static ScheduledExecutorService getScheduledExecutorService(String threadName) {
        return Executors.newScheduledThreadPool(1, (getThreadFactory(threadName, Thread.MAX_PRIORITY, true)));
    }
    /** Returns a FixedThread executor with MAX Priority */
    public static ExecutorService getFixedThreadExecutorService(String threadName, int maxThreads) {
        return Executors.newFixedThreadPool(maxThreads, getThreadFactory(threadName, Thread.MAX_PRIORITY, true));
    }

    /**
     * Returns a CachedThread executor with HIGH priority. For high performance tasks, a high number of 'maxThreads'
     * can be used.
     *
     * @param threadName    Thread name (for tracking/logging purposes)
     * @param maxThreads    Maximum number of Threads created
     */
    public static ExecutorService getCachedThreadExecutorService(String threadName, int maxThreads) {
        return new ThreadPoolExecutor(maxThreads, maxThreads, 60L, TimeUnit.SECONDS,
                //new SynchronousQueue<Runnable>(), getThreadFactory(threadName, Thread.MAX_PRIORITY, true));
                new LinkedBlockingQueue<>(), getThreadFactory(threadName, Thread.MAX_PRIORITY, true));
    }

    /**
     * Returns a CachedThread executor with HIGH Priority. The max number of Threads created is equals to the number of
     * available processor, so for more high-performance tasks where thousands of tasks per sec might be triggered,
     * you better use the 'getCachedThreadExecutorService(threadName, maxThread) version.
     *
     * @param threadName Thread name (for tracking/logging purposes)
     */
    public static ExecutorService getCachedThreadExecutorService(String threadName) {
        return new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().availableProcessors(), 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(), getThreadFactory(threadName, Thread.MAX_PRIORITY, true));
    }
    /** Returns a SingleThread executor with MAX Priority */
    public static ScheduledExecutorService getSingleThreadScheduledExecutorService(String threadName) {
        return Executors.newSingleThreadScheduledExecutor(getThreadFactory(threadName, Thread.MAX_PRIORITY, true));
    }


    /**
     * Temporary Code to printout info about the Threads triggered by JCL (A Threads is being considered part of
     * JCL if tis name starts with "Jcl".
     */
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

        // Now we create the result, choosing only the ones belonging to JCL:
        String result = numThreadsByPreffixList.stream()
                .filter(e -> e.getKey().startsWith("Jcl"))
                .map(e -> e.getKey() + ":" + e.getValue() + ", ")
                .limit(30)
                .collect(Collectors.joining());
        return Thread.activeCount() + " Threads. JCL distribution: " + result;
    }
}
