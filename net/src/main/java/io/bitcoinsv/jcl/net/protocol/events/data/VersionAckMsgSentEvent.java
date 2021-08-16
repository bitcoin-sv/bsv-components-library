package io.bitcoinsv.jcl.net.protocol.events.data;

import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.protocol.messages.VersionAckMsg;
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsg;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when a VERSION_ACK Message is sent to a remote Peer.
 */
public final class VersionAckMsgSentEvent extends MsgSentEvent<VersionAckMsg> {
    public VersionAckMsgSentEvent(PeerAddress peerAddress, BitcoinMsg<VersionAckMsg> btcMsg) {
        super(peerAddress, btcMsg);
    }
}
