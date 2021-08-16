package io.bitcoinsv.jcl.net.protocol.events.data;

import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.protocol.messages.GetHeadersMsg;
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsg;

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
