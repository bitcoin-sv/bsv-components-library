package com.nchain.jcl.net.protocol.messages;


import com.nchain.jcl.net.protocol.messages.common.Message;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * The getaddr message sends a request to a node asking for information about known active peers to help with
 * finding potential nodes in the network. This message consists of only a message  header with the command  "getaddr".
 */
@Value
@EqualsAndHashCode
public final class GetAddrMsg extends Message {

    // Message Type (stored in the "command" field in the HeaderMsg of a Bitcoin Message
    public static final String MESSAGE_TYPE = "getaddr";
    // The  GetAddr is an empty message
    private static final int MESSAGE_LENGTH = 0;

   @Builder
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

}
