package com.nchain.jcl.store.levelDB.blockChainStore;

import com.nchain.jcl.base.domain.api.base.BlockHeader;
import com.nchain.jcl.base.domain.api.extended.ChainInfo;
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper;
import com.nchain.jcl.base.tools.thread.ThreadUtils;
import com.nchain.jcl.store.blockChainStore.BlockChainStore;
import com.nchain.jcl.store.blockChainStore.BlockChainStoreState;
import com.nchain.jcl.store.blockChainStore.events.BlockChainStoreStreamer;
import com.nchain.jcl.store.blockChainStore.events.ChainForkEvent;
import com.nchain.jcl.store.blockChainStore.events.ChainPruneEvent;
import com.nchain.jcl.store.blockChainStore.events.ChainStateEvent;
import com.nchain.jcl.store.levelDB.blockStore.BlockStoreLevelDB;
import com.nchain.jcl.store.levelDB.common.HashesList;
import static com.nchain.jcl.store.levelDB.blockChainStore.BlockChainKeyValueUtils.*;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;


import java.io.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Implementation of the BlockChainStore component using LevelDB as the Tech Stack.
 *
 * LevelDB is a No-SQL that provides Key-Value storage. This implementation extends the BlockStore one, so it
 * already provides all the basic operation to manipulate Blocks and Transactions.
 * This implementation provides additional operations to manage information about the different Chains we might have
 * in the db, and the relationships betwwen those Blocks and those Chains.
 *
 */
@Slf4j
public class BlockChainStoreLevelDB extends BlockStoreLevelDB implements BlockChainStore {

    // Configuration:
    private final BlockChainStoreLevelDBConfig config;

    // State publish configuration:
    private final Duration statePublishFrequency;
    private ScheduledExecutorService scheduledExecutorService;

    // Automatic prunning configuration:
    private static Duration PRUNNING_FREQUENCY_DEFAULT = Duration.ofMinutes(180);
    private static int      PRUNNING_HEIGHT_DIFF_DEFAULT = 2;

    private final Boolean   enableAutomaticPrunning;
    private final Duration  prunningFrequency;
    private final int       prunningHeightDifference;
    private final boolean   prunningTxs;

    // Events Streamer:
    private final BlockChainStoreStreamer blockChainStoreStreamer;

    // We keep a static reference of the List of Tips of all teh Chains stored in the DB. Some methods
    // will change this structure, so it's more efficient to keep a copy here. The list is saved in the Db when the
    // service is stopped, but just in case, and in order not to loose data if the service stops unexpectedly, we keep
    // track of the times the structure is updated, and when a threshold is reached, we save it.
    private static HashesList tipsChains;

    /** Constructor */
    @Builder(builderMethodName = "chainBuilder")
    public BlockChainStoreLevelDB(BlockChainStoreLevelDBConfig config,
                                  Boolean triggerBlockEvents,
                                  Boolean triggerTxEvents,
                                  Duration statePublishFrequency,
                                  Boolean enableAutomaticPrunning,
                                  Duration prunningFrequency,
                                  Integer prunningHeightDifference,
                                  boolean prunningTxs) throws RuntimeException {
        super(config, triggerBlockEvents, triggerTxEvents);
        this.config = config;

        this.enableAutomaticPrunning = (enableAutomaticPrunning != null) ? enableAutomaticPrunning : false;
        this.statePublishFrequency = statePublishFrequency;
        this.prunningFrequency = (prunningFrequency != null) ? prunningFrequency : PRUNNING_FREQUENCY_DEFAULT;
        this.prunningHeightDifference = (prunningHeightDifference != null) ? prunningHeightDifference : PRUNNING_HEIGHT_DIFF_DEFAULT;
        this.prunningTxs = prunningTxs;

        blockChainStoreStreamer = new BlockChainStoreStreamer(eventBus);

        // either publishing the state or the automatic prunning need an Scheduler:
        if (this.statePublishFrequency != null || this.enableAutomaticPrunning) {
            this.scheduledExecutorService = ThreadUtils.getScheduledExecutorService("BlockChainStore-LevelDB-state");
        }
    }


    // Basic Operations:
    // Internal/private functions.

    // Returns the relative chain info for a Block, or null if hte block is not store or not connected to a Chain
    private BlockChainInfo _getBlockChainInfo(String blockHashHex) {
        byte[] value = levelDBStore.get(bytes(getKeyForBlockChain(blockHashHex)));
        return bytesToChainInfo(value);
    }

