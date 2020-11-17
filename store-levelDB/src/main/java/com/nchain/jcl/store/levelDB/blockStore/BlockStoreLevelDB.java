package com.nchain.jcl.store.levelDB.blockStore;

import com.google.common.primitives.Longs;
import com.nchain.jcl.base.domain.api.base.BlockHeader;
import com.nchain.jcl.base.domain.api.base.Tx;
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper;
import com.nchain.jcl.base.tools.events.EventBus;
import com.nchain.jcl.base.tools.thread.ThreadUtils;
import com.nchain.jcl.store.blockStore.BlockStore;
import com.nchain.jcl.store.blockStore.BlocksCompareResult;
import com.nchain.jcl.store.blockStore.events.*;
import com.nchain.jcl.store.levelDB.common.HashesList;
import static com.nchain.jcl.store.levelDB.blockStore.BlockStoreKeyValueUtils.*;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import static org.iq80.leveldb.impl.Iq80DBFactory.factory;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Implementation of the BlockStore component using LevelDB as the Tech Stack.
 */

@Slf4j
public class BlockStoreLevelDB implements BlockStore {

    // When triggering Events, some times a Pagination is needed:
    private static final int TX_PAG_SIZE_DEFAULT = 10_000;

    // Working Folder where te LevelDB files will be stored:
    private static final String LEVELDB_FOLDER = "store/levelDB";

    // Configuration:
    private final BlockStoreLevelDBConfig config;
    private final boolean triggerBlockEvents;
    private final boolean triggerTxEvents;

    // Events Configuration:
    private final ExecutorService executorService;
    protected final EventBus eventBus;
    private final BlockStoreStreamer blockStoreStreamer;

    // LevelDB instance:
    protected final DB levelDBStore;


    /** Constructor */
    @Builder
    public BlockStoreLevelDB(BlockStoreLevelDBConfig config,
                             Boolean triggerBlockEvents,
                             Boolean triggerTxEvents) throws RuntimeException {
        try {
            this.config = config;
            this.triggerBlockEvents = (triggerBlockEvents != null) ? triggerBlockEvents : false;
            this.triggerTxEvents = (triggerTxEvents != null) ? triggerTxEvents : false;

            // LevelDB engine configuration:
            Options options = new Options();
            Path levelDBPath = Paths.get(config.getWorkingFolder().toString(), LEVELDB_FOLDER);
            levelDBStore = factory.open(levelDBPath.toFile(), options);

            // Events Configuration:
            this.executorService = ThreadUtils.getThreadPoolExecutorService("BlockStore-LevelDB");
            this.eventBus = EventBus.builder().executor(this.executorService).build();
            this.blockStoreStreamer = new BlockStoreStreamer(this.eventBus);

        } catch (IOException ioe) {
            log.error(ioe.getMessage());
            throw new RuntimeException(ioe);
        }
    }


    /**
     * A generic method to loop over Keys and to perform an operation with each one of them.
     * It allows for pagination by using the "startingKeyIndex" and "maxKeysToProcess".
     *
     * @param keyPrefix         Key prefix. Only those Keys that start with these Preffix will be processed
     * @param startingKeyIndex  The first Key found has the index 0, the next 1, and so on. Only the Keys which index
     *                          is equals or higher than "startingKeyIndex" will be processed.
     * @param maxKeysToProcess  Maximum number of Keys to process. If not presents, all the keys found are processed.
     * @param taskForKey        A piece of code that is executed for each Key that is found.
     * @param taskForAllKeys    A piece of code that is executed for all the Keys retrieved
     *                          IMPORTANT: The task defined hare is a consumer that takes aList of Keys (String) as
     *                          a parameter. So we need to consider how many Keys are we going to loop over, if the
     *                          number of them is too big, this coud cause a memory problem when passing a list of
     *                          all those keys to this task. So only use this task when you are confident that the
     *                          number of keys won't break the memory.
     */
    protected void _loopOverKeysAndRun(String keyPrefix,
                                     String keySuffix,
                                     Long startingKeyIndex,
                                     Optional<Long> maxKeysToProcess,
                                     Consumer<String> taskForKey,
                                     Consumer<List<String>> taskForAllKeys) {
        try {
            DBIterator iterator = levelDBStore.iterator();
            iterator.seek(bytes(keyPrefix));

            // We store each one of the Keys we process, so we can also trigger the global "taskForAllKeys", passing
            // all of them as a parameter. This list could be potentially huge, so we only use it if the
            // "taskForAllKeys" as been set (not null)
            List<String> allKeysToProcess = new ArrayList<>();

            long currentIndex = -1;
            long numKeysProcessed = 0;
            while(true) {
                if (!iterator.hasNext()) break;

                // We advance the Iterator...
                String key = new String(iterator.next().getKey());
                currentIndex++;

                // Exit Conditions:
                if (!key.startsWith(keyPrefix)) break;
                if (keySuffix != null && !key.endsWith(keySuffix)) break;
                if (maxKeysToProcess.isPresent() && (numKeysProcessed >= maxKeysToProcess.get())) break;
                if (currentIndex < startingKeyIndex) continue;

                // If we reach this far then it means that we have to process it, so we do it and move forward:
                taskForKey.accept(key);
                numKeysProcessed++;

                // We only use this list if the global task has been defined:
                if (taskForAllKeys != null) allKeysToProcess.add(key);
            }
            iterator.close();

            // We are done iterating over the Keys. We trigger the global Task:
            if (taskForAllKeys != null) taskForAllKeys.accept(allKeysToProcess);

        } catch (IOException ioe) {
            log.error(ioe.getMessage(), ioe);
            throw new RuntimeException(ioe);
        }
    }

