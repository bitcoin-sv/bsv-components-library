/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.protocol.messages;


import io.bitcoinsv.jcl.net.protocol.messages.common.Message;

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * The getaddr message sends a request to a node asking for information about known active peers to help with
 * finding potential nodes in the network. This message consists of only a message  header with the command  "getaddr".
 */
public final class GetAddrMsg extends Message {

    // Message Type (stored in the "command" field in the HeaderMsg of a Bitcoin Message
    public static final String MESSAGE_TYPE = "getaddr";
    // The  GetAddr is an empty message
    private static final int MESSAGE_LENGTH = 0;

   protected GetAddrMsg() { init();}

    @Override
    public String getMessageType() { return MESSAGE_TYPE; }

    @Override
    protected long calculateLength() {
       long length = MESSAGE_LENGTH;
       return length;
   }

    @Override
    protected void validateMessage() {}

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

    /**
     * Builder
     */
    public static class GetAddrMsgBuilder {
        GetAddrMsgBuilder() {}
        public GetAddrMsg build() {
            return new GetAddrMsg();
        }
    }
}
