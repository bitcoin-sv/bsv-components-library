package com.nchain.jcl.net.protocol.streams;


import com.nchain.jcl.net.network.streams.StreamState;
import com.nchain.jcl.net.protocol.streams.deserializer.DeserializerStreamState;
import com.nchain.jcl.net.protocol.streams.serializer.SerializerStreamState;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * It stores the State of a MessageStream. It's just a placeholder for the States of both the
 * Input and the Output of a MessageStream.
 */
public final class MessageStreamState extends StreamState {
    private final DeserializerStreamState inputState;
    private final SerializerStreamState outputState;

    MessageStreamState(DeserializerStreamState inputState, SerializerStreamState outputState) {
        this.inputState = inputState;
        this.outputState = outputState;
    }

    public DeserializerStreamState getInputState()  { return this.inputState; }
    public SerializerStreamState getOutputState()   { return this.outputState; }


    @Override
    public String toString() {
        return "MessageStreamState(inputState=" + this.getInputState() + ", outputState=" + this.getOutputState() + ")";
    }


    public static MessageStreamStateBuilder builder() {
        return new MessageStreamStateBuilder();
    }

    public MessageStreamStateBuilder toBuilder() {
        return new MessageStreamStateBuilder().inputState(this.inputState).outputState(this.outputState);
    }

    /**
     * Builder
     */
    public static class MessageStreamStateBuilder {
        private DeserializerStreamState inputState;
        private SerializerStreamState outputState;

        MessageStreamStateBuilder() {}

        public MessageStreamState.MessageStreamStateBuilder inputState(DeserializerStreamState inputState) {
            this.inputState = inputState;
            return this;
        }

        public MessageStreamState.MessageStreamStateBuilder outputState(SerializerStreamState outputState) {
            this.outputState = outputState;
            return this;
        }

        public MessageStreamState build() {
            return new MessageStreamState(inputState, outputState);
        }
    }
}
