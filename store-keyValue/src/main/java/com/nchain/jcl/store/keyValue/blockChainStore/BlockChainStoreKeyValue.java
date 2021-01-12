package com.nchain.jcl.store.keyValue.blockChainStore;

import com.nchain.jcl.base.domain.api.base.BlockHeader;
import com.nchain.jcl.base.domain.api.extended.ChainInfo;
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper;
import com.nchain.jcl.store.blockChainStore.BlockChainStore;
import com.nchain.jcl.store.blockChainStore.BlockChainStoreState;
import com.nchain.jcl.store.blockChainStore.events.ChainForkEvent;
import com.nchain.jcl.store.blockChainStore.events.ChainPruneEvent;
import com.nchain.jcl.store.blockChainStore.events.ChainStateEvent;
import com.nchain.jcl.store.keyValue.blockStore.BlockStoreKeyValue;
import com.nchain.jcl.store.keyValue.common.HashesList;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Thsi interface extends teh "BlockStore" and adds capabilities to store and traverse the Chain of Blocks, and
 * also detected Forks (and prune them).
 *
 * In additiona to all the infor already sotred by the "BlockStore" interface, this one adds some more.
 * A Block has now other Entries, all under the "BLOCKS" directory:
 *
 *  "b:[blockHash]:next:    Stores a LIST of those Blcoks built on top of this one. Its usually a one-element list, but
 *                          in case of a FORK there might be more than one.
 *
 *  "b:chain:[blockHash]":  an instance of b.lockChainInfo. If this Block can be CONNECTED to the Chain (meaning that
 *                          its parent is also stored and connected to the Chain), then this isntance stores the
 *                          relative chain info for this Block.*
 *
 *  Also, there is a new Key (in the "BLOCKCHAIN" directory) that stores a List with the TIPS of the Chain
 *  (Block Hashes). Its usually a one-element List, but in case of a FORK it might contain more than one element.
 *
 * @param <E>   Type of each ENTRY in the DB. Each Key-Value DB implementation usually provides Iterators that returns
 *              Entries from the DB (KeyValue in FoundationDB, a Map.Entry in LevelDb, etc).
 * @param <T>   Type of the TRANSACTION used by the DB-specific implementation. If Transactions are NOT supported, the
 *              "Object" Type can be used.
 */
public interface BlockChainStoreKeyValue<E, T> extends BlockStoreKeyValue<E, T>, BlockChainStore {

    // Keys used to store block info and it's relative position within the Chain:
    String KEY_SUFFIX_BLOCK_NEXT     = "next";        // Block built on top of this one
    String KEY_PREFFIX_BLOCK_CHAIN   = "b_chain";     // Chan info for this block
    String KEY_CHAIN_TIPS            = "chain_tips";  // List of all the Tip Chains

    /* Functions to generate Simple Keys in String format: */

    default String keyForBlockNext(String blockHash)        { return KEY_PREFFIX_BLOCK_PROP + KEY_SEPARATOR + blockHash + KEY_SEPARATOR + KEY_SUFFIX_BLOCK_NEXT; }
    default String keyForBlockChainInfo(String blockHash)   { return KEY_PREFFIX_BLOCK_CHAIN + KEY_SEPARATOR + blockHash + KEY_SEPARATOR; }
    default String keyForChainTips()                        { return KEY_CHAIN_TIPS + KEY_SEPARATOR; }

    /* Functions to generate WHOLE Keys, from the root up to the item. to be implemented by specific DB provider */

    byte[] fullKeyForBlockNext(String blockHash);
    byte[] fullKeyForBlockChainInfo(String blockHash);
    byte[] fullKeyForChainTips();

    /* Functions to serialize Objects:  */

    default byte[] bytes(BlockChainInfo blockChainInfo)     { return BlockChainInfoSerializer.getInstance().serialize(blockChainInfo); }

    /* Functions to deserialize Objects: */

    default BlockChainInfo  toBlockChainInfo(byte[] bytes)  { return (isBytesOk(bytes)) ? BlockChainInfoSerializer.getInstance().deserialize(bytes) : null;}