    // It saves teh relative Chain info for a Block
    private BlockChainInfo _saveBlockChainInfo(BlockHeader block, BlockChainInfo parentBlockChainInfo) {

        // We calculate the Height and total chain size:
        int resultHeight = (parentBlockChainInfo != null)
                ? parentBlockChainInfo.getHeight() + 1
                : 0;
        long resultChainSize = (parentBlockChainInfo != null)
                ? block.getSizeInBytes() + parentBlockChainInfo.getTotalChainSize()
                : block.getSizeInBytes();

        // We build the object asn save it:
        BlockChainInfo blockChainInfo = BlockChainInfo.builder()
                .blockHash(block.getHash().toString())
                .chainWork(block.getWork())
                .height(resultHeight)
                .totalChainSize(resultChainSize)
                .build();
        levelDBStore.put(bytes(getKeyForBlockChain(block.getHash().toString())), bytes(blockChainInfo));
        return blockChainInfo;
    }

    // Indicates if the block is connected to a chain (so there is a ChainInfo stored for this block)
    private boolean _isBlockConnectedToAChain(String blockHashHex) {
        return levelDBStore.get(bytes(getKeyForBlockChain(blockHashHex))) != null;
    }

    // Returns the list of Blocks built on top of the given one:
    // If the Block has NO Childrenm it returtns an EMPTY List
    private HashesList _getNextBlocks(String blockHashHex) {
        return bytesToHashesList(levelDBStore.get(bytes(getKeyForBlockNext(blockHashHex))));
    }

    // Adds one Block as a CHILD of this one (one built on top of this one)
    private void _addChildToBlock(String parentBlockHash, String childBlockHash) {
        HashesList childs = _getNextBlocks(parentBlockHash);
        if (childs == null) childs = HashesList.builder().build();
        if (!(childs.getHashes().contains(childBlockHash)))
            childs.getHashes().add(childBlockHash);
        levelDBStore.put(bytes(getKeyForBlockNext(parentBlockHash)), bytes(childs));
    }

    // Removes a Block from the lis tof CHILDREN of this one
    private void _removeChildFromBlock(String parentBlockHash, String childBlockHash) {
        HashesList childs = _getNextBlocks(parentBlockHash);
        if (childs != null) {
            childs.getHashes().remove(childBlockHash);
            levelDBStore.put(bytes(getKeyForBlockNext(parentBlockHash)), bytes(childs));
        }
    }

    // Updates the List of Chain Tips, adding one and/or removing one from the List
    private void _updateTipsChain(String blockHashToAdd, String blockHashToRemove) {
        if (blockHashToAdd != null)     tipsChains.getHashes().add(blockHashToAdd);
        if (blockHashToRemove != null)  tipsChains.getHashes().remove(blockHashToRemove);
        _saveChainsTips(tipsChains);
    }

    // It Connects "blockHeader" with the Chain identified by "parentBlockChainInfo".
    //  - "parentBlockChainInfo" is the ChainInfo of the PARENT of "blockHeader"
    // After connecting "blockHeader" (saving its relative chainInfo), we move traverse through all its children
    // and their children and so on, connecting them all as we go

    private void _connectBlock(BlockHeader blockHeader, BlockChainInfo parentBlockChainInfo) {
        // We store the BlockChain Info of this new Block:
        BlockChainInfo blockChainInfo = _saveBlockChainInfo(blockHeader, parentBlockChainInfo);

        // If the Parent is part of the TIPS of the Chains, then it must be removed from it:
        if (parentBlockChainInfo != null && tipsChains.getHashes().contains(parentBlockChainInfo.getBlockHash())) {
            _updateTipsChain(null, parentBlockChainInfo.getBlockHash()); // we don't add, just remove
        }

        // Now we look into the CHILDREN (Blocks built on top of this Block), and we connect them as well...
        // If the Block has NOT Children, then this is the Last Block that can be connected, so we add it to the Tips
        HashesList children = _getNextBlocks(blockHeader.getHash().toString());
        if (children != null && children.getHashes().size() > 0) {
            for (String childHashHex : children.getHashes()) {
                Optional<BlockHeader> childBlock = getBlock(Sha256Wrapper.wrap(childHashHex));
                if (childBlock.isPresent()) _connectBlock(childBlock.get(), blockChainInfo);
            }
        } else {
            _updateTipsChain(blockHeader.getHash().toString(), null); // We add this block to the Tips
        }
    }

    // We disconnect this block from the Chain
    private void _disconnectBlock(String blockHashHex) {
        // If this block is already connected we remove the Chain Info:
        BlockChainInfo blockChainInfo = _getBlockChainInfo(blockHashHex);
        if (blockChainInfo != null) {
            // We remove the ChainInfo for this Node:
            levelDBStore.delete(bytes(getKeyForBlockChain(blockHashHex)));

            // We update the tip of the chain (this block is not the tip anymore, if its already)
            _updateTipsChain(null, blockHashHex);

            // We remove all the Chain Info from its Children...
            HashesList children = _getNextBlocks(blockHashHex);
            if (children != null)
                children.getHashes().forEach(h -> _disconnectBlock(h));
        }
    }

