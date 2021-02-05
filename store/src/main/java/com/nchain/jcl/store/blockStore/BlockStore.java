package com.nchain.jcl.store.blockStore;


import com.nchain.jcl.store.blockStore.events.BlockStoreStreamer;
import io.bitcoinj.bitcoin.api.base.HeaderReadOnly;
import io.bitcoinj.bitcoin.api.base.Tx;
import io.bitcoinj.core.Sha256Hash;

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
    void saveBlock(HeaderReadOnly blockHeader);

    /**
     * Saves the list of Blocks given.
     * If the Block Events are enabled, this method will trigger a "BlocksSavedEvent" containing a list with the
     * hashes of the Blocks saved.
     */
    void saveBlocks(List<HeaderReadOnly> blockHeaders);

    /**
     * Tells whether the Db contains the Block given
     */
    boolean containsBlock(Sha256Hash blockHash);

    /**
     * Retrieves the Block specified by the Hash given
     */
    Optional<HeaderReadOnly> getBlock(Sha256Hash blockHash);

    /**
     * Removes the block with the HASH given.
     * It also removes any relation between this block and any TX that could be stored, but it does NOT
     * remove the TX themselves (if you need to also remove them you need to use "removeBlockTxs()" separately.
     *
     * If the Block Events are enabled, this method triggeres a "BlocksRemovedEvent", with a list containing this
     * block hash.
     */
    void removeBlock(Sha256Hash blockHash);

    /**
     * Removes the Blocks referenced by the list of Hashes (in HEX format)
     * It also removes any relation ship between this block and any TX that clould be stored, but it does NOT
     * remove the TX themselves (if you need to also remove them you need to use "removeBlockTxs()".
     *
     * If the Block Events are enabled, this methods triggered a "BlocksRemovedEvent" containing the list of
     * these block hashes.
     */
    void removeBlocks(List<Sha256Hash> blockHashes);

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
    boolean containsTx(Sha256Hash txHash);

    /**
     * Retrieves the TX with the HASH given, or null if it's not found
     */
    Optional<Tx> getTx(Sha256Hash txHash);

    /**
     * Removes the TX with the HASH (in HEX format) given.
     * If this Tx is linked to a Block, that link is also removed.
     *
     * ATENTION:
     * If TX Events are enabled, this method will trigger an "TxsRemovedEvent" Event, containing a List with this Tx hash.
     * If a high number of "removeTx" operatiosn are expected, this might highly affect the performane, in that case
     * you should use the "removeTxs()" method instead.
     */
    void removeTx(Sha256Hash txHash);

    /**
     * Removes the TXs referenced by the List of HASHes given
     * If these Txs are linked to a Block, that link is also removed.
     *
     * If the TX Events are enabled, this methid will trigger a "TxsRemovedEvent" Event, containing a List with these
     * Tx Hashes.
     */
    void removeTxs(List<Sha256Hash> txHashes);


    /**
     * Returns the list of Txs that the Tx given (parameter) depends on, because it's using some of their outputs
     * as inputs.
     */
    List<Sha256Hash> getPreviousTxs(Sha256Hash txHash);

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
    void linkTxToBlock(Sha256Hash txHash, Sha256Hash blockHash);

    /**
     * It links the Txs given to this Block.
     * This method does NOT check whether the Txs are already linked to another Block, so that verification must be
     * performed outside of this method if the DB are to be in a consistent state.
     */
    void linkTxsToBlock(List<Sha256Hash> txsHashes, Sha256Hash blockHashes);

    /**
     * It un-links the Tx from the Block given.
     */
    void unlinkTxFromBlock(Sha256Hash txHash, Sha256Hash blockHash);

    /**
     * It un-links the Txs from the Block given.
     */
    void unlinkTxsFromBlock(List<Sha256Hash> txsHashes, Sha256Hash blockHashes);

    /**
     * It unlinks the Tx from any Block it might belong to. It doe NOT remove the Tx itself, only the relation with
     * any Block there might be.
     */
    void unlinkTx(Sha256Hash txHash);

    /**
     * It unliks the block given from any Tx it migth contain
     * this method does NOT remove the Tx themselves, only the relation with the Block. If you need to remove the
     * Txs belonging to a Block, use the "removeBlockTxs()" method instead.
     */
    void unlinkBlock(Sha256Hash blockHash);

    /**
     * Indicates if the Tx given belongs to the Block given.
     */
    boolean isTxLinkToblock(Sha256Hash txHash, Sha256Hash blockHash);

    /**
     * Retrieves the Block Hash of the Block the TX given belongs to. The result is usually a List that is either:
     * - An empty List:
     *   the Transaction doesn't belong to any Block yet, maybe because it's just been saved and not link to any
     *   Block yet, or because its a Transaction received over the Network and its not been included in a mined Block
     * - A List with one Block Hash: The usual scenario: The TX belongs to a Block.
     * - A List with more than 1 Block Hashes: This is a Fork scenario: The TX has been contained in more than 1 Block,
     *   so this Tx belongs to 2 different Chains, until the fork is resolved and one of the Chains is pruned.
     */
    List<Sha256Hash> getBlockHashLinkedToTx(Sha256Hash txHash);


    /**  Returns an Iterable with the Tx Hashes belonging to the block given */
    Iterable<Sha256Hash> getBlockTxs(Sha256Hash blockHash);

    /** Returns the number of TXs belonging to this block */
    long getBlockNumTxs(Sha256Hash blockHash);

    /**
     * Saves the Transactiosn given and links them to the block
     * Using this method instead of "saveTxs" allows for creating the relationship between a Block and its
     * Transactions, so they can be retrieved alter on by the method "getBlockTxs".
     */
    void saveBlockTxs(Sha256Hash blockHash, List<Tx> txs);

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
    void removeBlockTxs(Sha256Hash blockHash);

    /**
     * It compares the content of both Blocks, and return the result highlighting what Tx they both have in common, the
     * ones missing in one of them and the other, etc.
     * If any of the Blocks does not exists, it returns an Empty optional.
     */
    Optional<BlocksCompareResult> compareBlocks(Sha256Hash blockHashA, Sha256Hash blockHashB);

    // Events Streaming:

    /**
     * It returns a blockStorageStreamer, which can be used to subscribe to all the different types of Events that
     * can be triggered by the BlockStorage Component
     */
    BlockStoreStreamer EVENTS();

    /**
     * Removes the Whole DB
     */
    void clear();

    // ONLY FOR TESTING
    /** Returns the number of Keys starting with the preffix given */
    long getNumKeys(String keyPrefix);

    /** Prints out the Content of the DB */
    void printKeys();

}
