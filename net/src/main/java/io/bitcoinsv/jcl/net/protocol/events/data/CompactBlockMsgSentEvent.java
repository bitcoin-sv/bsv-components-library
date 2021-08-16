/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.protocol.events.data;

import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.protocol.messages.CompactBlockMsg;
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsg;

/**
 * @author j.pomer@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * <p>
 * An Event triggered when a COMPACT BLOCK Message is sent to a remote Peer.
 * </p>
 */
public final class CompactBlockMsgSentEvent extends MsgSentEvent<CompactBlockMsg> {
    public CompactBlockMsgSentEvent(PeerAddress peerAddress, BitcoinMsg<CompactBlockMsg> btcMsg) {
        super(peerAddress, btcMsg);
    }
}
