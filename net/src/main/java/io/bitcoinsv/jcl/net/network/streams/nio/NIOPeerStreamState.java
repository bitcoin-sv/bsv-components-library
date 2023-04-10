package io.bitcoinsv.jcl.net.network.streams.nio;

import io.bitcoinsv.jcl.net.network.streams.StreamState;

/**
 * @author i.¡fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This class stores the State of a NIOStream. It's just a placeholder for the States of both the
 * input and the output channels of the Stream.
 */
public final class NIOPeerStreamState extends StreamState {
    private final NIOStreamState inputState;
    private final NIOStreamState outputState;

    NIOPeerStreamState(NIOStreamState inputState, NIOStreamState outputState) {
        this.inputState = inputState;
        this.outputState = outputState;
    }

    public NIOStreamState getInputState()      { return this.inputState; }
    public NIOStreamState getOutputState()    { return this.outputState; }

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
        private NIOStreamState inputState;
        private NIOStreamState outputState;

        NIOStreamStateBuilder() { }

        public NIOPeerStreamState.NIOStreamStateBuilder inputState(NIOStreamState inputState) {
            this.inputState = inputState;
            return this;
        }

        public NIOPeerStreamState.NIOStreamStateBuilder outputState(NIOStreamState outputState) {
            this.outputState = outputState;
            return this;
        }
        public NIOPeerStreamState build() { return new NIOPeerStreamState(inputState, outputState); }
    }
}