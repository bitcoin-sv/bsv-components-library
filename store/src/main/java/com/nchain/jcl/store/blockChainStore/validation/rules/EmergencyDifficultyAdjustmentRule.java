package com.nchain.jcl.store.blockChainStore.validation.rules;

import com.nchain.jcl.store.blockChainStore.BlockChainStore;
import com.nchain.jcl.store.blockChainStore.validation.exception.BlockChainRuleFailureException;
import com.nchain.jcl.tools.util.PowUtil;
import io.bitcoinj.bitcoin.api.base.HeaderReadOnly;
import io.bitcoinj.bitcoin.api.extended.ChainInfo;
import io.bitcoinj.core.Utils;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.function.Predicate;

public class EmergencyDifficultyAdjustmentRule extends AbstractBlockChainRule {

    private static final long TARGET_PRODUCTION_TIME_IN_SECONDS = 12 * 60 * 60; // 12 hours
    private static final int REFERENCE_OF_BLOCKS_PRODUCED_SIZE = 6;
    private static final int REFERENCE_BEFORE_BLOCK_DISTANCE = 10;

    private final BlockChainStore blockChainStore;
    private final BigInteger maxTarget;

    public EmergencyDifficultyAdjustmentRule(Predicate<ChainInfo> predicate, BlockChainStore blockChainStore, BigInteger maxTarget) {
        super(predicate);
        this.blockChainStore = blockChainStore;
        this.maxTarget = maxTarget;
    }

    @Override
    public void checkRule(ChainInfo candidateBlock) throws BlockChainRuleFailureException {
        ChainInfo prevBlockChainInfo = blockChainStore.getBlockChainInfo(candidateBlock.getHeader().getPrevBlockHash()).get();

        long lastBlocksMPTinSeconds = getMedianProducingTimeInSeconds(prevBlockChainInfo);

        checkEDARules(prevBlockChainInfo, candidateBlock, lastBlocksMPTinSeconds);
    }

    private long getMedianProducingTimeInSeconds(ChainInfo storedPrev) throws BlockChainRuleFailureException {
        ChainInfo referenceBlockChainInfo = blockChainStore.getBlock(storedPrev.getHeight() - REFERENCE_OF_BLOCKS_PRODUCED_SIZE).orElseThrow(() -> new BlockChainRuleFailureException("Not enough blocks to check difficulty."));

        if(referenceBlockChainInfo.getHeight() - REFERENCE_BEFORE_BLOCK_DISTANCE < 0 ) {
            throw new BlockChainRuleFailureException("Not enough blocks to check difficulty.");
        }

        return getMedianTimestampOfRecentBlocks(storedPrev) -
                getMedianTimestampOfRecentBlocks(referenceBlockChainInfo);
    }

    private void checkEDARules(ChainInfo prevBlock, ChainInfo candidateBlock, long lastBlocksMPTinSeconds) throws BlockChainRuleFailureException {

        if (needToReduceTheDifficulty(lastBlocksMPTinSeconds)) {
            BigInteger nPow = calculateReducedDifficulty(prevBlock.getHeader());
            if (!PowUtil.hasEqualDifficulty(candidateBlock.getHeader().getDifficultyTarget(), nPow)) {
                throwUnexpectedReducedDifficultyException(prevBlock, candidateBlock, nPow);
            }
        } else {
            if (!PowUtil.hasEqualDifficulty(prevBlock.getHeader(), candidateBlock.getHeader())) {
                throwUnexpectedDifficultyChangedException(prevBlock, candidateBlock);
            }
        }
    }

    private boolean needToReduceTheDifficulty(long lastBlocksMPTinSeconds) {
        return lastBlocksMPTinSeconds >= TARGET_PRODUCTION_TIME_IN_SECONDS;
    }

    private BigInteger calculateReducedDifficulty(HeaderReadOnly prevBlock) {
        BigInteger pow = prevBlock.getDifficultyTargetAsInteger();
        // Divide difficulty target by 1/4 (which reduces the difficulty by 20%)
        pow = pow.add(pow.shiftRight(2));

        if (pow.compareTo(maxTarget) > 0) {
            pow = maxTarget;
        }
        return pow;
    }

    private void throwUnexpectedReducedDifficultyException(ChainInfo storedPrev, ChainInfo candidateBlock, BigInteger nPow) throws BlockChainRuleFailureException {
        throw new BlockChainRuleFailureException("Unexpected change in difficulty [6 blocks >12 hours] at height " + storedPrev.getHeight() +
                ": " + Long.toHexString(candidateBlock.getHeader().getDifficultyTarget()) + " vs " +
                Utils.encodeCompactBits(nPow));
    }

    private void throwUnexpectedDifficultyChangedException(ChainInfo prevBlock, ChainInfo candidateBlock) throws BlockChainRuleFailureException {
        throw new BlockChainRuleFailureException("Unexpected change in difficulty at height " + prevBlock.getHeight() +
                ": " + Long.toHexString(candidateBlock.getHeader().getDifficultyTarget()) + " vs " +
                Long.toHexString(prevBlock.getHeader().getDifficultyTarget()));
    }

    //TODO perhaps clean this up to make it more intuitive
    public long getMedianTimestampOfRecentBlocks(ChainInfo block) {
        long[] timestamps = new long[11];
        int unused = 9;

        for(timestamps[10] = block.getHeader().getTime();
            unused >= 0 && (block = blockChainStore.getBlockChainInfo(block.getHeader().getPrevBlockHash()).get()) != null;
            timestamps[unused--] = block.getHeader().getTime())
        {
        }

        Arrays.sort(timestamps, unused + 1, 11);
        return timestamps[unused + (11 - unused) / 2];
    }

}