    // We sve the ChainTips in the DB
    private void _saveChainsTips(HashesList chainstips) {
        levelDBStore.put(bytes(PREFFIX_KEY_CHAINS_TIPS), bytes(chainstips));
    }

    // We init the Db with the Genesis Block, and we also CONNECT it, so its part of the Chain
    private void _initGenesisBlock(BlockHeader genesisBlock) {
        _saveBlock(genesisBlock);
        _connectBlock(genesisBlock, null); // No parent for this block
    }

    // It publishes an Event containing the DB State
    private void _publishState() {
        ChainStateEvent event = ChainStateEvent.builder().state(getState()).build();
        eventBus.publish(event);
    }

    // It performs an Automatic Prunning: It search for Fork Chains, and if they meet the criteria to be pruned, it
    // prunes them. Criteria to prune a Chain:
    // - it is NOT the longest Chain
    // - its Height is >= than "prunningHeightDifference"
    // - the difference of age between the block of the tip and the on in the tip of the longest chain is
    //   longer than "prunningAgeDifference"

    private synchronized void _automaticPrune() {
        // We only prune if there is more than one chain:
        if (tipsChains.getHashes().size() > 1) {
            ChainInfo longestChain = getLongestChain().get();
            List<Sha256Wrapper> tipsToPrune = getState().getTipsChains().stream()
                    .filter(c -> (!c.equals(longestChain))
                            && ((longestChain.getHeight() - c.getHeight()) >= prunningHeightDifference))
                    .map(c -> c.getHeader().getHash())
                    .collect(Collectors.toList());
            tipsToPrune.forEach(c -> prune(c, prunningTxs));
        }
    }