    // Convenience method with suffix and without pagination
    protected void _loopOverKeysAndRun(String keyPrefix, String keySuffix, Consumer<String> individualTask, Consumer<List<String>> globalTask) {
        _loopOverKeysAndRun(keyPrefix, keySuffix,  0L, Optional.empty(), individualTask, globalTask);
    }
    // Convenience method without suffix and without pagination
    protected void _loopOverKeysAndRun(String keyPrefix, Consumer<String> individualTask, Consumer<List<String>> globalTask) {
        _loopOverKeysAndRun(keyPrefix, null,  0L, Optional.empty(), individualTask, globalTask);
    }
    // Convenience method without suffix and with pagination
    protected void _loopOverKeysAndRun(String keyPrefix, Long startingKeyIndex, Optional<Long> maxKeysToProcess,
                                     Consumer<String> individualTask, Consumer<List<String>> globalTask) {
        _loopOverKeysAndRun(keyPrefix, null,  startingKeyIndex, Optional.empty(), individualTask, globalTask);
    }

    // Basic operations:
    // This operations make the changes in the DB but do NOT trigger Events.
    // They work with more basic data types like String, which are closer to the Types used for the Keys in LevelDB.
    // Each function is responsible for leaving the DB in a consistent state. For example, the method that removes
    // a TX will also take care of removing the relationships between that Tx an the Block its linked to, if any.

    protected void _saveBlock(BlockHeader blockHeader) {
        // We save the serialized form of the Block:
        String key = getKeyForBlock(blockHeader.getHash().toString());
        levelDBStore.put(bytes(key), bytes(blockHeader));

        // We save the number of Txs (this field is NOT part of the BlockHeader Serialization)
        String txsKey = getKeyForBlockNumTxs(blockHeader.getHash().toString());
        levelDBStore.put(bytes(txsKey), bytes(blockHeader.getNumTxs()));
    }

    protected void _removeBlock(String blockHashHex) {
        // We remove the Serialized form of the Block:
        String key = getKeyForBlock(blockHashHex);
        levelDBStore.delete(bytes(key));

        // We remove the property with the number of Txs of this block:
        String txsKey = getKeyForBlockNumTxs(blockHashHex);
        levelDBStore.delete(bytes(txsKey));

        // And we unlink the Block:
        _unlinkBlock(blockHashHex);
    }

    private void _saveTx(Tx tx) {
        // We save the Whole TXs:
        String key = getKeyForTx(tx.getHash().toString());
        levelDBStore.put(bytes(key), bytes(tx));

        // We save the list of TXs NEEDED for this TX: The Txs that contains oututs that are used in THIS Txs as
        // inputs:
        Set<String> txsNeeded = tx.getInputs().stream()
                .map(i -> i.getOutpoint().getHash().toString())
                .collect(Collectors.toSet());
        HashesList txsHashesNeeded = HashesList.builder()
                .hashes(new ArrayList<>(txsNeeded))
                .build();
        String txsNeddedKey = getKeyForTxNeededTxs(tx.getHash().toString());
        levelDBStore.put(bytes(txsNeddedKey), bytes(txsHashesNeeded));
    }

