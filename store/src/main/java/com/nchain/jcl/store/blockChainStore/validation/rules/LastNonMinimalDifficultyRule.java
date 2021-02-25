package com.nchain.jcl.store.blockChainStore.validation.rules;

import com.nchain.jcl.store.blockChainStore.BlockChainStore;
import com.nchain.jcl.store.blockChainStore.validation.exception.BlockChainRuleFailureException;
import com.nchain.jcl.tools.util.PowUtil;
import io.bitcoinj.bitcoin.api.base.HeaderReadOnly;
import io.bitcoinj.bitcoin.api.extended.ChainInfo;
import io.bitcoinj.params.NetworkParameters;

import java.math.BigInteger;
import java.util.List;
import java.util.function.Predicate;

/**
 * Walk backwards until we find a block that doesn't have the easiest proof of work,
 * then check that difficulty is equal to that one.
 */
public class LastNonMinimalDifficultyRule extends AbstractBlockChainRule {

    private final BigInteger maxTarget;
    private final BlockChainStore blockChainStore;
    private final int blockDifficultyAdjustmentInterval;

    public LastNonMinimalDifficultyRule(List<Predicate<ChainInfo>> predicates, BlockChainStore blockChainStore, BigInteger maxTarget, int blockDifficultyAdjustmentInterval) {
        super(predicates);
        this.blockChainStore = blockChainStore;
        this.maxTarget = maxTarget;
        this.blockDifficultyAdjustmentInterval = blockDifficultyAdjustmentInterval;
    }

    @Override
    public void checkRule(ChainInfo candidateBlock) throws BlockChainRuleFailureException {
        ChainInfo prevBlockChainInfo = blockChainStore.getBlockChainInfo(candidateBlock.getHeader().getPrevBlockHash()).get();
        if (isUnderPeriod(prevBlockChainInfo.getHeader(), candidateBlock.getHeader())) {
            checkLastNonMinimalDifficultyIsSet(prevBlockChainInfo, candidateBlock);
        }
    }

    private boolean isUnderPeriod(HeaderReadOnly prevBlock, HeaderReadOnly candidateBlock) {
        final long timeDelta = candidateBlock.getTime() - prevBlock.getTime();
        return timeDelta >= 0 && timeDelta <= NetworkParameters.TARGET_SPACING * 2;
    }

    private void checkLastNonMinimalDifficultyIsSet(ChainInfo prevBlock, ChainInfo candidateBlock) throws BlockChainRuleFailureException {

        HeaderReadOnly lastNotEasiestPowBlock = findLastNotEasiestPowBlock(prevBlock);

        if (!PowUtil.hasEqualDifficulty(lastNotEasiestPowBlock, candidateBlock.getHeader()))
            throw new BlockChainRuleFailureException("Testnet block transition that is not allowed: " +
                    Long.toHexString(lastNotEasiestPowBlock.getDifficultyTarget()) + " vs " +
                    Long.toHexString(candidateBlock.getHeader().getDifficultyTarget()));

    }

    private HeaderReadOnly findLastNotEasiestPowBlock(ChainInfo storedPrev) {
        ChainInfo cursor = storedPrev;
        HeaderReadOnly genesis = blockChainStore.getBlock(0).get().getHeader();

        //TODO optimize
        while (!cursor.equals(genesis) &&
                cursor.getHeight() % blockDifficultyAdjustmentInterval != 0 &&
                PowUtil.hasEqualDifficulty(cursor.getHeader().getDifficultyTarget(), maxTarget)) {
            cursor = blockChainStore.getBlockChainInfo(cursor.getHeader().getPrevBlockHash()).get();
        }

        return cursor.getHeader();
    }

}
