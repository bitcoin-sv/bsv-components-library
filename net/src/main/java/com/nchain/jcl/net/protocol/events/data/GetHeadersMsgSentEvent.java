package com.nchain.jcl.net.protocol.events.data;

import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.net.protocol.messages.GetHeadersMsg;
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when a GET_HEADERS Message is sent to a remote Peer.
 */
public final class GetHeadersMsgSentEvent extends MsgSentEvent<GetHeadersMsg> {
    public GetHeadersMsgSentEvent(PeerAddress peerAddress, BitcoinMsg<GetHeadersMsg> btcMsg) {
        super(peerAddress, btcMsg);
    }
}
