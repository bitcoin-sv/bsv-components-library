package com.nchain.jcl.store.blockChainStore.validation.rules;

import io.bitcoinj.bitcoin.api.extended.ChainInfo;

import java.util.List;
import java.util.function.Predicate;

/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2021 nChain Ltd
 * @date 25/02/2021
 */
public abstract class AbstractBlockChainRule implements BlockChainRule {

    List<Predicate<ChainInfo>> predicateList;

    public AbstractBlockChainRule(List<Predicate<ChainInfo>> predicateList){
        this.predicateList = predicateList;
    }

    public boolean applies(ChainInfo candidateBlock) {
        for(Predicate<ChainInfo> predicate : predicateList){
            if(!predicate.test(candidateBlock)){
                return false;
            }
        }

        return true;
    }
}
