package com.nchain.jcl.net.protocol.events.data;

import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.net.protocol.messages.NotFoundMsg;
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when a NOT_FOUND Message is sent to a remote Peer.
 */
public final class NotFoundMsgSentEvent extends MsgSentEvent<NotFoundMsg> {
    public NotFoundMsgSentEvent(PeerAddress peerAddress, BitcoinMsg<NotFoundMsg> btcMsg) {
        super(peerAddress, btcMsg);
    }
}
