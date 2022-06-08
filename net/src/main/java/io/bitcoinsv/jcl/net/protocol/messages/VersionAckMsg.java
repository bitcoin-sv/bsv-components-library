package io.bitcoinsv.jcl.net.protocol.messages;

import io.bitcoinsv.jcl.net.protocol.messages.common.BodyMessage;

import java.io.Serializable;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 *
 * The verack message is sent in reply to version. This message consists of only a message
 * header with the command string "verack".
 *
 */
public final class VersionAckMsg extends BodyMessage implements Serializable {

    // Message Type (stored in the "command" field in the HeaderMsg of a Bitcoin Message
    public static final String MESSAGE_TYPE = "verack";
    private static final int MESSAGE_LENGTH = 0;

    protected VersionAckMsg(byte[] extraBytes, long checksum) {
        super(extraBytes, checksum);
        init();
    }

    @Override
    public String getMessageType() { return MESSAGE_TYPE; }

    @Override
    public String toString() {
        return "[VersionAck| empty message" + "]";
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    protected long calculateLength() {
        long length = MESSAGE_LENGTH;
        return length;
    }

    @Override
    protected void validateMessage() {}

    public static VersionAckMsgBuilder builder() {
        return new VersionAckMsgBuilder();
    }

    @Override
    public VersionAckMsgBuilder toBuilder() {
        return new VersionAckMsgBuilder(super.extraBytes, super.checksum);
    }

    /**
     * Builder
     */
    public static class VersionAckMsgBuilder extends BodyMessageBuilder {
        public VersionAckMsgBuilder() {}
        public VersionAckMsgBuilder(byte[] extraBytes, long checksum) { super(extraBytes, checksum);}

        public VersionAckMsg build() {
            return new VersionAckMsg(super.extraBytes, super.checksum);
        }
    }
}
