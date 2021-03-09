package com.nchain.jcl.store.blockChainStore.validation.rules;

import com.nchain.jcl.store.blockChainStore.BlockChainStore;
import com.nchain.jcl.store.blockChainStore.validation.exception.BlockChainRuleFailureException;
import com.nchain.jcl.tools.util.PowUtil;
import io.bitcoinj.bitcoin.api.base.HeaderReadOnly;
import io.bitcoinj.bitcoin.api.extended.ChainInfo;
import io.bitcoinj.core.Utils;

import java.math.BigInteger;
import java.util.function.Predicate;

public class DifficultyTransitionPointRule extends AbstractBlockChainRule {

    private final BigInteger maxTarget;
    private final int blockDifficultyAdjustmentInterval;
    private final int targetTimespan;

    public DifficultyTransitionPointRule(Predicate<ChainInfo> predicate, BigInteger maxTarget, int blockDifficultyAdjustmentInterval, int targetTimespan) {
        super(predicate);
        this.maxTarget = maxTarget;
        this.blockDifficultyAdjustmentInterval = blockDifficultyAdjustmentInterval;
        this.targetTimespan = targetTimespan;
    }

    @Override
    public void checkRule(ChainInfo candidateBlock, BlockChainStore blockChainStore) throws BlockChainRuleFailureException {
        ChainInfo prevBlockChainInfo = blockChainStore.getBlockChainInfo(candidateBlock.getHeader().getPrevBlockHash()).orElseThrow(() -> new BlockChainRuleFailureException("Not enough blocks to check difficulty transition rule."));

        HeaderReadOnly lastBlockInterval = findLastBlockInterval(candidateBlock, blockChainStore);
        int timeSpan = (int) (prevBlockChainInfo.getHeader().getTime() - lastBlockInterval.getTime());
        BigInteger newTarget = calculateNewTarget(prevBlockChainInfo.getHeader(), timeSpan);

        PowUtil.verifyDifficulty(maxTarget, newTarget, candidateBlock.getHeader().getDifficultyTarget());
    }

    private HeaderReadOnly findLastBlockInterval(ChainInfo candidateBlock, BlockChainStore blockChainStore) throws BlockChainRuleFailureException {
        ChainInfo referenceBlockChainInfo = blockChainStore.getBlock(candidateBlock.getHeight() - blockDifficultyAdjustmentInterval).orElseThrow(() -> new BlockChainRuleFailureException("Not enough blocks to check difficulty transition rule."));

        return referenceBlockChainInfo.getHeader();
    }

    private BigInteger calculateNewTarget(HeaderReadOnly prevBlock, int timeSpan) {
        if (timeSpan < targetTimespan / 4) {
            timeSpan = targetTimespan / 4;
        } else if (timeSpan > targetTimespan * 4) {
            timeSpan = targetTimespan * 4;
        }

        return Utils.decodeCompactBits(prevBlock.getDifficultyTarget())
                .multiply(BigInteger.valueOf(timeSpan))
                .divide(BigInteger.valueOf(targetTimespan));
    }

}
