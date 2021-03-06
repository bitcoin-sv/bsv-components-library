package io.bitcoinsv.jcl.store.blockChainStore.validation.rules.predicate;


import io.bitcoinsv.bitcoinjsv.bitcoin.api.extended.ChainInfo;

import java.util.function.Predicate;

/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2021 nChain Ltd
 * @date 25/02/2021
 */
public class DifficultyAdjustmentActivatedPredicate implements Predicate<ChainInfo> {

    private final int daaActivationHeight;

    public DifficultyAdjustmentActivatedPredicate(int daaActivationHeight) {
        this.daaActivationHeight = daaActivationHeight;
    }

    @Override
    public boolean test(ChainInfo chainInfo) {
        return chainInfo.getHeight() >= daaActivationHeight;
    }

}
