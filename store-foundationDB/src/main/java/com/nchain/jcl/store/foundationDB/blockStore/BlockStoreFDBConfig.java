package com.nchain.jcl.store.foundationDB.blockStore;

import com.nchain.jcl.base.tools.config.RuntimeConfig;
import lombok.Builder;
import lombok.Getter;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Configuration class for the FoundationDB Implementation of the BlockStore interface
 */
@Builder(toBuilder = true)
@Getter
public class BlockStoreFDBConfig {

    /**
     * Number of Items to process on each Transaction. An "item" might be a Block, a Tx, etc.
     * FoundationDB has a limitation on the number of Bytes affected within a Transaction and also on the time it takes
     * for each Transaction tom complete, that means that when running operations on a list of items (Saving Blocks or
     * Tx, removing, etc), we need to make sure that the number of items is not too big. So we use these property to
     * break down the list into smaller sublist, and on Tx is created for each sublist, to handle the items in that
     * sublist.
     *
     * This technique only takes into consideration the number of Items, not their size, so its not very efficient. A
     * more accurate implementation will take into consideration the SIZE of each ITem and will break down the list
     * depending on those sizes. That is doable when insert elements (since the items themselves are in the list so we
     * can inspect them and check out their size), but its more problematic when removing. So this technique is a
     * middle-ground solution.
     */
    public static final int TRANSACTION_BATCH_SIZE = 5000;

    /** Runtime Config */
    private final RuntimeConfig config;

    /** FoundationDb cluster file. If not specified, the default location is used */
    private String clusterFile;

    /** Java API Version. This might change if the maven dependency is updated, so be careful */
    @Builder.Default
    private int apiVersion = 510;

    /**
     * The network Id to use as a Base Directoy. We use a String here to keep dependencies simple,
     * but in real scenarios this value will be obtained from a ProtocolConfiguration form the JCL-Net
     * module
     */
    private String networkId;

    /**
     * FoundationDb triggers an Exception if a Transaction takes longer than a threshold to finalize, and this
     * Threshold might be too small for production-like scenarios where we process thousands of Txs. For now, the only
     * workaround we have is to breakdown those Tx into smaller ones.
     */
    @Builder.Default
    private int transactionBatchSize = TRANSACTION_BATCH_SIZE;
}
