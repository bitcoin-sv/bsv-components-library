/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.store.blockChainStore.validation.rules.predicate;

import io.bitcoinsv.jcl.tools.util.PowUtil;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.extended.ChainInfo;

import java.math.BigInteger;
import java.util.function.Predicate;

/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2021 nChain Ltd
 * @date 25/02/2021
 */
public class DifficultyEqualtoMaxTargetPredicate implements Predicate<ChainInfo> {

    private final BigInteger maxTarget;

    public DifficultyEqualtoMaxTargetPredicate( BigInteger maxTarget) {
        this.maxTarget = maxTarget;
    }

    @Override
    public boolean test(ChainInfo chainInfo) {
        return PowUtil.hasEqualDifficulty(chainInfo.getHeader().getDifficultyTarget(), maxTarget);
    }
}
