package com.nchain.jcl.protocol.messages;

import com.nchain.jcl.protocol.messages.common.Message;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 30/07/2020
 *
 * This message consists of only a message header with the command string "mempool".
 */
@Value
@EqualsAndHashCode
public class MemPoolMsg extends Message {

    public static final String MESSAGE_TYPE = "mempool";
    private static final int MESSAGE_LENGTH = 0;

    @Builder
    public MemPoolMsg(){

    }

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }

    @Override
    protected long calculateLength() {
        return MESSAGE_LENGTH;
    }

    @Override
    protected void validateMessage() {

    }
}
