package com.nchain.jcl.store.blockChainStore.validation.rules;

import com.nchain.jcl.store.blockChainStore.BlockChainStore;
import com.nchain.jcl.store.blockChainStore.validation.exception.BlockChainRuleFailureException;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.extended.ChainInfo;

/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2021 nChain Ltd
 * @date 24/02/2021
 */
public interface BlockChainRule {
    boolean applies(ChainInfo candidateBlock);
    void checkRule(ChainInfo candidateBlock,  BlockChainStore blockChainStore) throws BlockChainRuleFailureException;
}