    private void _removeTx(String txHash) {
        // We remove the TX itself:
        String key = getKeyForTx(txHash);
        levelDBStore.delete(bytes(key));

        // We remove the "TxsNeeded" property:
        String keyToRemove = getKeyForTxNeededTxs(txHash);
        levelDBStore.delete(bytes(keyToRemove));

        // We unlink this Tx from any block/s it might belong to
        _unlinkTx(txHash);

    }

    private List<String> _getBlockHashLinkedToTx(String txHashHex) {
        List<String> result = new ArrayList<>();
        String key = getKeyForTxBlock(txHashHex);
        byte[] value = levelDBStore.get(bytes(key));
        if (value != null && value.length > 0) {
            HashesList blockHashes = bytesToHashesList(value);
            result = blockHashes.getHashes();
        }
        return result;
    }

    private void _linkTxToBlock(String txHashHex, String blockHashHex) {

        // First of all we check this TXs does NOT belong ALREADY to this Block...
        if (!_isTxLinkToBlock(txHashHex, blockHashHex)) {

            // first we save the key to store the relationship Block-Tx: We just save an integer here, since here
            // the important thing is the KEY, not the value:
            String bTxKey = getKeyForBlockTx(blockHashHex, txHashHex);
            levelDBStore.put(bytes(bTxKey), new byte[]{1});

            // There is also a Property where we save the number of Txs belonging to this Block. We update the Value:
            String keyBlockNumTxs = getKeyForBlockNumTxs(blockHashHex);
            byte[] numTxsValue = levelDBStore.get(bytes(keyBlockNumTxs));
            Long numTxs = (numTxsValue == null)? 0L : (Longs.fromByteArray(numTxsValue) + 1);
            levelDBStore.put(bytes(keyBlockNumTxs), bytes(numTxs));

            // Now we store, for this TXs, the Block Hash it belongs to. For this Txs, we store a LIST of Block Hashes,
            // a Tx might belong to more than 1 Block (on case of a FORK)
            String txBKey = getKeyForTxBlock(txHashHex);
            HashesList blockHashes = bytesToHashesList(levelDBStore.get(bytes(txBKey)));
            if (blockHashes == null) blockHashes = HashesList.builder().build();
            blockHashes.getHashes().add(blockHashHex);
            levelDBStore.put(bytes(txBKey), bytes(blockHashes));
        }
    }

    private void _unlinkTxFromBlock(String txHashHex, String blockHashHex) {

        // First we check that this TXS really Belongs to the Block given:
        if (_isTxLinkToBlock(txHashHex, blockHashHex)) {
            // first we remove the Block-Txs relationsship:
            String bTxKey = getKeyForBlockTx(blockHashHex, txHashHex);
            levelDBStore.delete(bytes(bTxKey));

            // No we decrease the number of Txs assigned to this Block, which is stored in a separate property
            String keyBlockNumTxs = getKeyForBlockNumTxs(blockHashHex);
            byte[] numTxsValue = levelDBStore.get(bytes(keyBlockNumTxs));
            Long numTxs = (numTxsValue == null)? 0L : (Longs.fromByteArray(numTxsValue) - 1);
            levelDBStore.put(bytes(keyBlockNumTxs), bytes(numTxs));

            // Now we remove this block from the list this Txs is linked to:
            String txBKey = getKeyForTxBlock(txHashHex);
            HashesList blockHashes = bytesToHashesList(levelDBStore.get(bytes(txBKey)));
            blockHashes.getHashes().remove(blockHashHex);
            if (blockHashes.getHashes().size() > 0)
                levelDBStore.put(bytes(txBKey), bytes(blockHashes));
            else    levelDBStore.delete(bytes(txBKey));
        }
    }

    private void _unlinkTx(String txHashHex) {
        _getBlockHashLinkedToTx(txHashHex).forEach(h -> _unlinkTxFromBlock(txHashHex, h));
    }

    private void _unlinkBlock(String blockHashHex) {
        // we loop over the "btx" keys, and for each one we remove it and also remove the "txb" entry:
        String keyPrefix = PREFFIX_KEY_BLOCKTXS + blockHashHex;
        _loopOverKeysAndRun(keyPrefix, k -> _unlinkTxFromBlock(getTxHashFromKey(k), blockHashHex), null);
    }

