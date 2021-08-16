package io.bitcoinsv.jcl.net.protocol.events.data;

import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.protocol.messages.GetdataMsg;
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsg;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when a GET_DATA Message is sent to a remote Peer.
 */
public final class GetDataMsgSentEvent extends MsgSentEvent<GetdataMsg> {
    public GetDataMsgSentEvent(PeerAddress peerAddress, BitcoinMsg<GetdataMsg> btcMsg) {
        super(peerAddress, btcMsg);
    }
}
