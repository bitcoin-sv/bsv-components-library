package com.nchain.jcl.blockStore;

import com.nchain.jcl.base.domain.api.base.BlockHeader;
import com.nchain.jcl.base.domain.api.base.Tx;
import com.nchain.jcl.base.domain.api.extended.ChainInfo;
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper;
import com.nchain.jcl.blockStore.events.BlockStoreStreamer;

import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A BlockStorage Component provides operatiosn to store, retreive and remove Blocks and Transactions.
 * Different implementations could provide different performances.
 *
 * The BlockStorage componetn also provides operations that work at Chain-level. In this case, more
 * information is added to the Block, information about the height of the Block, accumulate work in the
 * chain from the beggining up to that block, etc.
 *
 * Finally, the BlockStorage components also provides an EventStreamer, which can be used to subscribe to
 * different Events so your application can react to them.
 *
 */
public interface BlockStore {

    // Block Storage Operations:

    /** Saves the Block given */
    void saveBlock(BlockHeader blockHeader);
    /** Removes the Block with the HASH given */
    void removeBlock(Sha256Wrapper blockHash);
    /** Remvoed the block with the HASH (in HEX format) given */
    void removeBlock(String blockHashHex);
    /** Retrieves the Block with the HASH given */
    BlockHeader getBlock(Sha256Wrapper blockHash);
    /** Retrieves the Block with the HASH (in HEX format) given */
    BlockHeader getBlock(String blockHashHex);


    // Tx Storage Operations:

    /** Saves the TX given */
    void saveTx(Tx tx);
    /** Removes the TX with the HASH given */
    void removeTx(Sha256Wrapper txHash);
    /** Removes the TX with the HASH (in HEX format) given */
    void removeTx(String txHashHex);
    /** Retrieves the tX with the HASH given */
    Tx getTx(Sha256Wrapper txHash);
    /** Retrieves the TX with the HASH (in HEX format) given */
    Tx getTx(String txHashHex);
    /** Returns the TXs belonging to the Block with the HASH given */
    PaginatedResult<Tx> getTxs(Sha256Wrapper blockHash, PaginatedRequest pagReq);
    /** Returns the TXS belonging the the block with the HASH (in HEX format) given */
    PaginatedResult<Tx> getTxs(String blockHashHex, PaginatedResult pagReq);

    // Chain Info Operations:

    /** Returns the ChainInfo for the Block with the HASH given */
    ChainInfo getChainInfo(Sha256Wrapper blockHash);
    /** Returns the ChainInfo fof the block with the HASH (in HEX Format) given */
    ChainInfo getChainInfo(String blockHashHex);
    /** Prunes the chain starting from the block given. This blocks and all its descendant will be removed */
    void pruneBlock(Sha256Wrapper blockHash);
    /** Prunes the chain starting from the block given. This blocks and all its descendant will be removed */
    void pruneBlock(String blockHashHex);
    List<ChainInfo> getChainsTips();
    /**
     * Returns all the Blocks belonging to the chain pointed at by the Block given. For each block, a complete
     * ChainInfo is provided.
     */
    PaginatedResult<ChainInfo> getBlocksChain(Sha256Wrapper blockHash, PaginatedRequest pagReq);
    /**
     * Returns all the Blocks belonging to the chain pointed at by the Block given. For each block, a complete
     * ChainInfo is provided.
     */
    PaginatedResult<ChainInfo> getBlocksChain(String blockHashHex, PaginatedRequest pagReq);

    // Events Streaming:

    /**
     * It returns a blockStorageStreamer, which can be used to subscribe to all the different types of Events that
     * can be triggered by the BlockStorage Component
     */
    BlockStoreStreamer EVENTS();

}
