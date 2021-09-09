package com.nchain.jcl.net.protocol.handlers.block;

import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.tools.thread.ThreadUtils;
import io.bitcoinj.core.Sha256Hash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * @date 22/05/2021
 *
 * This class stores the history of the download process. For each Block, it stores a List of HistoryItems,
 * which store a timestamps, the event taking place and the remote peer responsible for that. In some cases
 * where we might want to register an action that is not related to a remote peer, we can just leave that
 * field empty
 */
public class BlocksDownloadHistory {

    private Logger logger = LoggerFactory.getLogger(BlocksDownloadHistory.class);

    /** Represents an Historic Item. A block History is made up of several of this objects, ordered by timestamp */
    public class HistoricItem {
        private Instant timestamp;
        private PeerAddress peerAddress;
        private String event;

        // Constructor
        public HistoricItem(PeerAddress peerAddress, String event) {
            this.timestamp = Instant.now();
            this.peerAddress = peerAddress;
            this.event = event;
        }
        public HistoricItem(String event)   { this(null, event); }
        public Instant getTimestamp()       { return this.timestamp;}
        public PeerAddress getPeerAddress() { return this.peerAddress;}
        public String getEvent()            { return this.event;}

        @Override
        public String toString() {
            String result = timestamp + " :: "
                + ((peerAddress != null) ? "[" + peerAddress + "] " : " ")
                + event;
                return result;
        }
    }

    // Blocks Download History
    private Map<String, List<HistoricItem>> history = new ConcurrentHashMap<>();

    // A Set storing which Blocks are ok to delete after the timeout has expired (this only applies for
    // the automatic deletion by the cron job)
    private Set<String> blocksMarkedForDeletion =  ConcurrentHashMap.newKeySet();

    // Configuration to clean entries in the DB after a timeout is configured:
    private Duration cleaningTimeout;
    private ExecutorService executor;

    // By default, after removing a Block History we loose all info about it. That might make things harder
    // to track when testing, so the properties below can add a remaining Item even after removing...
    private boolean addingItemAfterAutomaticRemoveEnabled;
    private boolean addingItemAfterOnDemandRemoveEnabled;
    private String ITEM_AFTER_AUTOMATIC_REMOVE = "Block History removed automatically after %d seconds";
    private String ITEM_AFTER_ONDEMAND_REMOVE = "Block History removed.";

    /** Constructor */
    public BlocksDownloadHistory() {
        this.executor = ThreadUtils.getSingleThreadExecutorService("jclBlocksDownloadHistory");
    }

    public void setCleaningTimeout(Duration cleaningTimeout)    { this.cleaningTimeout = cleaningTimeout; }
    public void enableAddingItemAfterAutomaticRemove()          { this.addingItemAfterAutomaticRemoveEnabled = true;}
    public void enableAddingItemAfterOnDemandRemoveEnabled()    { this.addingItemAfterOnDemandRemoveEnabled = true;}

    /** It registers a item/s in a Block history */
    public synchronized void register(String blockHashHex, PeerAddress peerAddress, String ...historyItems) {
        List<HistoricItem> items = history.containsKey(blockHashHex) ? history.get(blockHashHex) : new ArrayList<>();
            for (String item : historyItems) {
                items.add(new HistoricItem(peerAddress, item));
            }
            history.put(blockHashHex, items);
    }
    /** It registers a item/s in a Block history */
    public synchronized  void register(String blockHash, String ...historyItems) {
        register(blockHash, null, historyItems);
    }

    /**
     * Removes the whole history of a block
     */
    public synchronized void remove(String blockHash) {
        history.remove(blockHash);
        if (addingItemAfterOnDemandRemoveEnabled) {
            register(blockHash, (PeerAddress) null, ITEM_AFTER_ONDEMAND_REMOVE);
        }
    }

    /**
     * Removes the whole history of a block
     */
    private synchronized void clean(String blockHash) {
        history.remove(blockHash);
        if (addingItemAfterAutomaticRemoveEnabled) {
            register(blockHash, (PeerAddress) null, String.format(ITEM_AFTER_AUTOMATIC_REMOVE, this.cleaningTimeout.toSeconds()));
        }
    }

    /**
     * Marks a Block for deletion. When the timeout for this Block history expired, it will be removed
     */
    public synchronized void markForDeletion(String blockHash) { blocksMarkedForDeletion.add(blockHash);}

    /**
     * returns the history of the block given
     */
    public Optional<List<HistoricItem>> getBlockHistory(Sha256Hash blockHash) {
        return history.containsKey(blockHash.toString())? Optional.of(history.get(blockHash)) : Optional.empty();
    }

    /**
     * returns the history of the block given
     */
    public Optional<List<HistoricItem>> getBlockHistory(String blockHash) {
        return history.containsKey(blockHash)? Optional.of(history.get(blockHash)) : Optional.empty();
    }

    /** returns the history of ALL the blocks */
    public Map<String, List<HistoricItem>> getBlocksHistory() {
        return history;
    }

    /**
     * Returns the time passed since there's been activity for this block
     */
    public Duration getTimeSinceLastActivity(String blockHash) {
        Duration result = history.containsKey(blockHash)
                ? Duration.between(history.get(blockHash).get(history.get(blockHash).size() - 1).timestamp, Instant.now())
                : Duration.ZERO;
        return result;
    }

    /**
     * Returns the timestamp of the last activity recorded for this block
     */
    public Optional<Instant> getLastActivity(String blockHash) {
        List<HistoricItem> blockHistory = history.get(blockHash);
        Optional<Instant> result = (blockHistory != null)
                ? Optional.of(blockHistory.get(blockHistory.size() - 1).getTimestamp())
                : Optional.empty();
        return result;
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
                synchronized (this.getClass()) {
                    List<String> hashesToClean = history.entrySet().stream()
                            .map(e -> e.getKey())
                            .filter(hash ->
                                    (getLastActivity(hash).isPresent()
                                        && Duration.between(getLastActivity(hash).get(), Instant.now()).compareTo(cleaningTimeout) > 0)
                                        || (blocksMarkedForDeletion.contains(hash)))
                            .collect(Collectors.toList());
                    // We remove its history and also form the markForDeletion Map:
                    hashesToClean.forEach(this::clean);
                    hashesToClean.forEach(hash -> blocksMarkedForDeletion.remove(hash));
                    logger.trace(hashesToClean.size() + " Blocks history removed");
                }
                Thread.sleep(10_000);
            }
        } catch (Exception e) {
            // Probably its just the system shutting down...
        }
    }

}
