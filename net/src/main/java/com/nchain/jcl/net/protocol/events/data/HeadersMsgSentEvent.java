package com.nchain.jcl.net.protocol.events.data;

import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.net.protocol.messages.HeadersMsg;
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg;

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
