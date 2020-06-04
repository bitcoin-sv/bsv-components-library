package com.nchain.jcl.protocol.messages;

import com.nchain.jcl.protocol.messages.common.Message;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;


/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 19/07/2019 10:53
 *
 *
 * The ping message is sent primarily to confirm that the TCP/IP connection is still valid. An error in transmission is
 * presumed to be a closed connection and the address is removed as a current peer.The response to a ping message
 * is the pong message.
 *
 * * Structure of the BODY of Message:
 *
 * - field: "nonce" (8 bytes) uint64_t
 *   Random nonce assigned to this ping message. The responding pong message will include this nonce to
 *   identify the ping message to which it is replying.
 *
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class PingMsg extends Message {
    public static final String MESSAGE_TYPE = "ping";
    protected static int FIXED_MESSAGE_LENGTH = 8;

    private long nonce;

    @Builder
    protected PingMsg(long nonce) {
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
