package com.nchain.jcl.store.blockChainStore;

import com.nchain.jcl.base.domain.api.extended.ChainInfo;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * It stores the current State of the BlockChain Store at a point in time
 */
@Builder
@Value
public class BlockChainStoreState {
    private List<ChainInfo> tipsChains;
    private long numBlocks;
    private long numTxs;
}