    /*
     BlockChain Store DB Operations:
     These methods execute the business logic. Most of the time, each one of the methods below map a method of the
     BlockStore interface, but with some peculiarities:
     - They do NOT trigger Events
     - They do NOT crete new DB Transaction, instead they need to reuse one passed as a parameter.

     The Events and Transactions are created at a higher-level (byt he public methods that implemen the BlockStore
     interface).
     */

    private BlockChainInfo _getBlockChainInfo(T tr, String blockHash) {
        byte[] value = read(tr, fullKeyForBlockChainInfo(blockHash));
        return toBlockChainInfo(value);
    }

    private BlockChainInfo _saveBlockChainInfo(T tr, BlockHeader block, BlockChainInfo parentBlockChainInfo) {

        // We calculate the Height and total chain size:
        int resultHeight = (parentBlockChainInfo != null)
                ? parentBlockChainInfo.getHeight() + 1
                : 0;
        long resultChainSize = (parentBlockChainInfo != null)
                ? block.getSizeInBytes() + parentBlockChainInfo.getTotalChainSize()
                : block.getSizeInBytes();

        // We build the object and save it:
        BlockChainInfo blockChainInfo = BlockChainInfo.builder()
                .blockHash(block.getHash().toString())
                .chainWork(block.getWork())
                .height(resultHeight)
                .totalChainSize(resultChainSize)
                .build();
        save(tr, fullKeyForBlockChainInfo(block.getHash().toString()), bytes(blockChainInfo));
        return blockChainInfo;
    }

    private HashesList _getNextBlocks(T tr, String blockHash) {
        HashesList result = toHashes(read(tr, fullKeyForBlockNext(blockHash)));
        if (result == null) result = HashesList.builder().build();
        return result;
    }

    private void _addChildToBlock(T tr, String parentBlockHash, String childBlockHash) {
        HashesList childs = _getNextBlocks(tr, parentBlockHash);
        if (childs == null) childs = HashesList.builder().build();
        if (!(childs.getHashes().contains(childBlockHash)))
            childs.getHashes().add(childBlockHash);
        save(tr, fullKeyForBlockNext(parentBlockHash), bytes(childs));
    }

    private void _removeChildFromBlock(T tr, String parentBlockHash, String childBlockHash) {
        HashesList childs = _getNextBlocks(tr, parentBlockHash);
        if (childs != null) {
            childs.getHashes().remove(childBlockHash);
            if (childs.getHashes().size() > 0)
                    save(tr, fullKeyForBlockNext(parentBlockHash), bytes(childs));
            else    remove(tr, fullKeyForBlockNext(parentBlockHash));
        }
    }

    default HashesList _getChainTips(T tr) {
        HashesList result = toHashes(read(tr, fullKeyForChainTips()));
        if (result == null) result = HashesList.builder().build();
        return result;
    }

    private void _saveChainTips(T tr, HashesList chainstips) {
        save(tr, fullKeyForChainTips(), bytes(chainstips));
    }

    private void _updateTipsChain(T tr, String blockHashToAdd, String blockHashToRemove) {
        HashesList tipsChain = _getChainTips(tr);
        if (blockHashToAdd != null)     tipsChain.getHashes().add(blockHashToAdd);
        if (blockHashToRemove != null)  tipsChain.getHashes().remove(blockHashToRemove);
        _saveChainTips(tr, tipsChain);
    }

