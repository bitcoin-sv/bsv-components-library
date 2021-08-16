package io.bitcoinsv.jcl.net.protocol.messages;

import io.bitcoinsv.jcl.net.protocol.messages.common.Message;

/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This message consists of only a message header with the command string "mempool".
 */
public final class MemPoolMsg extends Message {

    public static final String MESSAGE_TYPE = "mempool";
    private static final int MESSAGE_LENGTH = 0;

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
    protected void validateMessage() {}

    @Override
    public String toString() {
        return "MemPoolMsg()";
    }

    @Override
    public int hashCode() {
        return 1;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) { return false; }
        if (obj == this) { return true; }
        return true;
    }

    public static MemPoolMsgBuilder builder() {
        return new MemPoolMsgBuilder();
    }

    /**
     * Builder
     */
    public static class MemPoolMsgBuilder {
        MemPoolMsgBuilder() {}

        public MemPoolMsg build() {
            return new MemPoolMsg();
        }
    }
}
