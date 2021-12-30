package io.bitcoinsv.jcl.store.blockChainStore.validation.rules;

import io.bitcoinsv.jcl.store.blockChainStore.BlockChainStore;
import io.bitcoinsv.jcl.store.blockChainStore.validation.exception.BlockChainRuleFailureException;
import io.bitcoinsv.jcl.tools.util.PowUtil;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.HeaderReadOnly;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.extended.ChainInfo;
import io.bitcoinsv.bitcoinjsv.core.Utils;


import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class EmergencyDifficultyAdjustmentRule extends AbstractBlockChainRule {

    private static final long TARGET_PRODUCTION_TIME_IN_SECONDS = 12 * 60 * 60; // 12 hours
    private static final int REFERENCE_OF_BLOCKS_PRODUCED_SIZE = 6;
    private static final int REFERENCE_BEFORE_BLOCK_DISTANCE = 10;

    private final BigInteger maxTarget;

    public EmergencyDifficultyAdjustmentRule(Predicate<ChainInfo> predicate, BigInteger maxTarget) {
        super(predicate);
        this.maxTarget = maxTarget;
    }

    @Override
    public void checkRule(ChainInfo candidateBlock, BlockChainStore blockChainStore) throws BlockChainRuleFailureException {
        ChainInfo prevBlockChainInfo = blockChainStore.getBlockChainInfo(candidateBlock.getHeader().getPrevBlockHash()).orElseThrow(() -> new BlockChainRuleFailureException("Not enough blocks to check emergency difficulty transition rule."));

        long lastBlocksMPTinSeconds = getMedianProducingTimeInSeconds(prevBlockChainInfo, blockChainStore);

        checkEDARules(prevBlockChainInfo, candidateBlock, lastBlocksMPTinSeconds);
    }

    private long getMedianProducingTimeInSeconds(ChainInfo storedPrev, BlockChainStore blockChainStore) throws BlockChainRuleFailureException {
        // TODO: This needs reviewing, now that we support Forks (multiple ChainInfo at a certain height)
        // NOTE: We assume the are not fork!! If the list of ChainInfos for a certain Height returns more than one Block,
        // we just take the first one:
        List<ChainInfo> blocksAtHeight = blockChainStore.getBlock(storedPrev.getHeight() - REFERENCE_OF_BLOCKS_PRODUCED_SIZE);
        Optional<ChainInfo> firstBlockAtHeight = Optional.of((blocksAtHeight != null && !blocksAtHeight.isEmpty()) ? blocksAtHeight.get(0) : null);
        ChainInfo referenceBlockChainInfo = firstBlockAtHeight.orElseThrow(() -> new BlockChainRuleFailureException("Not enough blocks to check emergency difficulty adjustment rule."));

        if(referenceBlockChainInfo.getHeight() - REFERENCE_BEFORE_BLOCK_DISTANCE < 0 ) {
            throw new BlockChainRuleFailureException("Not enough blocks to check emergency difficulty adjustment rule.");
        }

        return getMedianTimestampOfRecentBlocks(storedPrev, blockChainStore) -
                getMedianTimestampOfRecentBlocks(referenceBlockChainInfo, blockChainStore);
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
    public long getMedianTimestampOfRecentBlocks(ChainInfo block, BlockChainStore blockChainStore) {
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
