package io.bitcoinsv.bsvcl.tools.bigObjects;

import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This class represents a "Chunk" of a Collection, which is a partial piece of it.
 * For example, a collection of 1000 Items can be broken down into 10 Chunks of 100 items each, or 50 chunks
 * of 200 items each. Each chunk has an ordinal so we can keep track of the order of the items.
 *
 * This Class is meant to be used for those collections so large that its not possible to keep them in memory.
 */
public interface BigCollectionChunk<I> {
    List<I> getItems();
    int getChunkOrdinal();
}