    private boolean _isTxLinkToBlock(String txHashHex, String blockHashHex) {
        String bTxKey = getKeyForBlockTx(blockHashHex, txHashHex);
        byte[] value = levelDBStore.get(bytes(bTxKey));
        return (value != null && value.length > 0);
    }

    private void _triggerBlocksStoredEvent(List<BlockHeader> blockHeaders) {
        if (triggerBlockEvents) {
            List<Sha256Wrapper> blockHashes = blockHeaders.stream().map(b -> b.getHash()).collect(Collectors.toList());
            eventBus.publish(BlocksSavedEvent.builder().blockHashes(blockHashes).build());
        }
    }

    private void _triggerBlocksRemovedEvent(List<Sha256Wrapper> blockHashes) {
        if (triggerBlockEvents)
            eventBus.publish(BlocksRemovedEvent.builder().blockHashes(blockHashes).build());
    }

    private void _triggerTxsStoredEvent(List<Tx> txs) {
        if (triggerTxEvents) {
            List<Sha256Wrapper> txHashes = txs.stream().map(tx -> tx.getHash()).collect(Collectors.toList());
            eventBus.publish(TxsSavedEvent.builder().txHashes(txHashes).build());
        }
    }

    private void _triggerTxsRemovedEvent(List<Sha256Wrapper> txHashes) {
        if (triggerTxEvents)
            eventBus.publish(TxsRemovedEvent.builder().txHashes(txHashes).build());
    }

    // Init/post operations:

    @Override
    public void start() {
        log.info("JCL-Store Configuration:");
        log.info(" - LevelDB Implementation");
        log.info(" - working dir: " + config.getWorkingFolder().toAbsolutePath());
    }

    @Override
    public void stop() {
        try {
            levelDBStore.close();
        } catch (IOException ioe) {
            log.error(ioe.getMessage(), ioe);
            throw new RuntimeException(ioe);
        }
    }

    // Block Storage Operations:

    @Override
    public void saveBlock(BlockHeader blockHeader) {
        _saveBlock(blockHeader);
        _triggerBlocksStoredEvent(Arrays.asList(blockHeader));
    }

    @Override
    public void saveBlocks(List<BlockHeader> blockHeaders) {
        blockHeaders.forEach(b -> _saveBlock(b));
        _triggerBlocksStoredEvent(blockHeaders);
    }

    @Override
    public boolean containsBlock(Sha256Wrapper blockHash) {
        return levelDBStore.get(bytes(getKeyForBlock(blockHash.toString()))) != null;
    }

    @Override
    public Optional<BlockHeader> getBlock(Sha256Wrapper blockHash) {
        Optional<BlockHeader> result = Optional.empty();
        // We get the Block itself:
        byte[] value = levelDBStore.get(bytes(getKeyForBlock(blockHash.toString())));
        BlockHeader blockHeader = bytesToBlockHeader(value);

        // If the Block is found, we now get its "number of Txs" field:
        if (blockHeader != null) {
            String numTxsKey = getKeyForBlockNumTxs(blockHash.toString());
            Long numTxs = Longs.fromByteArray(levelDBStore.get(bytes(numTxsKey)));
            BlockHeader blockHeaderComplete = blockHeader.toBuilder().numTxs(numTxs).build();
            result = Optional.of(blockHeaderComplete);
        }
        return result;
    }

    @Override
    public void removeBlock(Sha256Wrapper blockHash) {
        _removeBlock(blockHash.toString());
        _triggerBlocksRemovedEvent(Arrays.asList(blockHash));
    }

    @Override
    public void removeBlocks(List<Sha256Wrapper> blockHashes) {
        blockHashes.forEach(b -> _removeBlock(b.toString()));
        _triggerBlocksRemovedEvent(blockHashes);
    }

    @Override
    public long getNumBlocks() {
        AtomicLong result = new AtomicLong();
        _loopOverKeysAndRun(PREFFIX_KEY_BLOCK, k -> result.incrementAndGet(),null);
        return result.get();
    }

    // Tx Storage Operations:

    @Override
    public void saveTx(Tx tx) {
        _saveTx(tx);
        _triggerTxsStoredEvent(Arrays.asList(tx));
    }

