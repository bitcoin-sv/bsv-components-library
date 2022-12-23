package io.bitcoinsv.jcl.store.levelDB.blockChainStore;


import io.bitcoinsv.jcl.store.blockChainStore.events.BlockChainStoreStreamer;
import io.bitcoinsv.jcl.store.blockChainStore.validation.exception.BlockChainRuleFailureException;
import io.bitcoinsv.jcl.store.blockChainStore.validation.rules.BlockChainRule;
import io.bitcoinsv.jcl.store.blockStore.metadata.Metadata;
import io.bitcoinsv.jcl.store.keyValue.blockChainStore.BlockChainStoreKeyValue;
import io.bitcoinsv.jcl.store.levelDB.blockStore.BlockStoreLevelDB;
import io.bitcoinsv.jcl.tools.thread.ThreadUtils;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.extended.ChainInfo;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * BlockChainStore Implementation based on LevelDB Database.
 * It extends the BlockStoreLevelDB class, so it already contains all the logic in the BlockStore interface and the
 * connection to the LevelDB.

 */
public class BlockChainStoreLevelDB extends BlockStoreLevelDB implements BlockChainStoreKeyValue<Map.Entry<byte[], byte[]>, Object> {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(BlockChainStoreLevelDB.class);
    // Configuration:
    private BlockChainStoreLevelDBConfig config;

    // State publish configuration:
    private final Duration statePublishFrequency;
    private ScheduledExecutorService scheduledExecutorService;

    // Events Streamer:
    private final BlockChainStoreStreamer blockChainStoreStreamer;

    public BlockChainStoreLevelDB(@Nonnull BlockChainStoreLevelDBConfig config,
                                  boolean triggerBlockEvents,
                                  boolean triggerTxEvents,
                                  Class<? extends Metadata> blockMetadataClass,
                                  Class<? extends Metadata> txMetadataClass,
                                  Duration statePublishFrequency) {

        super(config, triggerBlockEvents, triggerTxEvents, blockMetadataClass, txMetadataClass);
        this.config = config;

        this.statePublishFrequency = statePublishFrequency;

        blockChainStoreStreamer = new BlockChainStoreStreamer(super.eventBus);
    }



    @Override public byte[] fullKeyForBlockNext(String blockHash)       { return fullKey(this.fullKeyForBlocks(), keyForBlockNext(blockHash));}
    @Override public byte[] fullKeyForBlockChainInfo(String blockHash)  { return fullKey(this.fullKeyForBlocks(), keyForBlockChainInfo(blockHash));}
    @Override public byte[] fullKeyForChainTips()                       { return fullKey(this.fullKeyForBlocks(), keyForChainTips());}
    @Override public byte[] fullKeyForChainPathsLast()                  { return fullKey(this.fullKeyForBlocks(), keyForChainPathsLast());}
    @Override public byte[] fullKeyForChainPath(int branchId)           { return fullKey(this.fullKeyForBlocks(), keyForChainPath(branchId));}
    @Override public byte[] fullKeyForBlockHashesByHeight(int height)     {return fullKey(this.fullKeyForBlocks(), keyForBlocksByHeight(height));}


    @Override public BlockChainStoreStreamer EVENTS()                   { return blockChainStoreStreamer;}

    @Override
    public void validateBlockChainInfo(ChainInfo block) throws BlockChainRuleFailureException {
        for(BlockChainRule rule : config.getBlockChainRules()){
            if(rule.applies(block)){
                rule.checkRule(block, this); //TODO? if we're passing this then we shouldn't we make it read only, or separate storage from block
            }
        }
    }