    private void _connectBlock(T tr, BlockHeader blockHeader, BlockChainInfo parentBlockChainInfo) {
        // We store the BlockChain Info of this new Block:
        BlockChainInfo blockChainInfo = _saveBlockChainInfo(tr, blockHeader, parentBlockChainInfo);

        HashesList tipsChain = _getChainTips(tr);

        // If the Parent is part of the TIPS of the Chains, then it must be removed from it:
        if (parentBlockChainInfo != null && tipsChain.getHashes().contains(parentBlockChainInfo.getBlockHash())) {
            _updateTipsChain(tr, null, parentBlockChainInfo.getBlockHash()); // we don't add, just remove
        }

        // Now we look into the CHILDREN (Blocks built on top of this Block), and we connect them as well...
        // If the Block has NOT Children, then this is the Last Block that can be connected, so we add it to the Tips
        HashesList children = _getNextBlocks(tr, blockHeader.getHash().toString());
        if (children != null && children.getHashes().size() > 0) {
            for (String childHashHex : children.getHashes()) {
                Optional<BlockHeader> childBlock = getBlock(Sha256Wrapper.wrap(childHashHex));
                if (childBlock.isPresent()) _connectBlock(tr, childBlock.get(), blockChainInfo);
            }
        } else {
            _updateTipsChain(tr, blockHeader.getHash().toString(), null); // We add this block to the Tips
        }
    }

    private void _disconnectBlock(T tr, String blockHash) {
        // If this block is already connected we remove the Chain Info:
        BlockChainInfo blockChainInfo = _getBlockChainInfo(tr, blockHash);
        if (blockChainInfo != null) {
            // We remove the ChainInfo for this Node:
            remove(tr, fullKeyForBlockChainInfo(blockHash));

            // We update the tip of the chain (this block is not the tip anymore, if its already)
            _updateTipsChain(tr, null, blockHash);

            // We remove all the Chain Info from its Children...
            HashesList children = _getNextBlocks(tr, blockHash);
            if (children != null)
                children.getHashes().forEach(h -> _disconnectBlock(tr, h));
        }
    }

    default void _initGenesisBlock(T tr, BlockHeader genesisBlock) {
        _saveBlock(tr, genesisBlock);
        _connectBlock(tr, genesisBlock, null); // No parent for this block
    }

    default void _publishState() {
        ChainStateEvent event = ChainStateEvent.builder().state(getState()).build();
        getEventBus().publish(event);
    }


    @Override
    default void _saveBlock(T tr, BlockHeader blockHeader) {
        String parentHashHex = blockHeader.getPrevBlockHash().toString();

        // we save te Block...:
        BlockStoreKeyValue.super._saveBlock(tr, blockHeader);

        // and its relation with its parent
        _addChildToBlock(tr, parentHashHex, blockHeader.getHash().toString());

        // If the Parent exists and it's also Connected, we connect this one too:
        BlockChainInfo parentChainInfo =  _getBlockChainInfo(tr, parentHashHex);
        if (parentChainInfo != null) {
            _connectBlock(tr, blockHeader, parentChainInfo);

            // If this is a fork, we trigger a Fork Event:
            HashesList parentChilds = _getNextBlocks(tr, blockHeader.getPrevBlockHash().toString());
            if (parentChilds != null && parentChilds.getHashes().size() > 1) {
                ChainForkEvent event = ChainForkEvent.builder()
                        .blockForkHash(blockHeader.getHash())
                        .parentForkHash(blockHeader.getPrevBlockHash())
                        .build();
                getEventBus().publish(event);
            }
        }
    }

    @Override
    default void _removeBlock(T tr, String blockHash) {
        // Basic check if the block exists:
        BlockHeader block = _getBlock(tr, blockHash);
        if (block == null) return;

        // We remove the relationship between this block and its parent:
        _removeChildFromBlock(tr, block.getPrevBlockHash().toString(), blockHash);
        _disconnectBlock(tr, blockHash);

        // We update the tip of the chain (the parent is now the tip of the chain)
        // If the parent has already other children then we do NOT do it, since that would mean that that parent
        // is already part of other chain
        HashesList parentChildren = _getNextBlocks(tr, block.getPrevBlockHash().toString());
        if (parentChildren == null || parentChildren.getHashes().size() == 0)
            _updateTipsChain(tr, block.getPrevBlockHash().toString(), null);

        // we remove the Block the usual way:
        BlockStoreKeyValue.super._removeBlock(tr, blockHash);
    }



