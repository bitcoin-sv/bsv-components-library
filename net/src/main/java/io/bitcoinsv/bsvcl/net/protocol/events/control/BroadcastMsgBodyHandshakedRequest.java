package io.bitcoinsv.bsvcl.net.protocol.events.control;

import com.google.common.base.Objects;
import io.bitcoinsv.bsvcl.net.protocol.messages.common.BodyMessage;

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
 *
 * THE MESSAGE WILL ONLY BE SENT IF THE PEER IS HANDSHAKED
 */
public final class BroadcastMsgBodyHandshakedRequest extends BroadcastMsgBodyRequest {

    public BroadcastMsgBodyHandshakedRequest(BodyMessage body) {
        super(body);
    }

    @Override
    public String toString() {
        return "BroadcastMsgBodyHandshakedRequest[" + super.toString() + "])";
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode());
    }
}
