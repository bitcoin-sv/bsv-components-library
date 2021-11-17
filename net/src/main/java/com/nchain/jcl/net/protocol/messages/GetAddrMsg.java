package com.nchain.jcl.net.protocol.messages;


import com.google.common.base.Objects;
import com.nchain.jcl.net.protocol.messages.common.Message;

import java.io.Serializable;

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * The getaddr message sends a request to a node asking for information about known active peers to help with
 * finding potential nodes in the network. This message consists of only a message  header with the command  "getaddr".
 */
public final class GetAddrMsg extends Message implements Serializable {

    // Message Type (stored in the "command" field in the HeaderMsg of a Bitcoin Message
    public static final String MESSAGE_TYPE = "getaddr";

    // The  GetAddr is an empty message
    private static final int MESSAGE_LENGTH = 0;

   protected GetAddrMsg(long payloadChecksum) {
       super(payloadChecksum);
       init();
   }

    @Override
    public String getMessageType() { return MESSAGE_TYPE; }

    @Override
    protected long calculateLength() {
       long length = MESSAGE_LENGTH;
       return length;
   }

    @Override
    protected void validateMessage() {}

    @Override
    public String toString() {
        return "GetAddrMsg()";
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

    public static GetAddrMsgBuilder builder() {
        return new GetAddrMsgBuilder();
    }

    @Override
    public GetAddrMsgBuilder toBuilder() {
       return new GetAddrMsgBuilder();
    }

    /**
     * Builder
     */
    public static class GetAddrMsgBuilder extends MessageBuilder{
        GetAddrMsgBuilder() {}
        public GetAddrMsg build() {
            return new GetAddrMsg(super.payloadChecksum);
        }
    }
}
