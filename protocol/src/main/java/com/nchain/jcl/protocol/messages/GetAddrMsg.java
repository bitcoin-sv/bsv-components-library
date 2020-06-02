package com.nchain.jcl.protocol.messages;


import com.nchain.jcl.protocol.messages.common.Message;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2009-2010 Satoshi Nakamoto
 * Copyright (c) 2009-2016 The Bitcoin Core developers
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 07/08/2019 10:14
 *
 * The getaddr message sends a request to a node asking for information about known active peers to help with
 * finding potential nodes in the network. This message consists of only a message  header with the command  "getaddr".
 */
@Value
@EqualsAndHashCode(callSuper = true)
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
