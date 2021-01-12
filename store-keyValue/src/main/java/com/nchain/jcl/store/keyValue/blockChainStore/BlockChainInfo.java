package com.nchain.jcl.store.keyValue.blockChainStore;

import lombok.Builder;
import lombok.Value;

import java.io.Serializable;
import java.math.BigInteger;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * It stores info about the Chain, relative to the position of one specific Block.
 * This class is an alternative version of the {@link com.nchain.jcl.base.domain.api.extended.ChainInfo} class,
 * storing the same basic info, but this class does not store the whole BlockHeader, instead it only stored the Hash.
 * This class is used to store the relative Chain info for each Block. Sicne the Blocks themselves are
 * already being stored in other keys, we only need the Block Hash here.
 *
 * If a Block is "Connected" to a Chain, then there is an entry ("b_chain:[blockHash]")for that block, storing this
 * instane as the value (for there are at least 2 entries for that block, the "regular" one taht stores the block
 * itself, and this one).
 *
 * NOTE: A block is "connected" to a Chain if there is an entry for that Block containing this info.
 */
@Builder(toBuilder = true)
@Value
public class BlockChainInfo implements Serializable {
    private String blockHash;
    private BigInteger chainWork;
    private int height;
    private long totalChainSize;
}
