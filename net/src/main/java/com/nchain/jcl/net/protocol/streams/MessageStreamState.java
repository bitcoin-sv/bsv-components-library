package com.nchain.jcl.net.protocol.streams;


import com.google.common.base.Objects;
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg;
import com.nchain.jcl.net.protocol.streams.deserializer.DeserializerStreamState;
import com.nchain.jcl.net.protocol.streams.serializer.SerializerStreamState;
import com.nchain.jcl.tools.streams.StreamState;

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
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!(obj instanceof MessageStreamState)) return false;
        MessageStreamState other = (MessageStreamState) obj;
        return Objects.equal(this.inputState, other.inputState) && Objects.equal(this.outputState, other.outputState);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.inputState, this.outputState);
    }

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
