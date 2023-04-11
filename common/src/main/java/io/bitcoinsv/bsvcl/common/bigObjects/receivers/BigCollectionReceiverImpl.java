package io.bitcoinsv.bsvcl.common.bigObjects.receivers;


import io.bitcoinsv.bsvcl.common.bigObjects.BigCollectionChunk;
import io.bitcoinsv.bsvcl.common.bigObjects.stores.BigCollectionChunksStore;
import io.bitcoinsv.bsvcl.common.bigObjects.stores.ObjectSerializer;
import io.bitcoinsv.bsvcl.common.config.RuntimeConfig;
import io.bitcoinsv.bsvcl.common.util.TriConsumer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An implementation of BigCollectionReceiver
 *
 * NOTE: This component assumes that the Collections are received during the same run. So, if a Colelction is only
 * PARTIALLY received and the execution stops, next time we start over the DB might be in a inconsistent
 * state, so it's reponsability of the User to clean the DB before start.
 *
 * NOTE: In case we receive items for the SAME collection but from DIFFERENT Sources, we store all of them. To do that
 * without overlapping, we use a "conbined" id for a collection, which is [collectionId] + [source]. So the collection
 * items are saved by the "chunksStore" using these combined Ids.
 */
public class BigCollectionReceiverImpl<I> implements BigCollectionReceiver<I> {

    // String used to concat collectionId + Source and generate a combined String that will be used as colelctionId
    // from the underlying BigCollectionStore object:
    private static String CONCAT_STR = "[---]";

    private RuntimeConfig runtimeConfig;
    private String receiverId;
    private ObjectSerializer<I> itemSerializer;
    private BigCollectionChunksStore<I> chunksStore;

    /**
     * This class stores the State of each Collection, keeping Track of the items being received of each one, etc...
     */
    class CollectionState {
        private String collectionId;
        private Long numTotalItems;
        private long numCurrentItems;
        private boolean completed;
        private String source;

        public CollectionState(String collectionId, String source) {
            this.collectionId = collectionId;
            this.source = source;
        }
        public String getCollectionId()     { return this.collectionId;}
        public String getSource()           { return this.source;}
        public Long getNumTotalItems()      { return this.numTotalItems;}
        public long getNumCurrentItems()    { return this.numCurrentItems;}
        public boolean isCompleted()        { return completed; }
        public void setCompleted()          { completed = true; }
    }

    // A Map to keep track of the state of each incoming Collection:
    private Map<String, List<CollectionState>> collectionsState = new ConcurrentHashMap<>();

    // Callbacks:
    private BiConsumer<String, String> onCollectionCompletedCallback;
    private TriConsumer<String, BigCollectionChunk<I>, String> onItemsReceivedCallback;
    private BiConsumer<String, String> onSourceChangedCallback;

    /**
     * Constructor.
     * @param runtimeConfig             JCL Runtime Config
     * @param receiverId                Used to create the Folder where the Files will be saved
     * @param itemSerializer            Serializer of the Collection Items
     * @param chunksStore               Store of the Collection Items
     */
    public BigCollectionReceiverImpl(RuntimeConfig runtimeConfig,
                                     String receiverId,
                                     ObjectSerializer<I> itemSerializer,
                                     BigCollectionChunksStore<I> chunksStore) {
        this.runtimeConfig = runtimeConfig;
        this.receiverId = receiverId;
        this.itemSerializer = itemSerializer;
        this.chunksStore = chunksStore;
    }

    private String combine(String collectionId, String source) {
        return collectionId + CONCAT_STR + source;
    }

    private Optional<CollectionState> getColState(String collectionId, String source) {
        if (!collectionsState.containsKey(collectionId)) return Optional.empty();
        return collectionsState.get(collectionId).stream().filter(c -> c.source.equals(source)).findFirst();
    }

    private void checkCompletionAndCallback(String collectionId, String source) {
        if (!collectionsState.containsKey(collectionId)) return;

        CollectionState colState = getColState(collectionId, source).get();
        if ((colState.numTotalItems != null) && (colState.numCurrentItems == colState.numTotalItems)) {
            colState.setCompleted();
            // we update the "completed" state of this collection in the store:
            String combinedId = this.combine(collectionId, source);
            this.chunksStore.registerAsCompleted(combinedId);
        }
        // If specified, We triggered the callback...
        if (colState.isCompleted() && this.onCollectionCompletedCallback != null) {

            this.onCollectionCompletedCallback.accept(collectionId, colState.source);
        }
    }

    private void checkSourceChangeAndCallback(String collectionId, String incomingSource) {
        if (!collectionsState.containsKey(collectionId)) return;
        List<String> sources = collectionsState.get(collectionId).stream().map(c -> c.source).collect(Collectors.toList());
        if ((!sources.contains(incomingSource) && (this.onSourceChangedCallback != null))) {
            this.onSourceChangedCallback.accept(collectionId, incomingSource);
        }
    }

