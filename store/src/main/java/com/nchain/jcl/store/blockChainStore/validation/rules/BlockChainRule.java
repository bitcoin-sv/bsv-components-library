package com.nchain.jcl.store.blockChainStore.validation.rules;

import com.nchain.jcl.store.blockChainStore.validation.exception.BlockChainRuleFailureException;
import io.bitcoinj.bitcoin.api.extended.ChainInfo;

/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2021 nChain Ltd
 * @date 24/02/2021
 */
public interface BlockChainRule {
    boolean applies(ChainInfo candidateBlock);
    void checkRule(ChainInfo candidateBlock) throws BlockChainRuleFailureException;
}
