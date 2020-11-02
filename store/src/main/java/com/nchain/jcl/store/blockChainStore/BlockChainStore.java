package com.nchain.jcl.store.blockChainStore;

import com.nchain.jcl.base.domain.api.extended.ChainInfo;
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper;
import com.nchain.jcl.store.blockChainStore.events.BlockChainStoreStreamer;
import com.nchain.jcl.store.blockStore.BlockStore;

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
     * Returns the Hash of the Previous Block in the Chain
     */
    Optional<Sha256Wrapper> getPrevBlock(Sha256Wrapper blockHash);

    /**
     * Returns a List containing the list of Blocks builts on top of the Block given. Possible results:
     * - An empty List: No blocks are stored building on top of this block.
     * - 1-item List: The regular case
     * - n-items list: A fork scenario.
     */
    List<Sha256Wrapper> getNextBlocks(Sha256Wrapper blockHash);

    /**
     * Returns the relative info about the Chain that the Block given is connected to. If the Block is not stored in
     * the DB or it' stored but not connected to any Chain (because there might be a GAP between the Genesis clock and
     * this block), then it will return an empty Optional.
     */
    Optional<ChainInfo> getBlockChainInfo(Sha256Wrapper blockHash);

    /**
     * Returns the list of Tips of all the chains stored in the DB. In a "normal" scenario there will be only one Tip,
     * but there night also be a fork, in which case we can potentially have more than one Chain (and more than one
     * tip).
     */
    List<Sha256Wrapper> getTipsChains();

    /**
     * Return the ChainInfo of the Longest (highest) Chain at the moment the method is called
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
    void prune(Sha256Wrapper tipChainHash, boolean removeTxs);

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
