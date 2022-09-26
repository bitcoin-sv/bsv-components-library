package io.bitcoinsv.jcl.tools.util;

import static com.google.common.base.Preconditions.*;
import com.google.common.collect.ImmutableMap;
import io.bitcoinsv.jcl.tools.thread.ThreadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * @date 22/05/2021
 *
 * A generic class to store a series of events for an Item, which can be ordered chronologically. A "source" field
 * can also identify the origin of those Events.
 * It can be used for example to store the history of the blocks downloaded, using a PeerAddress as a "source", a
 * Hash as the ItemId and a String as a description of the Event. Bu other uses are also possible.
 *
 * @param <I> Item data type    This must be able to be used as a PK for the Item. If it's a Block, use the Hash
 * @param <E> Event data Type   Data type of the Event stored (A string, a custom class, etc)
 * @param <S> Source data Type  Data type tat specifies the Source of the Event
 */
public class EventsHistory<I, E,S>{

    private Logger logger = LoggerFactory.getLogger(EventsHistory.class);

    /** Represents an Historic Item. A History is made up of several of this objects, ordered by timestamp */
    public class HistoricItem<E,S> {
        private Instant timestamp;
        private E event;
        private S source;

        // Constructor
        public HistoricItem(E event, S source) {
            this.timestamp = Instant.now();
            this.event = event;
            this.source = source;
        }
        public HistoricItem(E event)   { this(event, null); }
        public Instant getTimestamp()  { return this.timestamp;}
        public S getSource()           { return this.source;}
        public E getEvent()            { return this.event;}

        @Override
        public String toString() {
            String result = timestamp + " :: "
                + ((source != null) ? "[" + source + "] " : "")
                + event;
                return result;
        }
    }

    // Items History
    private Map<I, List<HistoricItem<E,S>>> history = new ConcurrentHashMap<>();

    // A Set storing which Items are ok to delete after the timeout has expired (this only applies for
    // the automatic deletion by the cron job)
    private Set<I> itemsMarkedForDeletion =  ConcurrentHashMap.newKeySet();

    // Lock kor Multi-Thread sake:
    private ReadWriteLock lock = new ReentrantReadWriteLock();

    // Configuration to clean entries in the DB after a timeout is configured:
    private Duration cleaningTimeout;
    private ExecutorService executor;

    // By default, after removing a Item History we loose all info about it. That might make things harder
    // to track when testing, so the properties below can add a remaining Item even after removing. If any of
    // these properties is TRUE, one "special" entry will remain in the Item history. In that case, the client
    // of this class should inject a function that will return that "last" item using the
    // "addThisWhenHistoryRemoved()" method:

    private boolean addingItemAfterAutomaticRemoveEnabled;
    private boolean addingItemAfterOnDemandRemoveEnabled;

    // It will add an Event when the history is removed automatically:
    private Supplier<E> eventSupplierAfterAutomaticRemove;

    // It will add an Event when the history is removed on demand:
    private Supplier<E> eventSupplierAfterOnDemandRemove;

    private String ITEM_AFTER_AUTOMATIC_REMOVE  = "History removed automatically after %d seconds";
    private String ITEM_AFTER_ONDEMAND_REMOVE   = "History removed.";

    /** Constructor */
    public EventsHistory() {
        this.executor = ThreadUtils.getSingleThreadExecutorService("jclEventsHistory");
    }

    public void setCleaningTimeout(Duration cleaningTimeout) {
        this.cleaningTimeout = cleaningTimeout;
    }
    public void addItemWhenHistoryRemovedAutomatically(Supplier<E> itemSuplier) {
        this.eventSupplierAfterAutomaticRemove = itemSuplier;
    }
    public void addItemWhenHistoryRemovedOnDemand(Supplier<E> itemSuplier) {
        this.eventSupplierAfterOnDemandRemove = itemSuplier;
    }
    public void enableAddingItemAfterAutomaticRemove() {
        checkState(eventSupplierAfterAutomaticRemove != null,
                "you need to use 'addItemWhenHistoryRemovedAutomatically()' to inject a Supplier");
        this.addingItemAfterAutomaticRemoveEnabled = true;
    }
    public void enableAddingItemAfterOnDemandRemoveEnabled() {
        checkState(eventSupplierAfterAutomaticRemove != null,
                "you need to use 'addItemWhenHistoryRemovedOnDemand()' to inject a Supplier");
        this.addingItemAfterOnDemandRemoveEnabled = true;
    }

    /** It registers a item/s in an Item history */
    public void register(I itemId, S source, E ...historyEvents) {
        try {
            lock.writeLock().lock();
            List<HistoricItem<E,S>> items = history.containsKey(itemId) ? history.get(itemId) : new ArrayList<>();
            for (E event : historyEvents) {
                items.add(new HistoricItem(event, source));
            }
            history.put(itemId, items);
        } finally {
            lock.writeLock().unlock();
        }
    }
    /** It registers a item/s in an Item history */
    public void register(I itemId, E ...historyEvents) {
        register(itemId, null, historyEvents);
    }

