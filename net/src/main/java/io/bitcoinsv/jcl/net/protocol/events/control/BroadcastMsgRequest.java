/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.protocol.events.control;

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
public final class BroadcastMsgRequest extends P2PRequest {
    private final BitcoinMsg<?> btcMsg;

    public BroadcastMsgRequest(BitcoinMsg<?> btcMsg) {
        this.btcMsg = btcMsg;
    }

    public BitcoinMsg<?> getBtcMsg() { return this.btcMsg; }

    @Override
    public String toString() {
        return "BroadcastMsgRequest(btcMsg=" + this.getBtcMsg() + ")";
    }
}
