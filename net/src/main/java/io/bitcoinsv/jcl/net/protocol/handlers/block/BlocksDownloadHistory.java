package io.bitcoinsv.jcl.net.protocol.handlers.block;

import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.bitcoinjsv.core.Sha256Hash;
import io.bitcoinsv.jcl.tools.util.EventsHistory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

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
public class BlocksDownloadHistory extends EventsHistory<String, String, PeerAddress> {

    private Logger logger = LoggerFactory.getLogger(BlocksDownloadHistory.class);

    // Events to save when Block history is removed:
    private String ITEM_AFTER_AUTOMATIC_REMOVE = "Block History removed automatically after %d seconds";
    private String ITEM_AFTER_ONDEMAND_REMOVE = "Block History removed.";

    /** Constructor */
    public BlocksDownloadHistory() {
        super();
        this.addItemWhenHistoryRemovedAutomatically(() -> ITEM_AFTER_AUTOMATIC_REMOVE);
        this.addItemWhenHistoryRemovedOnDemand(() -> ITEM_AFTER_ONDEMAND_REMOVE);
    }

    /**
     * returns the history of the block given
     */
    public Optional<List<HistoricItem<String, PeerAddress>>> getBlockHistory(String blockHash) {

        return super.getItemHistory(blockHash);
    }

    /**
     * returns the history of the block given
     */
    public Optional<List<HistoricItem<String, PeerAddress>>> getBlockHistory(Sha256Hash blockHash) {
        return getBlockHistory(blockHash.toString());
    }

    /** returns the history of ALL the blocks */
    public Map<String, List<HistoricItem<String, PeerAddress>>> getBlocksHistory() {
        return super.getItemsHistory();
    }

}
