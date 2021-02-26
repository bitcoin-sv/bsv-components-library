package com.nchain.jcl.store.blockChainStore.validation.rules;

import com.nchain.jcl.store.blockChainStore.BlockChainStore;
import com.nchain.jcl.store.blockChainStore.validation.exception.BlockChainRuleFailureException;
import com.nchain.jcl.tools.util.PowUtil;
import io.bitcoinj.bitcoin.api.extended.ChainInfo;
import io.bitcoinj.core.Verification;

import java.math.BigInteger;
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
    private final BlockChainStore blockChainStore;
    private final int blockDifficultyAdjustmentInterval;
    private final BigInteger maxTarget;

    public NewDifficultyAdjustmentAlgorithmRule(Predicate<ChainInfo> predicate, BlockChainStore blockChainStore, BigInteger maxTarget, int blockDifficultyAdjustmentInterval){
        super(predicate);
        this.blockChainStore = blockChainStore;
        this.blockDifficultyAdjustmentInterval = blockDifficultyAdjustmentInterval;
        this.maxTarget = maxTarget;
    }

    @Override
    public void checkRule(ChainInfo candidateBlock) throws BlockChainRuleFailureException {
        checkNextCashWorkRequired(candidateBlock);
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
    private void checkNextCashWorkRequired(ChainInfo candidateBlock) throws BlockChainRuleFailureException {
        ChainInfo prevBlockChainInfo = blockChainStore.getBlockChainInfo(candidateBlock.getHeader().getPrevBlockHash()).get();

        if(prevBlockChainInfo.getHeight() < blockDifficultyAdjustmentInterval )
            throw new BlockChainRuleFailureException("previous block chain height is less than block difficulty adjustment interval");


        ChainInfo last = GetMostSuitableBlock(candidateBlock.getHeight());
        ChainInfo first = GetMostSuitableBlock(candidateBlock.getHeight() - AVERAGE_BLOCKS_PER_DAY);

            BigInteger nextTarget = Verification.ComputeTarget(
                    first.getChainWork(), first.getHeader().getTime(), first.getHeight(),
                    last.getChainWork(), last.getHeader().getTime(), last.getHeight());
            PowUtil.verifyDifficulty(maxTarget, nextTarget, candidateBlock.getHeader().getDifficultyTarget());

    }

    /**
     * To reduce the impact of timestamp manipulation, we select the block we are
     * basing our computation on via a median of 3.
     */
    private ChainInfo GetMostSuitableBlock(int candidateBlockIndex) throws BlockChainRuleFailureException {
        /**
         * In order to avoid a block is a very skewed timestamp to have too much
         * influence, we select the median of the 3 top most blocks as a starting
         * point.
         */
        ChainInfo blocks[] = new ChainInfo[3];
        blocks[2] = blockChainStore.getBlock(candidateBlockIndex).get();
        blocks[1] = blockChainStore.getBlock(candidateBlockIndex - 1).orElseThrow(() -> new BlockChainRuleFailureException("Not enough blocks in blockStore to calculate difficulty"));
        blocks[0] = blockChainStore.getBlock(candidateBlockIndex - 2).orElseThrow(() -> new BlockChainRuleFailureException("Not enough blocks in blockStore to calculate difficulty"));

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
