package com.nchain.jcl.store.foundationDB.blockChainStore;

import com.apple.foundationdb.KeyValue;
import com.apple.foundationdb.Transaction;
import com.nchain.jcl.base.tools.thread.ThreadUtils;
import com.nchain.jcl.store.blockChainStore.events.BlockChainStoreStreamer;
import com.nchain.jcl.store.foundationDB.blockStore.BlockStoreFDB;
import com.nchain.jcl.store.keyValue.blockChainStore.BlockChainStoreKeyValue;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * BlockChainStore Implementation based on FoundationDB Database.
 * It extends the BlockStoreLevelDB class, so it already contains all the logic in the BlockStore interface and the
 * connection to the LevelDB.
 */
public class BlockChainStoreFDB extends BlockStoreFDB implements BlockChainStoreKeyValue<KeyValue, Transaction> {

    // Configuration
    @Getter private BlockChainStoreFDBConfig config;

    // State publish configuration:
    private final Duration statePublishFrequency;
    private ScheduledExecutorService scheduledExecutorService;

    // Automatic prunning configuration:
    public static Duration FORK_PRUNNING_FREQUENCY_DEFAULT      = Duration.ofMinutes(180);
    public static Duration ORPHAN_PRUNNING_FREQUENCY_DEFAULT    = Duration.ofMinutes(60);

    private final Boolean  enableAutomaticForkPrunning;
    private final Duration forkPrunningFrequency;
    private final Boolean  enableAutomaticOrphanPrunning;
    private final Duration orphanPrunningFrequency;

    // Events Streamer:
    private final BlockChainStoreStreamer blockChainStoreStreamer;

    @Builder(builderMethodName = "chainStoreBuilder")
    public BlockChainStoreFDB(@NonNull BlockChainStoreFDBConfig config,
                              boolean triggerBlockEvents,
                              boolean triggerTxEvents, Duration statePublishFrequency,
                              Boolean enableAutomaticForkPrunning,
                              Duration forkPrunningFrequency,
                              Boolean enableAutomaticOrphanPrunning,
                              Duration orphanPrunningFrequency) {
        super(config, triggerBlockEvents, triggerTxEvents);
        this.config = config;

        this.enableAutomaticForkPrunning = (enableAutomaticForkPrunning != null) ? enableAutomaticForkPrunning : false;
        this.statePublishFrequency = statePublishFrequency;
        this.forkPrunningFrequency = (forkPrunningFrequency != null) ? forkPrunningFrequency : FORK_PRUNNING_FREQUENCY_DEFAULT;
        this.enableAutomaticOrphanPrunning = (enableAutomaticOrphanPrunning != null) ? enableAutomaticOrphanPrunning : false;
        this.orphanPrunningFrequency = (orphanPrunningFrequency != null) ? orphanPrunningFrequency: ORPHAN_PRUNNING_FREQUENCY_DEFAULT;

        // We set up the executor Service in case we need to launch processes in a different Thread, which is the case
        // when we publish state, do automatic Fork prunning or automatic orphan prunning
        if (this.statePublishFrequency != null || this.enableAutomaticForkPrunning || this.enableAutomaticOrphanPrunning) {
            this.scheduledExecutorService = ThreadUtils.getScheduledExecutorService("BlockChainStore-LevelDB-thread");
        }

        blockChainStoreStreamer = new BlockChainStoreStreamer(super.eventBus);
    }

    @Override public byte[] fullKeyForBlockNext(String blockHash)       { return fullKey(blocksDir, keyForBlockNext(blockHash));}
    @Override public byte[] fullKeyForBlockChainInfo(String blockHash)  { return fullKey(blocksDir, keyForBlockChainInfo(blockHash));}
    @Override public byte[] fullKeyForChainTips()                       { return fullKey(blocksDir, keyForChainTips());}
    @Override public byte[] fullKeyForChainPathsLast()                { return fullKey(blocksDir, keyForChainPathsLast());}
    @Override public byte[] fullKeyForChainPath(int branchId)         { return fullKey(blocksDir, keyForChainPath(branchId));}

    @Override public BlockChainStoreStreamer EVENTS()                   { return blockChainStoreStreamer;}

    @Override
    public void start() {
        super.start();

        // If the DB is empty, we initialize it with the Genesis block:
        if (getNumBlocks() == 0) {
            Transaction tr = createTransaction();
            executeInTransaction(tr, () -> _initGenesisBlock(tr, config.getGenesisBlock()));
        }

        // If enabled, we start the job to publish the DB State:
        if (statePublishFrequency != null)
            this.scheduledExecutorService.scheduleAtFixedRate(this::_publishState,
                    statePublishFrequency.toMillis(),
                    statePublishFrequency.toMillis(),
                    TimeUnit.MILLISECONDS);

        // If enabled, we start the job to do the automatic Prunning:
        if (enableAutomaticForkPrunning)
            this.scheduledExecutorService.scheduleAtFixedRate(this::_automaticForkPrunning,
                    forkPrunningFrequency.toMillis(),
                    forkPrunningFrequency.toMillis(),
                    TimeUnit.MILLISECONDS);

        // If enabled, we start the job to do the automatic ORPHAN Prunning:
        if (enableAutomaticOrphanPrunning)
            this.scheduledExecutorService.scheduleAtFixedRate(this::_automaticOrphanPrunning,
                    orphanPrunningFrequency.toMillis(),
                    orphanPrunningFrequency.toMillis(),
                    TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        // If enabled, we stop the job to publish the state
        if (statePublishFrequency != null || enableAutomaticForkPrunning || enableAutomaticOrphanPrunning)
            this.scheduledExecutorService.shutdownNow();
        super.stop();
    }

    @Override
    public void clear() {
        // We clear the DB the usual way:
        super.clear();
        // and we restore the Genesis block:
        db.run(tr -> {
            _initGenesisBlock(tr, config.getGenesisBlock());
            return null;
        });
    }
}
