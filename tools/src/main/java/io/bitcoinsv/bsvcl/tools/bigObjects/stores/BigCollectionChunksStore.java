package io.bitcoinsv.bsvcl.tools.bigObjects.stores;


import io.bitcoinsv.bsvcl.tools.bigObjects.BigCollectionChunk;

import java.util.Iterator;
import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This store allows for storing Big Collections. A "Big" collection is such a Collection that its not possible
 * to hold it all in memory, so instead its broken down into smaller "chunks". These "chunks" can then be
 * saved/read/removed in a store. This store works with each "chunk" as an individual unit so it saves complete "chunks"
 * and also read complete "chunks". If you need to loop/iterate over individual items within the Chunks, you need to
 * develop that logic yourself (but its quite straightforWard)
 *
 * Each Collection has a identifier (String), and it might have any number of chunks.
 * This store also keeps track of the size (in bytes) that each collection takes and the whole store as a whole as well.
 *
 * general Rules:
 * - You need to invoke "start()" before you can use it, and you call "stop()" when you are done.
 * - The "destroy" method releases the resources used by the Store.
 * - If you want to keep using the store after invoking "stop()" or "destroy()" you need to call "start()" again.
 *
 * @param <I> Type of each Item of the Collection.
 *
 * @see BigCollectionChunk
 */
public interface BigCollectionChunksStore<I> {
    void start();
    void stop();
    boolean save(String collectionId, BigCollectionChunk<I> chunk);
    void remove(String collectionId);
    void registerAsCompleted(String collectionId);
    boolean isCompleted(String collectionId);
    boolean contains(String collectionId);
    Iterator<BigCollectionChunk<I>> getChunks(String collectionId);
    long size(String collectionId);
    long sizeInBytes(String collectionId);
    long sizeInBytes();
    List<String> getCollectionsIds();
    void clear();
    void destroy();
    // for performance/cleaning operations, implementation dependent:
    default void compact() {}
}