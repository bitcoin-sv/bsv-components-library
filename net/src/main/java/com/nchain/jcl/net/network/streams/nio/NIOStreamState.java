package com.nchain.jcl.net.network.streams.nio;



import com.nchain.jcl.net.network.streams.StreamState;
import java.math.BigInteger;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This class stores the state of the NIOOutputStreamState.
 */
public final class NIOStreamState extends StreamState {
    // Number of bytes sent through this stream since the beginning
    private BigInteger numBytesProcessed = BigInteger.ZERO;

    public NIOStreamState(BigInteger numBytesSent) {
        if (numBytesSent != null)
            this.numBytesProcessed = numBytesSent;
    }

    public BigInteger getNumBytesProcessed() {
        return this.numBytesProcessed;
    }

    @Override
    public String toString() {
        return "NIOOutputStreamState(numBytesProcessed=" + this.getNumBytesProcessed() + ")";
    }

    public static NIOOutputStreamStateBuilder builder() {
        return new NIOOutputStreamStateBuilder();
    }

    public NIOOutputStreamStateBuilder toBuilder() {
        return new NIOOutputStreamStateBuilder().numBytesProcessed(this.numBytesProcessed);
    }

    /**
     * Builder
     */
    public static class NIOOutputStreamStateBuilder {
        private BigInteger numBytesProcessed;

        NIOOutputStreamStateBuilder() { }

        public NIOStreamState.NIOOutputStreamStateBuilder numBytesProcessed(BigInteger numBytesProcessed) {
            this.numBytesProcessed = numBytesProcessed;
            return this;
        }
        public NIOStreamState build() { return new NIOStreamState(numBytesProcessed); }
    }
}
