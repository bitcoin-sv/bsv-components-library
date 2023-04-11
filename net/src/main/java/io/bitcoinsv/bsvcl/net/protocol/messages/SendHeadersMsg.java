package io.bitcoinsv.bsvcl.net.protocol.messages;

import io.bitcoinsv.bsvcl.net.protocol.messages.common.BodyMessage;

import java.io.Serializable;

/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This message consists of only a message header with the command string "sendheaders".
 */
public final class SendHeadersMsg extends BodyMessage implements Serializable {

    public static final String MESSAGE_TYPE = "sendheaders";
    private static final int MESSAGE_LENGTH = 0;

    public SendHeadersMsg(byte[] extraBytes, long checksum){
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
    protected void validateMessage() {
    }

    public String toString() {
        return "SendHeadersMsg()";
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    public static SendHeadersMsgBuilder builder() {
        return new SendHeadersMsgBuilder();
    }

    @Override
    public SendHeadersMsgBuilder toBuilder() {
        return new SendHeadersMsgBuilder(super.extraBytes, super.checksum);
    }

    /**
     * Builder
     */
    public static class SendHeadersMsgBuilder extends BodyMessageBuilder {
        public SendHeadersMsgBuilder() { }
        public SendHeadersMsgBuilder(byte[] extraBytes, long checksum) { super(extraBytes, checksum);}

        public SendHeadersMsg build() {
            return new SendHeadersMsg(super.extraBytes, super.checksum);
        }
    }
}
