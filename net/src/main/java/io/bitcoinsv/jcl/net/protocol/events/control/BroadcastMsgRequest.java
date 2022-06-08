package io.bitcoinsv.jcl.net.protocol.events.control;

import com.google.common.base.Objects;
import io.bitcoinsv.jcl.net.network.events.P2PRequest;
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsg;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-25 17:04
 *
 * An Event representing a Request to broadcast a Message to all the Peers we are connected to.
 */
public class BroadcastMsgRequest extends P2PRequest {
    private final BitcoinMsg<?> btcMsg;

    public BroadcastMsgRequest(BitcoinMsg<?> btcMsg) {
        this.btcMsg = btcMsg;
    }

    public BitcoinMsg<?> getBtcMsg() { return this.btcMsg; }

    @Override
    public String toString() {
        return "BroadcastMsgRequest(btcMsg=" + this.getBtcMsg() + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) { return false; }
        BroadcastMsgRequest other = (BroadcastMsgRequest) obj;
        return Objects.equal(this.btcMsg, other.btcMsg);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), btcMsg);
    }
}
