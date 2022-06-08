package io.bitcoinsv.jcl.store.blockChainStore.validation.rules;

import io.bitcoinsv.jcl.store.blockChainStore.BlockChainStore;
import io.bitcoinsv.jcl.store.blockChainStore.validation.exception.BlockChainRuleFailureException;
import io.bitcoinsv.jcl.tools.util.PowUtil;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.extended.ChainInfo;
import io.bitcoinsv.bitcoinjsv.core.Utils;
import io.bitcoinsv.bitcoinjsv.exception.VerificationException;


import java.math.BigInteger;
import java.util.function.Predicate;

/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2021 nChain Ltd
 * @date 24/02/2021
 */
public class MinimalDifficultyRule extends AbstractBlockChainRule {

    private final BigInteger maxTarget;
    private final int targetSpacing;

    public MinimalDifficultyRule(Predicate<ChainInfo> predicate, BigInteger maxtarget, int targetSpacing){
        super(predicate);
        this.maxTarget = maxtarget;
        this.targetSpacing = targetSpacing;
    }

    @Override
    public void checkRule(ChainInfo candidateBlock, BlockChainStore blockChainStore) throws BlockChainRuleFailureException {
        ChainInfo prevBlockChainInfo = blockChainStore.getBlockChainInfo(candidateBlock.getHeader().getPrevBlockHash()).orElseThrow(() -> new BlockChainRuleFailureException("Candidate block given has no previous block"));

        if(isPeriodExceed(prevBlockChainInfo, candidateBlock)){
            checkMinimalDifficultyIsSet(candidateBlock);
        }
    }

    /**
     * There is an integer underflow bug in bitcoin-qt that means mindiff blocks are accepted
     * when time goes backwards.
     */
    private boolean isPeriodExceed(ChainInfo prevBlock, ChainInfo candidateBlock) {
        final long timeDelta = candidateBlock.getHeader().getTime() - prevBlock.getHeader().getTime();
        return timeDelta >= 0 && timeDelta > targetSpacing * 2;
    }


    private void checkMinimalDifficultyIsSet(ChainInfo candidateBlock) {
        if (!PowUtil.hasEqualDifficulty(candidateBlock.getHeader().getDifficultyTarget(), maxTarget)) {
            throw new VerificationException("Testnet block transition that is not allowed: " +
                    Long.toHexString(Utils.encodeCompactBits(maxTarget)) + " (required min difficulty) vs " +
                    Long.toHexString(candidateBlock.getHeader().getDifficultyTarget()));
        }
    }
}
