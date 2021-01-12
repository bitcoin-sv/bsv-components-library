package com.nchain.jcl.store.levelDB.blockChainStore;

import com.nchain.jcl.base.domain.api.extended.ChainInfo;
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper;
import com.nchain.jcl.base.tools.thread.ThreadUtils;
import com.nchain.jcl.store.blockChainStore.events.BlockChainStoreStreamer;
import com.nchain.jcl.store.keyValue.blockChainStore.BlockChainStoreKeyValue;
import com.nchain.jcl.store.levelDB.blockStore.BlockStoreLevelDB;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * BlockChainStore Implementation based on LevelDB Database.
 * It extends the BlockStoreLevelDB class, so it already contains all the logic in the BlockStore interface and the
 * connection to the LevelDB.

 */
@Slf4j
public class BlockChainStoreLevelDB extends BlockStoreLevelDB implements BlockChainStoreKeyValue<Map.Entry<byte[], byte[]>, Object> {

    // Configuration:
    @Getter private BlockChainStoreLevelDBConfig config;

    // State publish configuration:
    private final Duration statePublishFrequency;
    private ScheduledExecutorService scheduledExecutorService;

    // Automatic prunning configuration:
    public static Duration PRUNNING_FREQUENCY_DEFAULT = Duration.ofMinutes(180);
    public static int      PRUNNING_HEIGHT_DIFF_DEFAULT = 2;

    private final Boolean   enableAutomaticPrunning;
    private final Duration  prunningFrequency;
    private final int       prunningHeightDifference;
    private final boolean   prunningTxs;

    // Events Streamer:
    private final BlockChainStoreStreamer blockChainStoreStreamer;

    @Builder(builderMethodName = "chainStoreBuilder")
    public BlockChainStoreLevelDB(@NonNull BlockChainStoreLevelDBConfig config,
                                  boolean triggerBlockEvents,
                                  boolean triggerTxEvents,
                                  Duration statePublishFrequency,
                                  Boolean enableAutomaticPrunning,
                                  Duration prunningFrequency,
                                  Integer prunningHeightDifference,
                                  boolean prunningTxs) {

        super(config, triggerBlockEvents, triggerTxEvents);
        this.config = config;

        this.enableAutomaticPrunning = (enableAutomaticPrunning != null) ? enableAutomaticPrunning : false;
        this.statePublishFrequency = statePublishFrequency;
        this.prunningFrequency = (prunningFrequency != null) ? prunningFrequency : PRUNNING_FREQUENCY_DEFAULT;
        this.prunningHeightDifference = (prunningHeightDifference != null) ? prunningHeightDifference : PRUNNING_HEIGHT_DIFF_DEFAULT;
        this.prunningTxs = prunningTxs;

        // either publishing the state or the automatic prunning need an Scheduler:
        if (this.statePublishFrequency != null || this.enableAutomaticPrunning) {
            this.scheduledExecutorService = ThreadUtils.getScheduledExecutorService("BlockChainStore-LevelDB-state");
        }

        blockChainStoreStreamer = new BlockChainStoreStreamer(super.eventBus);
    }

    @Override public byte[] fullKeyForBlockNext(String blockHash)       { return fullKey(this.fullKeyForBlocks(), keyForBlockNext(blockHash));}
    @Override public byte[] fullKeyForBlockChainInfo(String blockHash)  { return fullKey(this.fullKeyForBlocks(), keyForBlockChainInfo(blockHash));}
    @Override public byte[] fullKeyForChainTips()                       { return fullKey(this.fullKeyForBlocks(), keyForChainTips());}

    @Override public BlockChainStoreStreamer EVENTS()                   { return blockChainStoreStreamer;}

    @Override
    public void start() {
        super.start();

        // If the DB is empty, we initialize it with the Genesis block:
        if (getNumBlocks() == 0) {
            Object tr = createTransaction();
            executeInTransaction(tr, () -> _initGenesisBlock(tr, config.getGenesisBlock()));
        }

        // If enabled, we start the job to publish the DB State:
        if (statePublishFrequency != null)
            this.scheduledExecutorService.scheduleAtFixedRate(this::_publishState,
                    statePublishFrequency.toMillis(),
                    statePublishFrequency.toMillis(),
                    TimeUnit.MILLISECONDS);

        // If enabled, we start the job to do the automatic Prunning:
        if (enableAutomaticPrunning)
            this.scheduledExecutorService.scheduleAtFixedRate(this::_automaticPrune,
                    prunningFrequency.toMillis(),
                    prunningFrequency.toMillis(),
                    TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        // If enabled, we stop the job to publish the state
        if (statePublishFrequency != null || enableAutomaticPrunning)
             this.scheduledExecutorService.shutdownNow();
        super.stop();
    }

    // It performs an Automatic Prunning: It search for Fork Chains, and if they meet the criteria to be pruned, it
    // prunes them. Criteria to prune a Chain:
    // - it is NOT the longest Chain
    // - its Height is >= than "prunningHeightDifference"
    // - the difference of age between the block of the tip and the on in the tip of the longest chain is
    //   longer than "prunningAgeDifference"

    private synchronized void _automaticPrune() {
        // We only prune if there is more than one chain:
        List<Sha256Wrapper> tipsChain = getTipsChains();
        if (tipsChain != null && (tipsChain.size() > 0)) {
            ChainInfo longestChain = getLongestChain().get();
            List<Sha256Wrapper> tipsToPrune = getState().getTipsChains().stream()
                    .filter(c -> (!c.equals(longestChain))
                            && ((longestChain.getHeight() - c.getHeight()) >= prunningHeightDifference))
                    .map(c -> c.getHeader().getHash())
                    .collect(Collectors.toList());
            tipsToPrune.forEach(c -> prune(c, prunningTxs));
        }
    }

}
