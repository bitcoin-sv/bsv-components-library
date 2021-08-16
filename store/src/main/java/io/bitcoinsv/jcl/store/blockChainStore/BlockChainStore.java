/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.store.blockChainStore;


import io.bitcoinsv.jcl.store.blockChainStore.events.BlockChainStoreStreamer;
import io.bitcoinsv.jcl.store.blockStore.BlockStore;
import io.bitcoinj.bitcoin.api.extended.ChainInfo;
import io.bitcoinj.core.Sha256Hash;

import java.util.List;
import java.util.Optional;


/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A BlockChainStorage Component provides operations to retrieve informatin about the "Chains" of Blocks that might
 * be stored. It makes possible to traverse back and forth through a Chain, detect forks, prune a Node, get a list
 * of all the chains currently stored, etc.
 *
 */
public interface BlockChainStore extends BlockStore {

    // Chain Methods:

    /**
     * Retrieves the Block specified by the Height given. In a regular scenario this will be a list of just on element
     * (or Zero). but in case of a fork we might have more than one Block at the same height, but in different "branches"
     */
    List<ChainInfo> getBlock(int height);

    /**
     * Returns the Hash of the Previous Block in the Chain
     */
    Optional<Sha256Hash> getPrevBlock(Sha256Hash blockHash);

    /**
     * Returns a List containing the list of Blocks builts on top of the Block given. Possible results:
     * - An empty List: No blocks are stored building on top of this block.
     * - 1-item List: The regular case
     * - n-items list: A fork scenario.
     */
    List<Sha256Hash> getNextBlocks(Sha256Hash blockHash);

    /**
     * Returns all the Orphan blocks in the DB
     */
    Iterable<Sha256Hash> getOrphanBlocks();

    /**
     * Returns the relative info about the Chain that the Block given is connected to. If the Block is not stored in
     * the DB or it' stored but not connected to any Chain (because there might be a GAP between the Genesis clock and
     * this block), then it will return an empty Optional.
     */
    Optional<ChainInfo> getBlockChainInfo(Sha256Hash blockHash);

    /**
     * Returns the list of Tips of all the chains stored in the DB. In a "normal" scenario there will be only one Tip,
     * but there night also be a fork, in which case we can potentially have more than one Chain (and more than one
     * tip).
     */
    List<Sha256Hash> getTipsChains();

    /**
     * Returns the List of the Tips of the Chains the block given belongs to
     */
    List<Sha256Hash> getTipsChains(Sha256Hash blockHash);

    /**
     * Returns the FIRST Block in the same Path as the block given.
     * If the block has been saved before a Fork, then this method will return the GENESIS Block.
     * If the block has been saved after a Fork, then this method will return the FIRST Block saved after that Fork
     * If the block given is not connected, it will return an Empty Optional
     */
    Optional<ChainInfo> getFirstBlockInHistory(Sha256Hash blockHash);

    /**
     * Removes the info about the tips of the Chain. Once this information is removed, it won't be possible to get the
     * longest chain. This operation is usually temporal: every time a new block is stored, the chain is revised, and
     * the Tups of the Chain are updated accordingly
     */
    void removeTipsChains();

    /**
     * Return the ChainInfo of the Longest (highest) Chain at the moment the method is called.
     * IMPORTANT_ In case there is a FORK, and there are more than one Chain with the SAME height, this method will
     * return one of them.
     */
    Optional<ChainInfo> getLongestChain();

    /**
     * Prunes the chain identified by the Hash of its TIP.
     * All the blocks starting from the TIP and going backward in time until the moment the Fork was created will be
     * removed.
     *
     * NOTE: If we try to call this method using the TIP of the MAIN Chain when there are NO forks, the actual result
     * is the removal of all the blocks in the DB
     *
     * @param tipChainHash  Tip of the Chain that we want to rune
     * @param removeTxs     if TRUE, the TXs belonging to each Block will also be removed
     */
    void prune(Sha256Hash tipChainHash, boolean removeTxs);


    /**
     * Returns the current State of the Info stored
     */
    BlockChainStoreState getState();

    // Events Streaming:

    /**
     * It returns a blockChainStorageStreamer, which can be used to subscribe to all the different types of Events that
     * can be triggered by the BlockChainStorage Component
     */
    @Override
    BlockChainStoreStreamer EVENTS();
}
