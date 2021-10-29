package com.nchain.jcl.net.protocol.events.data;

import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.net.protocol.messages.VersionAckMsg;
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg;

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