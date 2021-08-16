/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.store.blockChainStore.validation.rules;

import io.bitcoinsv.jcl.store.blockChainStore.BlockChainStore;
import io.bitcoinsv.jcl.store.blockChainStore.validation.exception.BlockChainRuleFailureException;
import io.bitcoinsv.jcl.tools.util.PowUtil;
import io.bitcoinj.bitcoin.api.base.HeaderReadOnly;
import io.bitcoinj.bitcoin.api.extended.ChainInfo;

import java.math.BigInteger;
import java.util.function.Predicate;

/**
 * Walk backwards until we find a block that doesn't have the easiest proof of work,
 * then check that difficulty is equal to that one.
 */
public class LastNonMinimalDifficultyRule extends AbstractBlockChainRule {

    private final BigInteger maxTarget;
    private final int blockDifficultyAdjustmentInterval;
    private final int targetSpacing;

    public LastNonMinimalDifficultyRule(Predicate<ChainInfo> predicate, BigInteger maxTarget, int blockDifficultyAdjustmentInterval, int targetSpacing) {
        super(predicate);
        this.maxTarget = maxTarget;
        this.blockDifficultyAdjustmentInterval = blockDifficultyAdjustmentInterval;
        this.targetSpacing = targetSpacing;
    }

    @Override
    public void checkRule(ChainInfo candidateBlock, BlockChainStore blockChainStore) throws BlockChainRuleFailureException {
        ChainInfo prevBlockChainInfo = blockChainStore.getBlockChainInfo(candidateBlock.getHeader().getPrevBlockHash()).orElseThrow(() -> new BlockChainRuleFailureException("Not enough blocks to check last non minimal difficulty rule."));
        if (isUnderPeriod(prevBlockChainInfo.getHeader(), candidateBlock.getHeader())) {
            checkLastNonMinimalDifficultyIsSet(prevBlockChainInfo, candidateBlock, blockChainStore);
        }
    }

    private boolean isUnderPeriod(HeaderReadOnly prevBlock, HeaderReadOnly candidateBlock) {
        final long timeDelta = candidateBlock.getTime() - prevBlock.getTime();
        return timeDelta >= 0 && timeDelta <= targetSpacing * 2;
    }

    private void checkLastNonMinimalDifficultyIsSet(ChainInfo prevBlock, ChainInfo candidateBlock,  BlockChainStore blockChainStore) throws BlockChainRuleFailureException {

        HeaderReadOnly lastNotEasiestPowBlock = findLastNotEasiestPowBlock(prevBlock, blockChainStore);

        if (!PowUtil.hasEqualDifficulty(lastNotEasiestPowBlock, candidateBlock.getHeader()))
            throw new BlockChainRuleFailureException("Testnet block transition that is not allowed: " +
                    Long.toHexString(lastNotEasiestPowBlock.getDifficultyTarget()) + " vs " +
                    Long.toHexString(candidateBlock.getHeader().getDifficultyTarget()));

    }

    private HeaderReadOnly findLastNotEasiestPowBlock(ChainInfo storedPrev, BlockChainStore blockChainStore) {
        ChainInfo cursor = storedPrev;

        HeaderReadOnly genesis = blockChainStore.getBlock(0).get(0).getHeader();

        //TODO optimize
        while (!cursor.equals(genesis) &&
                cursor.getHeight() % blockDifficultyAdjustmentInterval != 0 &&
                PowUtil.hasEqualDifficulty(cursor.getHeader().getDifficultyTarget(), maxTarget)) {
            cursor = blockChainStore.getBlockChainInfo(cursor.getHeader().getPrevBlockHash()).get();
        }

        return cursor.getHeader();
    }

}
