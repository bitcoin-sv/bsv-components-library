package com.nchain.jcl.net.protocol.messages;

import com.nchain.jcl.net.protocol.messages.common.Message;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
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
        init();
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
