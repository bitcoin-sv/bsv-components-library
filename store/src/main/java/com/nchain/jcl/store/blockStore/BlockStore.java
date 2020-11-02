package com.nchain.jcl.store.blockStore;

import com.nchain.jcl.base.domain.api.base.BlockHeader;
import com.nchain.jcl.base.domain.api.base.Tx;
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper;
import com.nchain.jcl.store.blockStore.events.BlockStoreStreamer;
import com.nchain.jcl.store.common.PaginatedRequest;
import com.nchain.jcl.store.common.PaginatedResult;

import java.util.List;
import java.util.Optional;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A BlockStorage Component provides operations to store, retreive and remove Blocks and Transactions.
 * Different implementations could provide different performances.
 *
 */
public interface BlockStore {

    // Init/post operations:

    /**
     * It performs DB Initialization. This method should be called BEFORE any other method in this API
     */
    void start();

    /**
     * It performs DB cleaning procedures. This method shoul dalways be called when finishing working with the
     * DB, and no further operations are allowed after this. If you need to operate with the DB again, call "start()"
     * again.
     */
    void stop();

    // Block Storage Operations:

    /**
     * Saves the Block Given.
     * If there is already another Block in the DB with the same HASH, it will be replaced with this one.
     *
     * ATENTION:
     * If the Blocks Events are enabled, this method will trigger a BlocksSavedEvent, containing a list with a
     * single element (this block hash). If the Block Events are enabled and a high number of block "save"
     * operations are expected, this method might affect highly the performance. In that case you should use the
     * "saveBlocks()" method instead.
     */
    void saveBlock(BlockHeader blockHeader);

    /**
     * Saves the list of Blocks given.
     * If the Block Events are enabled, this method will trigger a "BlocksSavedEvent" containing a list with the
     * hashes of the Blocks saved.
     */
    void saveBlocks(List<BlockHeader> blockHeaders);

    /**
     * Tells whether the Db contains the Block given
     */
    boolean containsBlock(Sha256Wrapper blockHash);

    /**
     * Retrieves the Block specified by the Hash given
     */
    Optional<BlockHeader> getBlock(Sha256Wrapper blockHash);

    /**
     * Removes the block with the HASH given.
     * It also removes any relation between this block and any TX that could be stored, but it does NOT
     * remove the TX themselves (if you need to also remove them you need to use "removeBlockTxs()" separately.
     *
     * If the Block Events are enabled, this method triggeres a "BlocksRemovedEvent", with a list containing this
     * block hash.
     */
    void removeBlock(Sha256Wrapper blockHash);

    /**
     * Removes the Blocks referenced by the list of Hashes (in HEX format)
     * It also removes any relation ship between this block and any TX that clould be stored, but it does NOT
     * remove the TX themselves (if you need to also remove them you need to use "removeBlockTxs()".
     *
     * If the Block Events are enabled, this methods triggered a "BlocksRemovedEvent" containing the list of
     * these block hashes.
     */
    void removeBlocks(List<Sha256Wrapper> blockHashes);

    /**
     * Returns the total number of Blocks stored in the DB
     */
    long getNumBlocks();

    // Tx Storage Operations:

    /**
     * Saves the TX given
     *
     * ATENTION:
     * If the TX Events are active, this method will trigger a "TxsSavedEvent", containing a list with this
     * Tx Hash. If a high number of TX "save" operations are expected, you should use the "saveTxs()" method instead.
     */
    void saveTx(Tx tx);

    /**
     * Saves the list of Tx given
     * If the TX Events are active, this method will trigger a "TxsSavedEvent", containing a list with these Tx hashes
     */
    void saveTxs(List<Tx> txs);

    /**
     * Tells whether the Db contains the Tx given
     */
    boolean containsTx(Sha256Wrapper txHash);

    /**
     * Retrieves the TX with the HASH given, or null if it's not found
     */
    Optional<Tx> getTx(Sha256Wrapper txHash);

    /**
     * Removes the TX with the HASH (in HEX format) given.
     * If this Tx is linked to a Block, that link is also removed.
     *
     * ATENTION:
     * If TX Events are enabled, this method will trigger an "TxsRemovedEvent" Event, containing a List with this Tx hash.
     * If a high number of "removeTx" operatiosn are expected, this might highly affect the performane, in that case
     * you should use the "removeTxs()" method instead.
     */
    void removeTx(Sha256Wrapper txHash);

    /**
     * Removes the TXs referenced by the List of HASHes given
     * If these Txs are linked to a Block, that link is also removed.
     *
     * If the TX Events are enabled, this methid will trigger a "TxsRemovedEvent" Event, containing a List with these
     * Tx Hashes.
     */
    void removeTxs(List<Sha256Wrapper> txHashes);

    /**
     * Returns the total number of Txs stored in the DB
     */
    long getNumTxs();

    // Block-Tx Link operations:

    /**
     * It links the Tx given to this Block.
     * This method does NOT check whether the Tx is already linked to another Block, so that verification must be
     * performed outside of this method if the DB are to be in a consistent state.
     */
    void linkTxToBlock(Sha256Wrapper txHash, Sha256Wrapper blockHash);

    /**
     * It un-links the Tx from the Block given.
     */
    void unlinkTxFromBlock(Sha256Wrapper txHash, Sha256Wrapper blockHash);

    /**
     * It unlinks the Tx from any Block it might belong to. It doe NOT remove the Tx itself, only the relation with
     * any Block there might be.
     */
    void unlinkTx(Sha256Wrapper txHash);

    /**
     * It unliks the block given from any Tx it migth contain
     * this method does NOT remove the Tx themselves, only the relation with the Block. If you need to remove the
     * Txs belonging to a Block, use the "removeBlockTxs()" method instead.
     */
    void unlinkBlock(Sha256Wrapper blockHash);

    /** Retrieves the Block Hash of the Block the TX given belongs to, if any */
    Optional<Sha256Wrapper> getBlockHashLinkedToTx(Sha256Wrapper txHash);

    /** Returns the Hashes of the TXS belonging the the block with the HASH (in HEX format) given */
    PaginatedResult<Sha256Wrapper> getBlockTxs(Sha256Wrapper blockHash, PaginatedRequest pagReq);

    /**
     * Saves the Transactiosn given and links them to the block
     * Using this method instead of "saveTxs" allows for creating the relationship between a Block and its
     * Transactions, so they can be retrieved alter on by the method "getBlockTxs".
     */
    void saveBlockTxs(Sha256Wrapper blockHash, List<Tx> txs);

    /**
     * Removes all the TXs belonging to the block given. The Tx and their relations with the Block given are
     * removed.
     *
     * If TX events are enabled, this method might potentially trigger an undetermined number of "TxsRemovedEvent".
     * Each "TxsRemovedEvent" event contains a list with the Tx hashes that have been removed. But when using this method,
     * ALL the TXs belonging to a block are removed, and there might be a huge number of Txs, a number so big that
     * it's not "safe" to trigger one single "TxRemovedEvent" event containing a List with ALL of the Tx hashes, since
     * we might run into memory issues. So in this case, if the Number of Txs contained in this block are higher than
     * a threshold specified (10_000 by default), then several "TxRemovedEvent" Events will be triggered, until all
     * the Tx hashes are triggered.
     */
    void removeBlockTxs(Sha256Wrapper blockHash);

    // Events Streaming:

    /**
     * It returns a blockStorageStreamer, which can be used to subscribe to all the different types of Events that
     * can be triggered by the BlockStorage Component
     */
    BlockStoreStreamer EVENTS();

    // ONLY FOR TESTING
    long getNumKeys(String keyPrefix);

}
