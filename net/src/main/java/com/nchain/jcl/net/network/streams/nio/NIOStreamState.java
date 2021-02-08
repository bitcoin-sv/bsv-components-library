package com.nchain.jcl.net.network.streams.nio;


import com.nchain.jcl.tools.streams.StreamState;

/**
 * @author i.¡fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This class stores the State of a NIOStream. It's just a placeholder for the States of both the
 * input and the output channels of the Stream.
 */
public final class NIOStreamState extends StreamState {
    private final NIOInputStreamState inputState;
    private final NIOOutputStreamState outputState;

    NIOStreamState(NIOInputStreamState inputState, NIOOutputStreamState outputState) {
        this.inputState = inputState;
        this.outputState = outputState;
    }

    public NIOInputStreamState getInputState()      { return this.inputState; }
    public NIOOutputStreamState getOutputState()    { return this.outputState; }

    @Override
    public String toString() {
        return "NIOStreamState(inputState=" + this.getInputState() + ", outputState=" + this.getOutputState() + ")";
    }

    public static NIOStreamStateBuilder builder() {
        return new NIOStreamStateBuilder();
    }

    public NIOStreamStateBuilder toBuilder() {
        return new NIOStreamStateBuilder().inputState(this.inputState).outputState(this.outputState);
    }

    /**
     * Builder
     */
    public static class NIOStreamStateBuilder {
        private NIOInputStreamState inputState;
        private NIOOutputStreamState outputState;

        NIOStreamStateBuilder() { }

        public NIOStreamState.NIOStreamStateBuilder inputState(NIOInputStreamState inputState) {
            this.inputState = inputState;
            return this;
        }

        public NIOStreamState.NIOStreamStateBuilder outputState(NIOOutputStreamState outputState) {
            this.outputState = outputState;
            return this;
        }
        public NIOStreamState build() { return new NIOStreamState(inputState, outputState); }
    }
}