    @Override
    public void start() {
        super.start();

        // If the DB is empty, we initialize it with the Genesis block:
        if (getNumBlocks() == 0) {
            Object tr = createTransaction();
            executeInTransaction(tr, () -> _initGenesisBlock(tr, config.getGenesisBlock()));
        }

        // We set up the executor Service in case we need to launch processes in a different Thread, which is the case
        // when we publish state, do automatic Fork prunning or automatic orphan prunning
        if (this.statePublishFrequency != null
                || config.isForkPruningAutomaticEnabled()
                || config.isForkPruningAlertEnabled()
                || config.isOrphanPruningAutomaticEnabled()) {
            this.scheduledExecutorService = ThreadUtils.getScheduledExecutorService("BlockChainStore-LevelDB-thread");
        }

        // If enabled, we start the job to publish the DB State:
        if (statePublishFrequency != null)
            this.scheduledExecutorService.scheduleAtFixedRate(this::_publishState,
                    statePublishFrequency.toMillis(),
                    statePublishFrequency.toMillis(),
                    TimeUnit.MILLISECONDS);

        // If enabled, we start the job to do the automatic FORK Prunning:
        if (config.isForkPruningAutomaticEnabled() || config.isForkPruningAlertEnabled())
            this.scheduledExecutorService.scheduleAtFixedRate(this::_automaticForkPrunning,
                    config.getForkPruningFrequency().toMillis(),
                    config.getForkPruningFrequency().toMillis(),
                    TimeUnit.MILLISECONDS);

        // If enabled, we start the job to do the automatic ORPHAN Prunning:
        if (config.isOrphanPruningAutomaticEnabled())
            this.scheduledExecutorService.scheduleAtFixedRate(this::_automaticOrphanPrunning,
                    config.getOrphanPruningFrequency().toMillis(),
                    config.getForkPruningFrequency().toMillis(),
                    TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        // If enabled, we stop the job to publish the state
        if (statePublishFrequency != null || config.isForkPruningAutomaticEnabled() || config.isOrphanPruningAutomaticEnabled()) {
            try {
                this.scheduledExecutorService.awaitTermination(1000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ie) {}
            this.scheduledExecutorService.shutdownNow();
        }
        super.stop();
    }

    @Override
    public void clear() {
        // We clear the DB the usual way:
        super.clear();
        // and we restore the Genesis block:
        Object tr = createTransaction();
        executeInTransaction(tr, () -> _initGenesisBlock(tr, config.getGenesisBlock()));
    }

    public BlockChainStoreLevelDBConfig getConfig() {
        return this.config;
    }

    public static BlockChainStoreLevelDBBuilder chainStoreBuilder() {
        return new BlockChainStoreLevelDBBuilder();
    }

    /**
     * Builder
     */
    public static class BlockChainStoreLevelDBBuilder {
        private BlockChainStoreLevelDBConfig config;
        private boolean triggerBlockEvents;
        private boolean triggerTxEvents;
        private Class<? extends Metadata> blockMetadataClass;
        private Class<? extends Metadata> txMetadataClass;
        private Duration statePublishFrequency;

        BlockChainStoreLevelDBBuilder() {
        }

        public BlockChainStoreLevelDB.BlockChainStoreLevelDBBuilder config(BlockChainStoreLevelDBConfig config) {
            this.config = config;
            return this;
        }

        public BlockChainStoreLevelDB.BlockChainStoreLevelDBBuilder triggerBlockEvents(boolean triggerBlockEvents) {
            this.triggerBlockEvents = triggerBlockEvents;
            return this;
        }

        public BlockChainStoreLevelDB.BlockChainStoreLevelDBBuilder triggerTxEvents(boolean triggerTxEvents) {
            this.triggerTxEvents = triggerTxEvents;
            return this;
        }

        public BlockChainStoreLevelDB.BlockChainStoreLevelDBBuilder blockMetadataClass(Class<? extends Metadata> blockMetadataClass) {
            this.blockMetadataClass = blockMetadataClass;
            return this;
        }

        public BlockChainStoreLevelDB.BlockChainStoreLevelDBBuilder txMetadataClass(Class<? extends Metadata> txMetadataClass) {
            this.txMetadataClass = txMetadataClass;
            return this;
        }

        public BlockChainStoreLevelDB.BlockChainStoreLevelDBBuilder statePublishFrequency(Duration statePublishFrequency) {
            this.statePublishFrequency = statePublishFrequency;
            return this;
        }

        public BlockChainStoreLevelDB build() {
            return new BlockChainStoreLevelDB(config,
                    triggerBlockEvents,
                    triggerTxEvents,
                    blockMetadataClass,
                    txMetadataClass,
                    statePublishFrequency);
        }
    }
}