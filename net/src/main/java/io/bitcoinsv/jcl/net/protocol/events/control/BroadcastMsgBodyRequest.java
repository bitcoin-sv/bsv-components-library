package io.bitcoinsv.jcl.net.protocol.events.control;

import com.google.common.base.Objects;
import io.bitcoinsv.jcl.net.network.events.P2PRequest;
import io.bitcoinsv.jcl.net.protocol.messages.common.BodyMessage;

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
public class BroadcastMsgBodyRequest extends P2PRequest {
    private final BodyMessage body;

    public BroadcastMsgBodyRequest(BodyMessage body) {
        this.body = body;
    }

    public BodyMessage getMsgBody() { return this.body; }

    @Override
    public String toString() {
        return "BroadcastMsgBodyRequest(msgBody=" + this.body + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) { return false;}
        BroadcastMsgBodyRequest other = (BroadcastMsgBodyRequest) obj;
        return Objects.equal(this.body, other.body);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), body);
    }

}
