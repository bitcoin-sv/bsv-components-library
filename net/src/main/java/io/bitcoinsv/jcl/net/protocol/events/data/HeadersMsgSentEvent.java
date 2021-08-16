/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.protocol.events.data;

import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.protocol.messages.HeadersMsg;
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsg;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when a HEADERS Message is sent to a remote Peer.
 */
public final class HeadersMsgSentEvent extends MsgSentEvent<HeadersMsg> {
    public HeadersMsgSentEvent(PeerAddress peerAddress, BitcoinMsg<HeadersMsg> btcMsg) {
        super(peerAddress, btcMsg);
    }
}
