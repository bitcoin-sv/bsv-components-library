package com.nchain.jcl.store.blockChainStore.validation.rules;

import com.nchain.jcl.store.blockChainStore.BlockChainStore;
import com.nchain.jcl.store.blockChainStore.validation.exception.BlockChainRuleFailureException;
import com.nchain.jcl.tools.util.PowUtil;
import io.bitcoinj.bitcoin.api.base.HeaderReadOnly;
import io.bitcoinj.bitcoin.api.extended.ChainInfo;
import io.bitcoinj.core.Utils;

import java.math.BigInteger;
import java.util.List;
import java.util.function.Predicate;

public class DifficultyTransitionPointRule extends AbstractBlockChainRule {

    private final BlockChainStore blockChainStore;
    private final BigInteger maxTarget;
    private final int blockDifficultyAdjustmentInterval;
    private final int targetTimespan;

    public DifficultyTransitionPointRule(List<Predicate<ChainInfo>> predicates, BlockChainStore blockChainStore, BigInteger maxTarget, int blockDifficultyAdjustmentInterval, int targetTimespan) {
        super(predicates);
        this.blockChainStore = blockChainStore;
        this.maxTarget = maxTarget;
        this.blockDifficultyAdjustmentInterval = blockDifficultyAdjustmentInterval;
        this.targetTimespan = targetTimespan;
    }

    @Override
    public void checkRule(ChainInfo candidateBlock) throws BlockChainRuleFailureException {
        ChainInfo prevBlockChainInfo = blockChainStore.getBlockChainInfo(candidateBlock.getHeader().getPrevBlockHash()).get();

        HeaderReadOnly lastBlockInterval = findLastBlockInterval(prevBlockChainInfo);
        int timeSpan = (int) (prevBlockChainInfo.getHeader().getTime() - lastBlockInterval.getTime());
        BigInteger newTarget = calculateNewTarget(prevBlockChainInfo.getHeader(), timeSpan);

        PowUtil.verifyDifficulty(maxTarget, newTarget, candidateBlock.getHeader().getDifficultyTarget());
    }

    private HeaderReadOnly findLastBlockInterval(ChainInfo prevBlock) throws BlockChainRuleFailureException {
        ChainInfo referenceBlockChainInfo = blockChainStore.getBlock(prevBlock.getHeight() - blockDifficultyAdjustmentInterval - 1).orElseThrow(() -> new BlockChainRuleFailureException("Not enough blocks to check difficulty."));

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
