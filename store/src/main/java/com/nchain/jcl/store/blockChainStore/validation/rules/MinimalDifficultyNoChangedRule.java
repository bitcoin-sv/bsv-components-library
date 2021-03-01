package com.nchain.jcl.store.blockChainStore.validation.rules;

import com.nchain.jcl.store.blockChainStore.BlockChainStore;
import com.nchain.jcl.store.blockChainStore.validation.exception.BlockChainRuleFailureException;
import com.nchain.jcl.tools.util.PowUtil;
import io.bitcoinj.bitcoin.api.extended.ChainInfo;

import java.math.BigInteger;
import java.util.function.Predicate;

public class MinimalDifficultyNoChangedRule extends AbstractBlockChainRule {

    private final BigInteger maxTarget;

    public MinimalDifficultyNoChangedRule(Predicate<ChainInfo> predicate, BigInteger maxTarget) {
        super(predicate);
        this.maxTarget = maxTarget;
    }

    @Override
    public void checkRule(ChainInfo candidateBlock,  BlockChainStore blockChainStore) throws BlockChainRuleFailureException {
        ChainInfo prevBlockChainInfo = blockChainStore.getBlockChainInfo(candidateBlock.getHeader().getPrevBlockHash()).orElseThrow(() -> new BlockChainRuleFailureException("Not enough blocks to check difficulty no changed rule."));

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
