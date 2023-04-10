package io.bitcoinsv.bsvcl.tools.bigObjects.receivers;


import io.bitcoinsv.bsvcl.tools.bigObjects.BigCollectionChunk;
import io.bitcoinsv.bsvcl.tools.util.TriConsumer;

import java.util.Iterator;
import java.util.function.BiConsumer;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A Collection Receiver is a component that can "receive" big Collections from different sources, it stores these
 * collections internally and it can also notify when the collection has been completely received, offering access to
 * that collection (via Iterator) and other operations.
 *
 * Since these are "Big" collections, this component will receive them in "Chunks", each one belonging to one specific
 * COLLECTION (defined by a "collectionId"), with an ORDINAL (to keep track of the order) and a SOURCE (which
 * specfies WHO is sending that Collection). This SOURCE might be the name of another component, an IP address, etc.
 *
 * All the Collections are stored until they are removed by the user of this Component. They can also be recovered
 * by using an Iterator over the Chunks stored for each Collection. This Component is supposed to keep track of the
 * different incoming chunks, and to perform some checks like:
 *  - If all the Items have been received, it notifies that by callback
 *  - If some chunks are received for the a collection coming from a DIFFERENT source than others from which we already
 *    received some, this component will assume that something has gone wrong with the First source, so it will remove
 *    the collection completey and will start over with the new Source.
 *
 * general Rules:
 * - You need to invoke "start()" before you can use it, and you call "stop()" when you are done.
 * - The "destroy" method releases the resources used by the Store.
 * - If you want to keep using the store after invoking "stop()" or "destroy()" you need to call "start()" again.
 *
 * @param <I> Class of each Item of the Collection
 */
public interface BigCollectionReceiver<I> {
    // Start/Stop
    void start();
    void stop();

    // To invoke when we receive Chunks/Info from the sources
    void registerNumTotalItems(String collectionId, long numTotalItems, String source);
    void registerIncomingItems(String collectionId, BigCollectionChunk<I> chunk, String source);

    // Indicates if the given collection has been received completely:
    boolean contains(String collectionId);
    boolean isCompleted(String collectionId);

    // Return info abut the Collections being saved:
    long sizeInBytes();
    Iterator<BigCollectionChunk<I>> getCollectionChunks(String collectionId);
    void remove(String collectionId);

    // to empty the Receiver and free resources
    void clear();
    void destroy();

    // Callbacks to notify the client of this Component:
    void onCollectionCompleted(BiConsumer<String, String> callback);
    void onItemsReceived(TriConsumer<String, BigCollectionChunk<I>, String> callback);
    void onSourceChanged(BiConsumer<String, String> callback);

    // for performance/cleaning operations, implementation dependent:
    default void compact() {}
}