    @Override
    public void saveTxs(List<Tx> txs) {
        txs.forEach( h -> _saveTx(h));
        _triggerTxsStoredEvent(txs);
    }

    @Override
    public boolean containsTx(Sha256Wrapper txHash) {
        return levelDBStore.get(bytes(getKeyForTx(txHash.toString()))) != null;
    }

    @Override
    public Optional<Tx> getTx(Sha256Wrapper txHashHex) {
        byte[] value = levelDBStore.get(bytes(getKeyForTx(txHashHex.toString())));
        Optional<Tx> result = Optional.ofNullable(bytesToTx(value));
        return result;
    }

    @Override
    public void removeTx(Sha256Wrapper txHashHex) {
        _removeTx(txHashHex.toString());
        _triggerTxsRemovedEvent(Arrays.asList(txHashHex));
    }

    @Override
    public void removeTxs(List<Sha256Wrapper> txHashes) {
        txHashes.forEach( h -> _removeTx(h.toString()));
        _triggerTxsRemovedEvent(txHashes);
    }

    @Override
    public List<Sha256Wrapper> getTxsNeeded(Sha256Wrapper txHash) {
        String key = getKeyForTxNeededTxs(txHash.toString());
        HashesList txNeeded = bytesToHashesList(levelDBStore.get(bytes(key)));
        List<Sha256Wrapper> result = (txNeeded == null)
                ? new ArrayList<Sha256Wrapper>()
                : txNeeded.getHashes().stream().map(h -> Sha256Wrapper.wrap(h)).collect(Collectors.toList());
        return result;
    }

    @Override
    public long getNumTxs() {
        AtomicLong result = new AtomicLong();
        _loopOverKeysAndRun(PREFFIX_KEY_TX, k -> result.incrementAndGet(), null);
        return result.get();
    }

    // Block-Tx Link operations:

    @Override
    public void linkTxToBlock(Sha256Wrapper txHash, Sha256Wrapper blockHash) {
        _linkTxToBlock(txHash.toString(), blockHash.toString());
    }

    @Override
    public void unlinkTxFromBlock(Sha256Wrapper txHash, Sha256Wrapper blockHash) {
        _unlinkTxFromBlock(txHash.toString(), blockHash.toString());
    }


    @Override
    public void unlinkTx(Sha256Wrapper txHash) {
        getBlockHashLinkedToTx(txHash).forEach(blockHash -> unlinkTxFromBlock(txHash, blockHash));
    }

    @Override
    public void unlinkBlock(Sha256Wrapper blockHash) {
        _unlinkBlock(blockHash.toString());
    }

    @Override
    public boolean isTxLinkToblock(Sha256Wrapper txHash, Sha256Wrapper blockHash) {
        return _isTxLinkToBlock(txHash.toString(), blockHash.toString());
    }

    @Override
    public List<Sha256Wrapper> getBlockHashLinkedToTx(Sha256Wrapper txHash) {
        List<Sha256Wrapper> result = _getBlockHashLinkedToTx(txHash.toString()).stream()
                .map(h -> Sha256Wrapper.wrap(h))
                .collect(Collectors.toList());
        return result;
    }


    @Override
    public Iterable<Sha256Wrapper> getBlockTxs(Sha256Wrapper blockHash) {
        String key = PREFFIX_KEY_BLOCKTXS + blockHash.toString();
        return new Iterable<Sha256Wrapper>() {
            @Override
            public Iterator<Sha256Wrapper> iterator() {
                return new BlockTxsIterator(levelDBStore, key);
            }
        };
    }

    @Override
    public long getBlockNumTxs(Sha256Wrapper blockHash) {
        String key = getKeyForBlockNumTxs(blockHash.toString());
        byte[] value = levelDBStore.get(bytes(key));
        return (value != null && value.length > 0) ? Longs.fromByteArray(value) : 0L;
    }

    @Override
    public void saveBlockTxs(Sha256Wrapper blockHash, List<Tx> txs) {
        txs.forEach(tx -> {
            _saveTx(tx);
            _linkTxToBlock(tx.getHash().toString(), blockHash.toString());
        });
       _triggerTxsStoredEvent(txs);
    }

