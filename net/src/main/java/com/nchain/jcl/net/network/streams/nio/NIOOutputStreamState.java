package com.nchain.jcl.net.network.streams.nio;


import com.nchain.jcl.tools.streams.StreamState;

import java.math.BigInteger;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This class stores the state of the NIOOutputStreamState.
 */
public final class NIOOutputStreamState extends StreamState {
    // Number of bytes sent through this stream since the beginning
    private BigInteger numBytesSent = BigInteger.ZERO;

    public NIOOutputStreamState(BigInteger numBytesSent) {
        if (numBytesSent != null)
            this.numBytesSent = numBytesSent;
    }

    public BigInteger getNumBytesSent() {
        return this.numBytesSent;
    }

    @Override
    public String toString() {
        return "NIOOutputStreamState(numBytesSent=" + this.getNumBytesSent() + ")";
    }

    public static NIOOutputStreamStateBuilder builder() {
        return new NIOOutputStreamStateBuilder();
    }

    public NIOOutputStreamStateBuilder toBuilder() {
        return new NIOOutputStreamStateBuilder().numBytesSent(this.numBytesSent);
    }

    /**
     * Builder
     */
    public static class NIOOutputStreamStateBuilder {
        private BigInteger numBytesSent;

        NIOOutputStreamStateBuilder() { }

        public NIOOutputStreamState.NIOOutputStreamStateBuilder numBytesSent(BigInteger numBytesSent) {
            this.numBytesSent = numBytesSent;
            return this;
        }
        public NIOOutputStreamState build() { return new NIOOutputStreamState(numBytesSent); }
    }
}
