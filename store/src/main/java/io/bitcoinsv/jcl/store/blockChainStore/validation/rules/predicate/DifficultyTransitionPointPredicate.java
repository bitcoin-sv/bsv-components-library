package io.bitcoinsv.jcl.store.blockChainStore.validation.rules.predicate;

import io.bitcoinj.bitcoin.api.extended.ChainInfo;

import java.util.function.Predicate;

/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2021 nChain Ltd
 * @date 25/02/2021
 */
public class DifficultyTransitionPointPredicate implements Predicate<ChainInfo> {

    private final int blockDifficultyAdjustmentInterval;

    public DifficultyTransitionPointPredicate(int blockDifficultyAdjustmentInterval) {
        this.blockDifficultyAdjustmentInterval = blockDifficultyAdjustmentInterval;
    }

    @Override
    public boolean test(ChainInfo chainInfo) {
        return chainInfo.getHeight() % blockDifficultyAdjustmentInterval == 0;
    }
}
