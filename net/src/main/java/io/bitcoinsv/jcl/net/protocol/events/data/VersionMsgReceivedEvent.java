package io.bitcoinsv.jcl.net.protocol.events.data;


import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.protocol.messages.VersionMsg;
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsg;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when a VERSION Message is received from a Remote Peer.
 */
public final class VersionMsgReceivedEvent extends MsgReceivedEvent<VersionMsg> {
    public VersionMsgReceivedEvent(PeerAddress peerAddress, BitcoinMsg<VersionMsg> btcMsg) {
        super(peerAddress, btcMsg);
    }
}
