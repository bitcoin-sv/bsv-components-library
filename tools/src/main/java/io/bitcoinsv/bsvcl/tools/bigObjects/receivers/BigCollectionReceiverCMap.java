package io.bitcoinsv.bsvcl.tools.bigObjects.receivers;

import io.bitcoinsv.bsvcl.tools.bigObjects.stores.BigCollectionChunksStoreCMap;
import io.bitcoinsv.bsvcl.tools.bigObjects.stores.ObjectSerializer;
import io.bitcoinsv.bsvcl.tools.config.RuntimeConfig;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An implementation of BigCollectionReceiver that uses internally ChornicleMap to store the Colections.
 *
 */
public class BigCollectionReceiverCMap<I> extends BigCollectionReceiverImpl<I> implements BigCollectionReceiver<I> {

    /**
     * Constructor.
     * @param runtimeConfig             JCL Runtime Config
     * @param receiverId                Used to create the Folder where the Files will be saved
     * @param itemSerializer            Serializer of the Collection Items
     * @param avgCollectionIdSize       Avg Key Size (it must be as long as the Collection Id at least)
     * @param maxCollections            Maximum number of Collections to save
     * @param avgItemSize               Avg Size of each Item
     * @param maxItemsEachFile          MAx number of items in each CMap
     */
    public BigCollectionReceiverCMap(RuntimeConfig runtimeConfig,
                                     String receiverId,
                                     ObjectSerializer<I> itemSerializer,
                                     long avgCollectionIdSize,
                                     long maxCollections,
                                     long avgItemSize,
                                     long maxItemsEachFile) {

        super(
                runtimeConfig,
                receiverId,
                itemSerializer,
                new BigCollectionChunksStoreCMap<>(
                        runtimeConfig,
                        receiverId,
                        itemSerializer,
                        avgCollectionIdSize,
                        maxCollections,
                        avgItemSize,
                        maxItemsEachFile));

    }
}