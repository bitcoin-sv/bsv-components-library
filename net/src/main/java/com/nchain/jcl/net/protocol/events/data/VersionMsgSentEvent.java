package com.nchain.jcl.net.protocol.events.data;

import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.net.protocol.messages.VersionMsg;
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when a VERSION Message is sent to a remote Peer.
 */
public final class VersionMsgSentEvent extends MsgSentEvent<VersionMsg> {
    public VersionMsgSentEvent(PeerAddress peerAddress, BitcoinMsg<VersionMsg> btcMsg) {
        super(peerAddress, btcMsg);
    }
}
