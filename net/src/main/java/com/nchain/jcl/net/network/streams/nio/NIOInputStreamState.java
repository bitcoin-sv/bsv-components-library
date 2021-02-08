package com.nchain.jcl.net.network.streams.nio;


import com.nchain.jcl.tools.streams.StreamState;

import java.math.BigInteger;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This class stores the State of a NIOInputStream
 */
public final class NIOInputStreamState extends StreamState {
    // Number of bytes received since the beginning
    private BigInteger numBytesReceived = BigInteger.ZERO;


    public NIOInputStreamState(BigInteger numBytesReceived) {
        if (numBytesReceived != null)
            this.numBytesReceived = numBytesReceived;
    }

    public BigInteger getNumBytesReceived() {
        return this.numBytesReceived;
    }

    @Override
    public String toString() {
        return "NIOInputStreamState(numBytesReceived=" + this.getNumBytesReceived() + ")";
    }

    public NIOInputStreamStateBuilder toBuilder() {
        return new NIOInputStreamStateBuilder().numBytesReceived(this.numBytesReceived);
    }

    public static NIOInputStreamStateBuilder builder() {
        return new NIOInputStreamStateBuilder();
    }

    /**
     * Builder
     */
    public static class NIOInputStreamStateBuilder {
        private BigInteger numBytesReceived;

        NIOInputStreamStateBuilder() { }

        public NIOInputStreamState.NIOInputStreamStateBuilder numBytesReceived(BigInteger numBytesReceived) {
            this.numBytesReceived = numBytesReceived;
            return this;
        }
        public NIOInputStreamState build() { return new NIOInputStreamState(numBytesReceived); }
    }
}
