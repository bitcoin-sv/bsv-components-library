package com.nchain.jcl.net.protocol.events.data;


import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.net.network.events.P2PEvent;
import com.nchain.jcl.net.protocol.events.data.MsgReceivedEvent;
import com.nchain.jcl.net.protocol.messages.BlockHeaderMsg;
import com.nchain.jcl.net.protocol.messages.PartialBlockHeaderMsg;
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg;
import com.nchain.jcl.tools.events.Event;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-07-29 13:21
 *
 * An Event triggered when a Block Header has been downloaded.
 */
public final class BlockHeaderDownloadedEvent extends MsgReceivedEvent<PartialBlockHeaderMsg> {

    public BlockHeaderDownloadedEvent(PeerAddress peerAddress, BitcoinMsg<PartialBlockHeaderMsg> blockHeaderMsg) {
        super(peerAddress, blockHeaderMsg);
    }

}
