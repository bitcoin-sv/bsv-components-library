package com.nchain.jcl.net.protocol.events.data;

import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.net.protocol.messages.InvMessage;
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg;

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
