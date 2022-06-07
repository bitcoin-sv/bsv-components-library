package io.bitcoinsv.jcl.net.protocol.messages;

import io.bitcoinsv.jcl.net.protocol.messages.common.BodyMessage;

import java.io.Serializable;

/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This message consists of only a message header with the command string "mempool".
 */
public final class MemPoolMsg extends BodyMessage implements Serializable {

    public static final String MESSAGE_TYPE = "mempool";
    private static final int MESSAGE_LENGTH = 0;

    public MemPoolMsg(byte[] extraBytes, long checksum){
        super(extraBytes, checksum);
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
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    public static MemPoolMsgBuilder builder() {
        return new MemPoolMsgBuilder();
    }

    @Override
    public MemPoolMsgBuilder toBuilder() {
        return new MemPoolMsgBuilder(super.extraBytes, super.checksum);
    }

    /**
     * Builder
     */
    public static class MemPoolMsgBuilder extends BodyMessageBuilder {
        public MemPoolMsgBuilder() {}
        public MemPoolMsgBuilder(byte[] extraBuild, long checksum) { super(extraBuild, checksum);}

        public MemPoolMsg build() {
            return new MemPoolMsg(super.extraBytes, super.checksum);
        }
    }
}
