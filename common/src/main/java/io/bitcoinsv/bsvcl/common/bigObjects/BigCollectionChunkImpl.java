package io.bitcoinsv.bsvcl.common.bigObjects;

import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Baic Implementation of BigCollectionChunk
 */
public class BigCollectionChunkImpl<I> implements BigCollectionChunk<I> {
    private List<I> items;
    private int chunkOrdinal;

    public BigCollectionChunkImpl(List<I> items, int chunkOrdinal) {
        this.items = items;
        this.chunkOrdinal = chunkOrdinal;
    }

    @Override
    public List<I> getItems() {
        return this.items;
    }

    @Override
    public int getChunkOrdinal() {
        return this.chunkOrdinal;
    }
}