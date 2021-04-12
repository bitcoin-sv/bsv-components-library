package com.nchain.jcl.net.protocol.events.control;


import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.net.network.events.P2PEvent;
import com.nchain.jcl.net.protocol.messages.BlockHeaderMsg;
import com.nchain.jcl.tools.events.Event;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-07-29 13:21
 *
 * An Event triggered when a Block Header has been downloaded.
 */
public final class BlockHeaderDownloadedEvent extends P2PEvent {
    private final PeerAddress peerAddress;
    private final BlockHeaderMsg blockHeaderMsg;

    public BlockHeaderDownloadedEvent(PeerAddress peerAddress, BlockHeaderMsg blockHeaderMsg) {
        this.peerAddress = peerAddress;
        this.blockHeaderMsg = blockHeaderMsg;
    }

    public PeerAddress getPeerAddress()         { return this.peerAddress; }
    public BlockHeaderMsg getBlockHeaderMsg()   { return this.blockHeaderMsg; }

    @Override
    public String toString() {
        return "BlockHeaderDownloadedEvent(peerAddress=" + this.getPeerAddress() + ", blockHeaderMsg=" + this.getBlockHeaderMsg() + ")";
    }
}
