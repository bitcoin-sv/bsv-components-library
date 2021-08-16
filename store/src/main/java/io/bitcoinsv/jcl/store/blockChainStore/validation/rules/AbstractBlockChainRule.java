package io.bitcoinsv.jcl.store.blockChainStore.validation.rules;

import io.bitcoinj.bitcoin.api.extended.ChainInfo;

import java.util.function.Predicate;

/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2021 nChain Ltd
 * @date 25/02/2021
 */
public abstract class AbstractBlockChainRule implements BlockChainRule {

    Predicate<ChainInfo> predicate;

    public AbstractBlockChainRule(Predicate<ChainInfo> predicate){
        this.predicate = predicate;
    }

    public AbstractBlockChainRule(){
    }

    public boolean applies(ChainInfo candidateBlock) {
        if(predicate != null) {
            if (!predicate.test(candidateBlock)) {
                return false;
            }
        }

        return true;
    }
}
