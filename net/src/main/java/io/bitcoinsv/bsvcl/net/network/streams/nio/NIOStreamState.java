package io.bitcoinsv.bsvcl.net.network.streams.nio;


import io.bitcoinsv.bsvcl.net.network.streams.StreamState;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * <p>
 * This class stores the state of the NIOOutputStreamState.
 */
public final class NIOStreamState extends StreamState {
    // Number of bytes sent through this stream since the beginning
    private final AtomicLong numBytesProcessed = new AtomicLong();

    public long getNumBytesProcessed() {
        return this.numBytesProcessed.get();
    }

    public long increment(long numBytesProcessed) {
        return this.numBytesProcessed.addAndGet(numBytesProcessed);
    }

    @Override
    public String toString() {
        return "NIOStreamState(numBytesProcessed=" + this.getNumBytesProcessed() + ")";
    }
}