    @Override
    public void removeBlockTxs(Sha256Wrapper blockHash) {
        // The problem here is not removing the Txs belonging to this Block (which is easy), but triggering the
        // "TxsRemovedEvent" Events as we do it. We cannot trigger a single event containing the whole list of TXs,
        // since this list can be huge. So we need to PAGINATE the Event and trigger multiple events instead.
        // This is only necessary when the TX Events are active though:

        // If the TX are NOT active, we do it the "easy" way:
        if (!triggerTxEvents) {
            _loopOverKeysAndRun(PREFFIX_KEY_BLOCKTXS + blockHash, k -> _removeTx(getTxHashFromKey(k)),null);
        } else {
            // We loop over all the Txs belonging to this block, and we only trigger and Event when we reach the THRESHOLD
            // specified for that
            Iterator<Sha256Wrapper> txsIt = getBlockTxs(blockHash).iterator();
            List<Sha256Wrapper> txsInEvent = new ArrayList<>();
            while (txsIt.hasNext()) {

                Sha256Wrapper txHash = txsIt.next();
                txsInEvent.add(txHash);
                //System.out.println("Removing Tx " + txsInEvent.size() + " :: " + txsInEvent.get(txsInEvent.size() - 1));
                _removeTx(txHash.toString());
                if (txsInEvent.size() == TX_PAG_SIZE_DEFAULT) {
                    _triggerTxsRemovedEvent(txsInEvent);
                    txsInEvent.clear();
                }
            } // while...
            if (txsInEvent.size() > 0) _triggerTxsRemovedEvent(txsInEvent);
        }
    }

    @Override
    public Optional<BlocksCompareResult> compareBlocks(Sha256Wrapper blockHashA, Sha256Wrapper blockHashB) {
        Optional<BlockHeader> blockHeaderA = getBlock(blockHashA);
        Optional<BlockHeader> blockHeaderB = getBlock(blockHashB);

        if (blockHeaderA.isEmpty() || blockHeaderB.isEmpty()) return Optional.empty();

        BlocksCompareResult.BlocksCompareResultBuilder resultBuilder = BlocksCompareResult.builder()
                .blockA(blockHeaderA.get())
                .blockB(blockHeaderB.get());

        // We create an Iterable for the TXs in common:
        String commonKeyPreffix = PREFFIX_KEY_BLOCKTXS + blockHashA.toString();
        Predicate<String> commonKeyValid = k ->  _isTxLinkToBlock(getTxHashFromKey(k), blockHashB.toString());
        Iterable<Sha256Wrapper> commonIt = () -> new BlockTxsIterator(levelDBStore, commonKeyPreffix, commonKeyValid);
        resultBuilder.txsInCommonIt(commonIt);

        // We create an Iterable for the TXs that are ONLY in the block A:
        String onlyAKeyPreffix = PREFFIX_KEY_BLOCKTXS + blockHashA.toString();
        Predicate<String> onlyAKeyValid = k ->  !_isTxLinkToBlock(getTxHashFromKey(k), blockHashB.toString());
        Iterable<Sha256Wrapper> onlyAIt = () -> new BlockTxsIterator(levelDBStore, onlyAKeyPreffix, onlyAKeyValid);
        resultBuilder.txsOnlyInA(onlyAIt);

        // We create an Iterable for the TXs that are ONLY in the block B:
        String onlyBKeyPreffix = PREFFIX_KEY_BLOCKTXS + blockHashB.toString();
        Predicate<String> onlyBKeyValid = k ->  !_isTxLinkToBlock(getTxHashFromKey(k), blockHashA.toString());
        Iterable<Sha256Wrapper> onlyBIt = () -> new BlockTxsIterator(levelDBStore, onlyBKeyPreffix, onlyBKeyValid);
        resultBuilder.txsOnlyInB(onlyBIt);

        return Optional.of(resultBuilder.build());
    }

    @Override
    public BlockStoreStreamer EVENTS() {
        return this.blockStoreStreamer;
    }

    @Override
    public long getNumKeys(String keyPrefix) {
        AtomicLong result = new AtomicLong();
        _loopOverKeysAndRun(keyPrefix, k -> result.incrementAndGet(), null);
        return result.get();
    }

    @Override
    public void printKeys() {
        DBIterator it = levelDBStore.iterator();
        it.seekToFirst();
        while (it.hasNext()) System.out.println(" > " + new String(it.next().getKey()) + " ...");
    }
}