    @Override
    default BlockChainStoreState getState() {
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
    default List<Sha256Wrapper> getTipsChains() {
        List<Sha256Wrapper> result = new ArrayList<>();
        T tr = createTransaction();
        executeInTransaction(tr , () -> {
            HashesList tipsChain = _getChainTips(tr);
            result.addAll(tipsChain.getHashes().stream().map(h -> Sha256Wrapper.wrap(h)).collect(Collectors.toList()));
        });
        return result;
    }

    @Override
    default void removeTipsChains() {
        T tr = createTransaction();
        executeInTransaction(tr, () -> {
            _saveChainTips(tr, HashesList.builder().build());
        });
    }

    @Override
    default Optional<ChainInfo> getBlockChainInfo(Sha256Wrapper blockHash) {
        AtomicReference<ChainInfo> result = new AtomicReference<>();
        T tr = createTransaction();
        executeInTransaction(tr, () -> {
            BlockHeader block = _getBlock(tr, blockHash.toString());
            if (block == null) return;

            byte[] value = read(tr, fullKeyForBlockChainInfo(blockHash.toString()));
            BlockChainInfo blockChainInfo = toBlockChainInfo(value);
            ChainInfo chainInfoResult = ChainInfo.builder()
                    .header(block)
                    .chainWork(blockChainInfo.getChainWork())
                    .height(blockChainInfo.getHeight())
                    .sizeInBytes(blockChainInfo.getTotalChainSize())
                    .build();
            result.set(chainInfoResult);
        });
        return Optional.ofNullable(result.get());
    }

    @Override
    default Optional<ChainInfo> getLongestChain() {
        List<ChainInfo> tips = getState().getTipsChains();
        if (tips.size() == 0) return Optional.empty();
        if (tips.size() == 1) return Optional.of(tips.get(0));

        // There are more than one chain ( there is one or more FORKS). So we need to locate the Longest one:
        int maxHeight = tips.stream().mapToInt(c -> c.getHeight()).max().getAsInt();
        Optional<ChainInfo> result = tips.stream().filter(c -> c.getHeight() == maxHeight).findFirst();
        return result;
    }

    @Override
    default Optional<Sha256Wrapper> getPrevBlock(Sha256Wrapper blockHash) {
        Optional<Sha256Wrapper> result = getBlock(blockHash).map(b -> b.getPrevBlockHash());
        return result;
    }

    @Override
    default List<Sha256Wrapper> getNextBlocks(Sha256Wrapper blockHash) {
        List<Sha256Wrapper> result = new ArrayList<>();
        T tr = createTransaction();
        executeInTransaction(tr, () -> {
            HashesList children = _getNextBlocks(tr, blockHash.toString());
            result.addAll(children.getHashes().stream().map(h -> Sha256Wrapper.wrap(h)).collect(Collectors.toList()));
        });
        return result;
    }

    @Override
    default void prune(Sha256Wrapper tipChainHash, boolean removeTxs) {
        getLogger().debug("Prunning chain tip #" + tipChainHash + " ...");
        synchronized (this.getClass()) {

            List<Sha256Wrapper> tipsChains = getTipsChains();

            // First we check if this Hash really is a TIP of a chain:
            if (!tipsChains.contains(tipChainHash))
                throw new RuntimeException("The Hash specified for Prunning is NOT the Tip of any Chain.");

            getLogger().debug("Prunning chain tip #" + tipChainHash + " ...");

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
                removeBlock(hashBlockToRemove);

                numBlocksRemoved++;

                // In the next loop, we try to remove the Parent
                hashBlockToRemove = parentHashOpt.get();
            } // while...

            getLogger().debug("chain tip #" + tipChainHash + " Pruned. " + numBlocksRemoved + " blocks removed.");

            // We trigger a Prune Event:
            ChainPruneEvent event = ChainPruneEvent.builder()
                    .tipForkHash(tipChainHash)
                    .parentForkHash(hashBlockToRemove)
                    .numBlocksPruned(numBlocksRemoved)
                    .build();
            getEventBus().publish(event);

        } // synchronized...
    }
}
