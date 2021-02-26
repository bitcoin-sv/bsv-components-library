package com.nchain.jcl.store.blockChainStore.validation.rules;

import com.nchain.jcl.store.blockChainStore.BlockChainStore;
import com.nchain.jcl.store.blockChainStore.validation.exception.BlockChainRuleFailureException;
import com.nchain.jcl.tools.util.PowUtil;
import io.bitcoinj.bitcoin.api.extended.ChainInfo;

import java.math.BigInteger;
import java.util.function.Predicate;

public class MinimalDifficultyNoChangedRule extends AbstractBlockChainRule {

    private final BigInteger maxTarget;
    private final BlockChainStore blockChainStore;

    public MinimalDifficultyNoChangedRule(Predicate<ChainInfo> predicate, BlockChainStore blockChainStore, BigInteger maxTarget) {
        super(predicate);
        this.blockChainStore = blockChainStore;
        this.maxTarget = maxTarget;
    }

    @Override
    public void checkRule(ChainInfo candidateBlock) throws BlockChainRuleFailureException {
        ChainInfo prevBlockChainInfo = blockChainStore.getBlockChainInfo(candidateBlock.getHeader().getPrevBlockHash()).get();

        if (PowUtil.hasEqualDifficulty(prevBlockChainInfo.getHeader().getDifficultyTarget(), maxTarget)) {
            if (!PowUtil.hasEqualDifficulty(prevBlockChainInfo.getHeader(), candidateBlock.getHeader())) {
                throw new BlockChainRuleFailureException("Unexpected change in difficulty at height " +
                        prevBlockChainInfo.getHeight() + ": " +
                        Long.toHexString(candidateBlock.getHeader().getDifficultyTarget()) + " vs " +
                        Long.toHexString(prevBlockChainInfo.getHeader().getDifficultyTarget()));
            }
        }
    }

}
