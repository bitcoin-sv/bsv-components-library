package io.bitcoinsv.bsvcl.tools.bigObjects.receivers;

import com.google.common.base.Preconditions;
import io.bitcoinsv.bsvcl.tools.bigObjects.receivers.events.BigObjectHeaderReceivedEvent;
import io.bitcoinsv.bsvcl.tools.bigObjects.receivers.events.BigObjectItemsReceivedEvent;
import io.bitcoinsv.bsvcl.tools.bigObjects.receivers.events.BigObjectReceivedEvent;
import io.bitcoinsv.bsvcl.tools.bigObjects.receivers.events.BigObjectSourceChangedEvent;
import io.bitcoinsv.bsvcl.tools.bigObjects.BigCollectionChunk;
import io.bitcoinsv.bsvcl.tools.bigObjects.stores.ObjectStore;
import io.bitcoinsv.bsvcl.tools.events.EventBus;
import io.bitcoinsv.bsvcl.tools.thread.ThreadUtils;
import io.bitcoinsv.bsvcl.tools.util.TriConsumer;


import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Base Implementation of BigObjectPlusReceiver.
 */
public class BigObjectHeaderPlusReceiverImpl<H,I> implements BigObjectHeaderPlusReceiver<H,I> {

    private ObjectStore<H> headerStore;
    private BigCollectionReceiver<I> itemsReceiver;
    private boolean headersProcessingEnabled;

    private EventBus eventBus;
    public BigObjectHeaderPlusReceiverEventStreamer EVENTS;

    private Lock lock = new ReentrantLock();

    // We keep a reference to those BigObjects from which we received Headers or all the items
    private Set<String> objectsHeadersReceived     = ConcurrentHashMap.newKeySet();
    private Set<String> objectsCollectionCompleted = ConcurrentHashMap.newKeySet();

    /**
     * Constructor.
     * @param headerStore                   ObjectStore of the HEADER of each Object
     * @param itemsReceiver                 Receiver of the Bg Collectins of each Object
     * @param headersProcessingEnabled      if TRUE, Headers are processed, otherwise only Collections are.
     */
    public BigObjectHeaderPlusReceiverImpl(ObjectStore<H> headerStore,
                                           BigCollectionReceiver<I> itemsReceiver,
                                           boolean headersProcessingEnabled) {
        this.headerStore = headerStore;
        this.itemsReceiver = itemsReceiver;
        this.headersProcessingEnabled = headersProcessingEnabled;

        this.eventBus = EventBus.builder()
                .executor(ThreadUtils.getSingleThreadExecutorService("RawBlockReceiver"))
                .build();
        this.EVENTS = new BigObjectHeaderPlusReceiverEventStreamer(eventBus);

        // We link the callbacks of the Receiver to our EventBus:
        this.itemsReceiver.onCollectionCompleted((blockHash, source) -> {
            try {
                lock.lock();
                this.objectsCollectionCompleted.add(blockHash);
                if (this.objectsHeadersReceived.contains(blockHash)) {
                    this.eventBus.publish(new BigObjectReceivedEvent(blockHash, source));
                    this.objectsHeadersReceived.remove(blockHash);
                    this.objectsCollectionCompleted.remove(blockHash);
                }
            } finally {
                lock.unlock();
            }
        });

        this.itemsReceiver.onItemsReceived((blockHash, chunk, source) -> {
            this.eventBus.publish(new BigObjectItemsReceivedEvent<>(blockHash, chunk.getItems(), source));
        });

        this.itemsReceiver.onSourceChanged((blockHash, source) -> {
            this.eventBus.publish(new BigObjectSourceChangedEvent(blockHash, source));
        });

    }

    @Override
    public void start() {
        if (headersProcessingEnabled) {
            this.headerStore.start();
        }
        this.itemsReceiver.start();
    }

    @Override
    public void stop() {
        if (headersProcessingEnabled) {
            this.headerStore.stop();
        }
        this.itemsReceiver.stop();
    }

    // TODO: We do NO check the SOURCE in this case
    @Override
    public void registerHeader(String objectId, H header, String source) {
        try {
            lock.lock();
            Preconditions.checkState(headersProcessingEnabled, "Headers processing is DISABLED");
            if (headersProcessingEnabled) {
                headerStore.save(objectId, header);
                this.eventBus.publish(new BigObjectHeaderReceivedEvent<>(objectId, header, source));
            }
            this.objectsHeadersReceived.add(objectId);
            if (this.objectsCollectionCompleted.contains(objectId)) {
                this.eventBus.publish(new BigObjectReceivedEvent(objectId, source));
                this.objectsHeadersReceived.remove(objectId);
                this.objectsCollectionCompleted.remove(objectId);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public H getHeader(String objectId) {
        Preconditions.checkState(headersProcessingEnabled, "Headers processing is DISABLED");
        return  headerStore.get(objectId);
    }

    @Override
    public void registerNumTotalItems(String collectionId, long numTotalItems, String source) {
        this.itemsReceiver.registerNumTotalItems(collectionId, numTotalItems, source);
    }

    @Override
    public void registerIncomingItems(String collectionId, BigCollectionChunk<I> chunk, String source) {
        this.itemsReceiver.registerIncomingItems(collectionId, chunk, source);
    }

    @Override
    public boolean isCompleted(String collectionId) {
        return this.itemsReceiver.isCompleted(collectionId);
    }

    @Override
    public boolean contains(String collectionId) {
        return this.itemsReceiver.contains(collectionId);
    }

    @Override
    public long sizeInBytes() {
        return this.itemsReceiver.sizeInBytes(); // We only care about the size of the collections, not the headers
    }

    @Override
    public void onCollectionCompleted(BiConsumer<String, String> callback) {
        this.itemsReceiver.onCollectionCompleted(callback);
    }

    @Override
    public void onItemsReceived(TriConsumer<String, BigCollectionChunk<I>, String> callback) { this.itemsReceiver.onItemsReceived(callback);}

    @Override
    public void onSourceChanged(BiConsumer<String, String> callback) { this.itemsReceiver.onSourceChanged(callback);}

    @Override
    public Iterator<BigCollectionChunk<I>> getCollectionChunks(String collectionId) {
        return this.itemsReceiver.getCollectionChunks(collectionId);
    }

    @Override
    public void remove(String collectionId) {
        if (headersProcessingEnabled) {
            headerStore.remove(collectionId);
        }
        itemsReceiver.remove(collectionId);
    }

    @Override
    public void clear() {
        if (headersProcessingEnabled) {
            headerStore.clear();
        }
        itemsReceiver.clear();
    }

    @Override
    public void destroy() {
        if (headersProcessingEnabled) {
            this.headerStore.destroy();
        }
        itemsReceiver.destroy();
    }

    @Override
    public void compact() {
        if (headersProcessingEnabled) {
            this.headerStore.compact();
        }
        itemsReceiver.compact();
    }

    @Override
    public BigObjectHeaderPlusReceiverEventStreamer EVENTS() {
        return this.EVENTS;
    }
}