    @Override
    public void start() {
        // first we start the Store...
        this.chunksStore.start();
        // Then we build the initial state of our "collectionsState", where we keep the state of each Collection. By
        // convention, the partially downloaded collections are removed on startup, so at this moment all the collections
        // stored in "chunksStore" are completed. However, the Id used to store them is a combination of
        // [collectionID] + [source], so here we get the list and extract the "real" collectionId from them.

        this.chunksStore.getCollectionsIds().stream()
                .forEach(id -> {
                    String collectionId = id.substring(0, id.indexOf(CONCAT_STR));
                    String source = id.substring(id.indexOf(CONCAT_STR) + CONCAT_STR.length());
                    CollectionState colState = new CollectionState(collectionId, source);
                    colState.setCompleted();
                    collectionsState.put(colState.collectionId, Arrays.asList(colState));
                });
    }

    @Override
    public void stop() {
        this.chunksStore.stop();
    }


    @Override
    public synchronized void registerNumTotalItems(String collectionId, long numTotalItems, String source) {
        // We check if the source has changed:
        checkSourceChangeAndCallback(collectionId, source);
        // We access the State of this Collection and source and update the total num of Items:
        Optional<CollectionState> colStateOpt = getColState(collectionId, source);
        CollectionState colState = colStateOpt.orElseGet(() -> new CollectionState(collectionId, source));
        colState.numTotalItems = numTotalItems;
        collectionsState.merge(collectionId, new ArrayList<>(){{add(colState);}}, (o, n) -> {
            n.addAll(o);
            return n;
        });
        // we check if the Collection is completed:
        checkCompletionAndCallback(collectionId, source);
    }

    @Override
    public synchronized void registerIncomingItems(String collectionId, BigCollectionChunk<I> chunk, String source) {
        // We check if the source has changed:
        checkSourceChangeAndCallback(collectionId, source);
        // We access the State of this Collection and source and update the num of Items received:
        Optional<CollectionState> colStateOpt = getColState(collectionId, source);
        CollectionState colState = colStateOpt.orElseGet(() -> new CollectionState(collectionId, source));
        colState.numCurrentItems += chunk.getItems().size();
        collectionsState.merge(collectionId, new ArrayList<>(){{add(colState);}}, (o, n) -> {
            n.addAll(o);
            return n;
        });
        // From the underlying BigCollectionStore standpoint, the CollectionId will be a combination of
        // [collectionId] + [source].
        String combinedColId = combine(collectionId, source);
        chunksStore.save(combinedColId, chunk);

        // We trigger the callback
        if (this.onItemsReceivedCallback != null) {
            this.onItemsReceivedCallback.accept(collectionId, chunk, source);
        }

        // we check if the Collection is completed:
        checkCompletionAndCallback(collectionId, source);
    }

    @Override
    public boolean isCompleted(String collectionId) {
        List<CollectionState> colsStates = collectionsState.get(collectionId);
        if (colsStates == null) { return false;}
        boolean result = colsStates.stream()
                .map(c -> combine(c.collectionId, c.source))
                .anyMatch(cid -> chunksStore.isCompleted(cid));
        return result;
    }

    @Override
    public boolean contains(String collectionId) {
        return collectionsState.containsKey(collectionId);
    }

    @Override
    public synchronized long sizeInBytes() {
        return chunksStore.sizeInBytes();
    }

    @Override
    public synchronized void onCollectionCompleted(BiConsumer<String, String> callback) {
        this.onCollectionCompletedCallback = callback;
    }

    @Override
    public void onItemsReceived(TriConsumer<String, BigCollectionChunk<I>, String> callback) {
        this.onItemsReceivedCallback = callback;
    }

    @Override
    public void onSourceChanged(BiConsumer<String, String> callback) {
        this.onSourceChangedCallback = callback;
    }

    @Override
    public synchronized Iterator<BigCollectionChunk<I>> getCollectionChunks(String collectionId) {
        // we get an Iterator form the first Source that has completed:
        CollectionState colState = collectionsState.get(collectionId).stream().filter(c -> c.isCompleted()).findFirst().get();
        String combinedColId = combine(colState.collectionId, colState.source);
        return chunksStore.getChunks(combinedColId);
    }

    @Override
    public synchronized void remove(String collectionId) {
        if (!collectionsState.containsKey(collectionId)) return;

        // We remove this CollectionId and the information we save from ALL the sources...
        for (CollectionState colState : collectionsState.get(collectionId)) {
            String combinedColId = combine(colState.collectionId, colState.source);
            chunksStore.remove(combinedColId);
        }
        collectionsState.remove(collectionId);
    }

    @Override
    public synchronized void clear() {
        chunksStore.clear();
        collectionsState.clear();
    }

    @Override
    public synchronized void destroy() {
        chunksStore.destroy();
        collectionsState.clear();
    }

    @Override
    public synchronized void compact() {
        chunksStore.compact();
    }

}