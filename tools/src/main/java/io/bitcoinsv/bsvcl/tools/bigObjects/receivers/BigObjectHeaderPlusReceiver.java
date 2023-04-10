package io.bitcoinsv.bsvcl.tools.bigObjects.receivers;


import io.bitcoinsv.bsvcl.tools.bigObjects.stores.ObjectStore;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This component combines the Functionality of 2 Components:
 * - ObjectStore
 * - BigCollectionReceiver
 *
 * It works as a "Big Object Receiver": A "Big Object" is an object so big that it cannot be held entirely into memory.
 * In this case, the Big object is made up of 2 main parts:
 *  - a Header: A small object, the Big Object only has one of these
 *  - a Big Collection: A colection of items too long to hold in memory.
 *
 *  Examples of such objcts are Blocks (Header + list of Txs), but also Compact blocks.
 *
 * This component is a "receiver" that can receive the HEADER of each Object and also a Big Collection of Items of each
 * object, in this case the Collectins are broken down into smaller "Chunks".
 *
 * @see ObjectStore
 * @see BigCollectionReceiver
 *
 * @param <H> Type of the HEADER part of this Object
 * @param <I> Type of each Item in the BIG COLLECTION part of this object.
 */
public interface BigObjectHeaderPlusReceiver<H,I> extends BigCollectionReceiver<I>{
    void registerHeader(String objectId, H header, String source);
    H getHeader(String objectId);

    BigObjectHeaderPlusReceiverEventStreamer EVENTS();
}