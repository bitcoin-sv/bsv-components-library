package com.nchain.jcl.net.protocol.handlers.block;

import com.nchain.jcl.net.network.PeerAddress;
import io.bitcoinj.core.Sha256Hash;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    /** Represents an Historic Item. A block History is made up of several of this objects, ordered by timesamp */
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

    /** Constructor */
    public BlocksDownloadHistory() {}

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

    /** Removes the whole history of a block */
    public synchronized void remove(String blockHash) {
        history.remove(blockHash);
    }

    /** returns the history of the block given */
    public List<HistoricItem> getBlockHistory(Sha256Hash blockHash) {
        return history.get(blockHash);
    }

    /** returns the history of ALL the blocks */
    public Map<String, List<HistoricItem>> getBlocksHistory() {
        return history;
    }

    /** Returns the time passed since ther's been activity for this block */
    public Duration getTimeSinceLastActivity(String blockHash) {
        Duration result = history.containsKey(blockHash)
                ? Duration.between(history.get(blockHash).get(history.get(blockHash).size() - 1).timestamp, Instant.now())
                : Duration.ZERO;
        return result;
    }
}