    /** Removes the whole history of an Item */
    public synchronized void remove(I itemId) {
        try {
            lock.writeLock().lock();
            history.remove(itemId);
            if (addingItemAfterOnDemandRemoveEnabled) {
                register(itemId, (S) null, this.eventSupplierAfterOnDemandRemove.get());
            }
        } finally {
            lock.writeLock().unlock();
        }

    }

    /** Removes the whole history of an Item */
    private void clean(I ItemId) {
        try {
            lock.writeLock().lock();
            history.remove(ItemId);
            if (addingItemAfterAutomaticRemoveEnabled) {
                register(ItemId, (S) null, this.eventSupplierAfterAutomaticRemove.get());
            }
        } finally {
            lock.writeLock().unlock();
        }

    }

    /**
     * Marks an Item for deletion. When the timeout for this Item expired, it will be removed
     */
    public void markForDeletion(I itemId) {
        try {
            lock.writeLock().lock();
            itemsMarkedForDeletion.add(itemId);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * returns the history of the item given
     */
    public Optional<List<HistoricItem<E,S>>> getItemHistory(I ItemId) {
        try {
            lock.readLock().lock();
            return history.containsKey(ItemId)? Optional.of(history.get(ItemId)) : Optional.empty();
        } finally {
            lock.readLock().unlock();
        }
    }


    /** returns the history of ALL the Items */
    public Map<I, List<HistoricItem<E,S>>> getItemsHistory() {
        try {
            lock.readLock().lock();
            return ImmutableMap.copyOf(history);
        } finally {
            lock.readLock().unlock();
        }

    }

    /**
     * Returns the time passed since there's been activity for this Item
     */
    public Duration getTimeSinceLastActivity(I itemId) {
        try {
            lock.readLock().lock();
            Duration result = history.containsKey(itemId)
                    ? Duration.between(history.get(itemId).get(history.get(itemId).size() - 1).timestamp, Instant.now())
                    : Duration.ZERO;
            return result;
        } finally {
            lock.readLock().unlock();
        }

    }

    /**
     * Returns the timestamp of the last activity recorded for this item
     */
    public Optional<Instant> getLastActivity(I itemId) {
        try {
            lock.readLock().lock();
            List<HistoricItem<E,S>> itemHistory = history.get(itemId);
            Optional<Instant> result = (itemHistory != null)
                    ? Optional.of(itemHistory.get(itemHistory.size() - 1).getTimestamp())
                    : Optional.empty();
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void start() {
        this.executor.submit(this::cleanHistoryJob);
    }
    public void stop() {
        this.executor.shutdownNow();
    }

    /**
     * Runs periodically and removes those entries that have expired and are marked for deletion
     */
    public void cleanHistoryJob() {
        try {
            while (true) {
                try {
                    lock.writeLock().lock();
                    List<I> itemsToClean = history.entrySet().stream()
                            .map(e -> e.getKey())
                            .filter(itemId -> (getLastActivity(itemId).isPresent()
                                    && Duration.between(getLastActivity(itemId).get(), Instant.now()).compareTo(cleaningTimeout) > 0)
                                    && (itemsMarkedForDeletion.contains(itemId))
                            )
                            .collect(Collectors.toList());
                    // We remove its history and also form the markForDeletion Map:
                    itemsToClean.forEach(this::clean);
                    itemsToClean.forEach(hash -> itemsMarkedForDeletion.remove(hash));
                    logger.trace(itemsToClean.size() + " Items history removed");
                } finally {
                    lock.writeLock().unlock();
                }
                // Delay between cleans...
                Thread.sleep(10_000);
            }
        } catch (Exception e) {
            // Probably its just the system shutting down...
        }
    }


    /** Returns a Builder s we can specify Event and source separately */
    public ItemHistoryEntryBuilder item(I itemId) {
        return new ItemHistoryEntryBuilder(itemId);
    }

    /**
     * A builder that can also be used to register an event. Thi sis useful when the data types <E> and <S> are the
     * same, in that case the regular "register" methods will be ambiguous, so this builder will come in handy.
     */
    public class ItemHistoryEntryBuilder {
        I itemId;
        E event;
        S source;

        public ItemHistoryEntryBuilder(I itemId) {
            this.itemId = itemId;
        }

        public ItemHistoryEntryBuilder event(E event) {
            this.event = event;
            return this;
        }

        public ItemHistoryEntryBuilder from(S source) {
            this.source = source;
            return this;
        }

        public void register() {
            try {
                lock.writeLock().lock();
                List<HistoricItem<E,S>> items = history.containsKey(itemId) ? history.get(itemId) : new ArrayList<>();
                items.add(new HistoricItem(event, source));
                history.put(itemId, items);
            } finally {
                lock.writeLock().unlock();
            }
        }

    }

}
