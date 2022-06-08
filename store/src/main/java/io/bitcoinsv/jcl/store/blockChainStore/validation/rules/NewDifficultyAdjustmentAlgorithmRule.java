package io.bitcoinsv.jcl.store.blockChainStore.validation.rules;

import io.bitcoinsv.jcl.store.blockChainStore.BlockChainStore;
import io.bitcoinsv.jcl.store.blockChainStore.validation.exception.BlockChainRuleFailureException;
import io.bitcoinsv.jcl.tools.util.PowUtil;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.extended.ChainInfo;
import io.bitcoinsv.bitcoinjsv.core.Sha256Hash;
import io.bitcoinsv.bitcoinjsv.core.Verification;


import java.math.BigInteger;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2021 nChain Ltd
 * @date 24/02/2021
 *

 *  The new DAA algorithm seeks to accomplish the following objectives:
 *  - Adjust difficulty to hash rate to target a mean block interval of 600 seconds.
 *  - Avoid sudden changes in difficulty when hash rate is fairly stable.
 *  - Adjust difficulty rapidly when hash rate changes rapidly.
 *  - Avoid oscillations from feedback between hash rate and difficulty.
 *  - Be resilient to attacks such as timestamp manipulation.
 *
 *  https://www.bitcoinabc.org/november
 *
 */
public class NewDifficultyAdjustmentAlgorithmRule extends AbstractBlockChainRule {

    private static final int AVERAGE_BLOCKS_PER_DAY = 144;
    private final BigInteger maxTarget;

    public NewDifficultyAdjustmentAlgorithmRule(Predicate<ChainInfo> predicate, BigInteger maxTarget){
        super(predicate);
        this.maxTarget = maxTarget;
    }

    @Override
    public void checkRule(ChainInfo candidateBlock, BlockChainStore blockChainStore) throws BlockChainRuleFailureException {
        checkNextCashWorkRequired(candidateBlock, blockChainStore);
    }

    /**
     * Compute the next required proof of work using a weighted average of the
     * estimated hashrate per block.
     * <p>
     * Using a weighted average ensure that the timestamp parameter cancels out in
     * most of the calculation - except for the timestamp of the first and last
     * block. Because timestamps are the least trustworthy information we have as
     * input, this ensures the algorithm is more resistant to malicious inputs.
     */
    private void checkNextCashWorkRequired(ChainInfo candidateBlock, BlockChainStore blockChainStore) throws BlockChainRuleFailureException {
        ChainInfo last = GetMostSuitableBlock(candidateBlock.getHeader().getHash(), candidateBlock.getHeight(), blockChainStore);
        ChainInfo first = GetMostSuitableBlock(candidateBlock.getHeader().getHash(), candidateBlock.getHeight() - AVERAGE_BLOCKS_PER_DAY, blockChainStore);

            BigInteger nextTarget = Verification.ComputeTarget(
                    first.getChainWork(), first.getHeader().getTime(), first.getHeight(),
                    last.getChainWork(), last.getHeader().getTime(), last.getHeight());
            PowUtil.verifyDifficulty(maxTarget, nextTarget, candidateBlock.getHeader().getDifficultyTarget());
    }

    /**
     * To reduce the impact of timestamp manipulation, we select the block we are
     * basing our computation on via a median of 3.
     */
    private ChainInfo GetMostSuitableBlock(Sha256Hash candidateBlockHash, int candiateBlockHeight, BlockChainStore blockChainStore) throws BlockChainRuleFailureException {
        /**
         * In order to avoid a block is a very skewed timestamp to have too much
         * influence, we select the median of the 3 top most blocks as a starting
         * point.
         */
        Optional<ChainInfo> blocksAtHeightMinus1 = blockChainStore.getAncestorByHeight(candidateBlockHash, candiateBlockHeight - 1);
        Optional<ChainInfo> blocksAtHeightMinus2 = blockChainStore.getAncestorByHeight(candidateBlockHash, candiateBlockHeight - 2);
        Optional<ChainInfo> blocksAtHeightMinus3 = blockChainStore.getAncestorByHeight(candidateBlockHash, candiateBlockHeight - 3);
        Optional<ChainInfo> firstBlockAtHeightMinus1 = Optional.of((blocksAtHeightMinus1 != null && !blocksAtHeightMinus1.isEmpty()) ? blocksAtHeightMinus1.get() : null);
        Optional<ChainInfo> firstBlockAtHeightMinus2 = Optional.of((blocksAtHeightMinus2 != null && !blocksAtHeightMinus2.isEmpty()) ? blocksAtHeightMinus2.get() : null);
        Optional<ChainInfo> firstBlockAtHeightMinus3 = Optional.of((blocksAtHeightMinus3 != null && !blocksAtHeightMinus3.isEmpty()) ? blocksAtHeightMinus3.get() : null);

        ChainInfo blocks[] = new ChainInfo[3];
        blocks[2] = firstBlockAtHeightMinus1.orElseThrow(() -> new BlockChainRuleFailureException("Not enough blocks in blockStore to calculate difficulty"));
        blocks[1] = firstBlockAtHeightMinus2.orElseThrow(() -> new BlockChainRuleFailureException("Not enough blocks in blockStore to calculate difficulty"));
        blocks[0] = firstBlockAtHeightMinus3.orElseThrow(() -> new BlockChainRuleFailureException("Not enough blocks in blockStore to calculate difficulty"));

        // Sorting network.
        if (blocks[0].getHeader().getTime() > blocks[2].getHeader().getTime()) {
            ChainInfo temp = blocks[0];
            blocks[0] = blocks[2];
            blocks[2] = temp;
        }

        if (blocks[0].getHeader().getTime() > blocks[1].getHeader().getTime()) {
            ChainInfo temp = blocks[0];
            blocks[0] = blocks[1];
            blocks[1] = temp;
        }

        if (blocks[1].getHeader().getTime() > blocks[2].getHeader().getTime()) {
            ChainInfo temp = blocks[1];
            blocks[1] = blocks[2];
            blocks[2] = temp;
        }

        // We should have our candidate in the middle now.
        return blocks[1];
    }
}
