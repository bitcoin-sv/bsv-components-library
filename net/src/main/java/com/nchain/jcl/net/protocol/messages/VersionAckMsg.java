package com.nchain.jcl.net.protocol.messages;


import com.nchain.jcl.net.protocol.messages.common.Message;
import lombok.Builder;
import lombok.EqualsAndHashCode;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 *
 * The verack message is sent in reply to version. This message consists of only a message
 * header with the command string "verack".
 *
 */
@EqualsAndHashCode
public final class VersionAckMsg extends Message {

    // Message Type (stored in the "command" field in the HeaderMsg of a Bitcoin Message
    public static final String MESSAGE_TYPE = "verack";
    private static final int MESSAGE_LENGTH = 0;

    @Builder
    protected VersionAckMsg() {init(); }

    @Override
    public String getMessageType() { return MESSAGE_TYPE; }

    @Override
    public String toString() {
        return "[VersionAck| empty message" + "]";
    }

    @Override
    public boolean equals(Object obj) {
        return true; // Since all VerackMsg are empty, they are ALWAYS Equals
    }

    @Override
    public int hashCode() {
        return 0; // TODO: Careful
    }


    @Override
    protected long calculateLength() {
        long length = MESSAGE_LENGTH;
        return length;
    }

    @Override
    protected void validateMessage() {}
}
