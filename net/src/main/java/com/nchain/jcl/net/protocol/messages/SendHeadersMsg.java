package com.nchain.jcl.net.protocol.messages;

import com.nchain.jcl.net.protocol.messages.common.BodyMessage;
import com.nchain.jcl.net.protocol.messages.common.Message;

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
        return 1;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) { return false; }
        if (obj == this) { return true; }
        if (obj.getClass() != getClass()) { return false; }
        return true;
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
