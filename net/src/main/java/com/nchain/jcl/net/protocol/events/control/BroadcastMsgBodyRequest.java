package com.nchain.jcl.net.protocol.events.control;

import com.nchain.jcl.net.network.events.P2PRequest;
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg;
import com.nchain.jcl.net.protocol.messages.common.Message;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-25 17:04
 *
 * An Event representing a Request to broadcast a Message to all the Peers we are connected to.
 *
 * Unlike the BroadcastMsgRequest, this Request does not specify the Header of the Message, only the Body. The Header will
 * be automatically created at the moment of sending the mesage...
 */
public final class BroadcastMsgBodyRequest extends P2PRequest {
    private final Message body;

    public BroadcastMsgBodyRequest(Message body) {
        this.body = body;
    }

    public Message getMsgBody() { return this.body; }

    @Override
    public String toString() {
        return "BroadcastMsgBodyRequest(msgBody=" + this.body + ")";
    }
}
