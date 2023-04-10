package io.bitcoinsv.bsvcl.tools.thread;

import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A utility Class for MultiThread purposes.
 */
public class ThreadUtils {


    /**
     * ThreadFactory used by the EventBus that control the events in the Streams assigned to the remote Peers.
     * This EventBus will publish the vents related to data being deserialized (incoming) or Serialized (outgoing).
     */
    static class PeerStreamThreadFactory implements ThreadFactory {
        private final String name;

        public PeerStreamThreadFactory() {
            this(null);
        }

        public PeerStreamThreadFactory(String name) {
            this.name = name;
        }

        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setPriority(Thread.NORM_PRIORITY);
            thread.setDaemon(true);
            thread.setName("JclNetworkStreams");

            if (name != null) {
                thread.setName(thread.getName() + " - " + name);
            }

            return thread;
        }
    }

    /** Convenience method to create a ThreadPoolFactory with the name given and other parameters.*/
    public static ThreadFactory getThreadFactory(String name, int priority, boolean daemon) {
        return r -> {
            Thread thread = new Thread(r);
            thread.setPriority(priority);
            thread.setDaemon(daemon);
            thread.setName(name + ":" + thread.getId());
            return thread;
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

    public static ThreadPoolExecutor getBlockingSingleThreadExecutorService(String threadName, int capacity) {
        return getBlockingSingleThreadExecutorService(threadName, capacity, Thread.NORM_PRIORITY);
    }

    public static ThreadPoolExecutor getBlockingSingleThreadExecutorService(String threadName, int priority, ArrayBlockingQueue<Runnable> queue) {
        var executor = new ThreadPoolExecutor(
            0,
            1,
            0L,
            TimeUnit.MILLISECONDS,
            queue,
            getThreadFactory(threadName, priority, true)
        );

        registerRejectionExecutionHandle(executor);

        return executor;
    }

    public static ThreadPoolExecutor getBlockingSingleThreadExecutorService(String threadName, int capacity, int priority) {
        var executor = new ThreadPoolExecutor(
                0,
                1,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(capacity),
                getThreadFactory(threadName, priority, true)
        );

        registerRejectionExecutionHandle(executor);

        return executor;
    }

    public static ThreadPoolExecutor getBlockingThreadExecutorService(String threadName, int maxThreads, int capacity) {
        return getBlockingThreadExecutorService(threadName, maxThreads, capacity, Thread.NORM_PRIORITY);
    }

    public static ThreadPoolExecutor getBlockingThreadExecutorService(String threadName, int maxThreads, int capacity, int priority) {
        var executor = new ThreadPoolExecutor(
                0,
                maxThreads,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(capacity),
                getThreadFactory(threadName, priority, true)
        );

        registerRejectionExecutionHandle(executor);

        return executor;
    }

    public static ThreadPoolExecutor getCachedBlockingThreadExecutorService(String threadName, int maxThreads, int capacity) {
        return getCachedBlockingThreadExecutorService(threadName, maxThreads, capacity, Thread.NORM_PRIORITY);
    }

    public static ThreadPoolExecutor getCachedBlockingThreadExecutorService(String threadName, int maxThreads, int capacity, int priority) {
        var executor = new ThreadPoolExecutor(
                0,
                maxThreads,
                60L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(capacity),
                getThreadFactory(threadName, priority, true)
        );

        registerRejectionExecutionHandle(executor);

        return executor;
    }

    private static void registerRejectionExecutionHandle(ThreadPoolExecutor executor) {
        executor.setRejectedExecutionHandler((runnable, threadPoolExecutor) -> {
            try {
                // block until there's room
                executor.getQueue().put(runnable);
                if (executor.isShutdown()) {
                    throw new RejectedExecutionException("Executor has shut down, task was rejected");
                }
            } catch (InterruptedException e) {
                throw new RejectedExecutionException("Task scheduling interrupted: ", e);
            }
        });
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
     * Returns a CachedThread executor with HIGH Priority.
     *
     * @param threadName Thread name (for tracking/logging purposes)
     */
    public static ExecutorService getCachedThreadExecutorService(String threadName) {
//        return new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().availableProcessors(), 60L, TimeUnit.SECONDS,
//                new LinkedBlockingQueue<>(), getThreadFactory(threadName, Thread.MAX_PRIORITY, true));
        return Executors.newCachedThreadPool(ThreadUtils.getThreadFactory(threadName, Thread.MAX_PRIORITY, true));
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