    @Override
    public void start() {

        super.start();
        // We load the current tips of the Chains from the DB...
        tipsChains = bytesToHashesList(levelDBStore.get(bytes(PREFFIX_KEY_CHAINS_TIPS)));
        if (tipsChains == null) tipsChains = HashesList.builder().build();

        // If the DB is empty, we initialize it with the Genesis block:
        if (getNumBlocks() == 0) {
            _initGenesisBlock(config.getGenesisBlock());
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
        super.stop();
        //_saveChainsTips(tipsChains);
        // If enabled, we stop the job to publish the state
        if (statePublishFrequency != null || enableAutomaticPrunning) this.scheduledExecutorService.shutdownNow();
    }

    /**
     * Apart from Saving the New Block in the DB, now we try to "connect" this Block to the Chain of Blocks:
     * It search of rht Parent Block, and if found, then it creates 2 new Keys:
     * - a "b_next" key, so you can navigate fromt he parent block to this one easily
     * - a "b_chain" key, to store the Chain info for this Block.
     */
    @Override
    public void _saveBlock(BlockHeader blockHeader) {
        String parentHashHex = blockHeader.getPrevBlockHash().toString();

        // we save te Block and its relation with its parent:
        super._saveBlock(blockHeader);
        _addChildToBlock(parentHashHex, blockHeader.getHash().toString());

        // If the Parent exists and it's also Connected, we connect this one too:
        BlockChainInfo parentChainInfo =  _getBlockChainInfo(parentHashHex);
        if (parentChainInfo != null) {
            _connectBlock(blockHeader, parentChainInfo);

            // If this is a fork, we trigger a Fork Event:
            List<Sha256Wrapper> parentChilds = getNextBlocks(blockHeader.getPrevBlockHash());
            if (parentChilds != null && parentChilds.size() > 1) {
                ChainForkEvent event = ChainForkEvent.builder()
                        .blockForkHash(blockHeader.getHash())
                        .parentForkHash(blockHeader.getPrevBlockHash())
                        .build();
                eventBus.publish(event);
            }
        }
    }

    @Override
    public void _removeBlock(String blockHash) {
        // Basic check if the block exists:
        Optional<BlockHeader> block = getBlock(Sha256Wrapper.wrap(blockHash));
        if (!block.isPresent()) return;

        // We remove the relationship between this block and its parent:
        _removeChildFromBlock(block.get().getPrevBlockHash().toString(), blockHash);
        _disconnectBlock(blockHash);

        // We update the tip of the chain (the parent is now the tip of the chain)
        // If the parent has already other children then we do NOT do it, since that would mean that that parent
        // is already part of other chain
        List<Sha256Wrapper> parentChildren = getNextBlocks(block.get().getPrevBlockHash());
        if (parentChildren == null || parentChildren.size() == 0)
            _updateTipsChain(block.get().getPrevBlockHash().toString(), null);

        // we remove the Block the usual way:
        super._removeBlock(blockHash);
    }

    @Override
    public synchronized void saveBlock(BlockHeader blockHeader) {
        super.saveBlock(blockHeader);
    }

    @Override
    public synchronized void saveBlocks(List<BlockHeader> blockHeaders) {
        super.saveBlocks(blockHeaders);
    }

    @Override
    public synchronized void removeBlock(Sha256Wrapper blockHash) {
        super.removeBlock(blockHash);
    }

    @Override
    public Optional<Sha256Wrapper> getPrevBlock(Sha256Wrapper blockHash) {
        Optional<Sha256Wrapper> result = getBlock(blockHash).map(b -> b.getPrevBlockHash());
        return result;
    }

    @Override
    public List<Sha256Wrapper> getNextBlocks(Sha256Wrapper blockHash) {
        HashesList children = _getNextBlocks(blockHash.toString());
        List<Sha256Wrapper> result = (children != null)
                ? children.getHashes().stream().map(h -> Sha256Wrapper.wrap(h)).collect(Collectors.toList())
                : new ArrayList<>();
        return result;
    }

    @Override
    public Optional<ChainInfo> getBlockChainInfo(Sha256Wrapper blockHash) {
        Optional<BlockHeader> blockHeader = getBlock(blockHash);
        if (blockHeader.isEmpty()) return Optional.empty();

        BlockChainInfo blockChainInfo = _getBlockChainInfo(blockHash.toString());
        if (blockChainInfo == null) return Optional.empty();

        Optional<ChainInfo> result =  Optional.of(ChainInfo.builder()
                            .header(blockHeader.get())
                            .chainWork(blockChainInfo.getChainWork())
                            .height(blockChainInfo.getHeight())
                            .sizeInBytes(blockChainInfo.getTotalChainSize())
                            .build());
        return result;
    }

    @Override
    public List<Sha256Wrapper> getTipsChains() {
        List<Sha256Wrapper> result = tipsChains.getHashes().stream().map(h -> Sha256Wrapper.wrap(h)).collect(Collectors.toList());
        return result;
    }

    @Override
    public Optional<ChainInfo> getLongestChain() {
        List<ChainInfo> tips = getState().getTipsChains();
        if (tips.size() == 0) return Optional.empty();
        if (tips.size() == 1) return Optional.of(tips.get(0));

        // There are more than one chain ( there is one or more FORKS). So we need to locate the Longest one:
        int maxHeight = tips.stream().mapToInt(c -> c.getHeight()).max().getAsInt();
        Optional<ChainInfo> result = tips.stream().filter(c -> c.getHeight() == maxHeight).findFirst();
        return result;
    }

    @Override
    public synchronized void prune(Sha256Wrapper tipChainHash, boolean removeTxs) {

        // First we check if this Hash really is a TIP of a chain:
        if (!tipsChains.getHashes().contains(tipChainHash.toString()))
            throw new RuntimeException("The Hash specified for Prunning is NOT the Tip of any Chain.");

        log.debug("Prunning chain tip #" + tipChainHash + " ...");
        // Now we move from the tip backwards until we find a Block that has MORE than one child (one being the Block
        // we are removing)

        boolean keepGoing = true;
        Sha256Wrapper hashBlockToRemove = tipChainHash;
        long numBlocksRemoved = 0;
        while (keepGoing) {

            // We find the Parent of this Block, ad we stop if the parent has MORE than one child /that would mean that
            // that parent is the block right BEFORE the Fork), and we stop in that case
            Optional<Sha256Wrapper> parentHashOpt = getPrevBlock(hashBlockToRemove);
            if (parentHashOpt.isPresent()) {
                List<Sha256Wrapper> children = getNextBlocks(parentHashOpt.get());
                if (children.size() > 1) keepGoing = false;
            }

            // If enabled, we remove its TXs...
            if (removeTxs) removeBlockTxs(hashBlockToRemove);
            // We remove this Block
            _removeBlock(hashBlockToRemove.toString());

            numBlocksRemoved++;

            // In the next loop, we try to remove the Parent
            hashBlockToRemove = parentHashOpt.get();
        }

        log.debug("chain tip #" + tipChainHash + " Pruned. " + numBlocksRemoved + " blocks removed.");

        // We trigger a Prune Event:
        ChainPruneEvent event = ChainPruneEvent.builder()
                .tipForkHash(tipChainHash)
                .parentForkHash(hashBlockToRemove)
                .numBlocksPruned(numBlocksRemoved)
                .build();
        eventBus.publish(event);
    }

    @Override
    public BlockChainStoreState getState() {
        List<ChainInfo> tipsChainInfo = getTipsChains().stream()
                .map(h -> getBlockChainInfo(h).get())
                .collect(Collectors.toList());
        return BlockChainStoreState.builder()
                .tipsChains(tipsChainInfo)
                .numBlocks(getNumBlocks())
                .numTxs(getNumTxs())
        .build();
    }


    @Override
    public BlockChainStoreStreamer EVENTS() {
        return blockChainStoreStreamer;
    }
}
