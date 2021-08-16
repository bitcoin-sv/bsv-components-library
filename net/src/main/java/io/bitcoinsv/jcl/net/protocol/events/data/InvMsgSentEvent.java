/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.protocol.events.data;

import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.protocol.messages.InvMessage;
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsg;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when a INV Message is sent to a remote Peer.
 */
public final class InvMsgSentEvent extends MsgSentEvent<InvMessage> {
    public InvMsgSentEvent(PeerAddress peerAddress, BitcoinMsg<InvMessage> btcMsg) {
        super(peerAddress, btcMsg);
    }
}
