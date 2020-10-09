package com.nchain.jcl.net.protocol.messages;


import com.nchain.jcl.net.protocol.messages.common.Message;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * The ping message is sent primarily to confirm that the TCP/IP connection is still valid.
 * An error in transmission is presumed to be a closed connection and the address is removed as a current peer.
 *
 *  Structure of the BODY of Message:
 *
 * - field: "nonce" (8 bytes) uint64_t
 *   Random nonce assigned to this pong message.
 *   The pong message sends back the same nonce received in the ping message it is replying to.
 */
@Value
@EqualsAndHashCode
public class PongMsg extends Message {
    protected static int FIXED_MESSAGE_LENGTH = 8;
    public static final String MESSAGE_TYPE = "pong";

    private long nonce;

    @Builder
    public PongMsg(long nonce) {
        this.nonce = nonce;
        init();
    }


    @Override
    protected long calculateLength() {
        long length = FIXED_MESSAGE_LENGTH;
        return length;
    }

    @Override
    protected void validateMessage() {}

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }
}
