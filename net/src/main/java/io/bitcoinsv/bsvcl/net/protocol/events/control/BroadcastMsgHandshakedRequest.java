package io.bitcoinsv.bsvcl.net.protocol.events.control;

import io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsg;

import java.util.Objects;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-25 17:04
 *
 * An Event representing a Request to broadcast a Message to all the Peers we are connected to.
 *
 * THE MESSAGE WILL ONLY BE SENT IF THE PEER IS HANDSHAKED
 */
public final class BroadcastMsgHandshakedRequest extends BroadcastMsgRequest {

    public BroadcastMsgHandshakedRequest(BitcoinMsg<?> btcMsg) {
        super(btcMsg);
    }

    @Override
    public String toString() {
        return "BroadcastMsgHandhsakedRequest[" + super.toString() + "]";
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
