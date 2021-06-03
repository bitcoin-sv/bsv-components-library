package com.nchain.jcl.net.protocol.events.data;

import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.net.protocol.messages.PartialBlockTxnMsg;
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg;

/**
 * @author j.pomer@nchain.com
 * Copyright (c) 2018-2021 nChain Ltd
 */
public final class PartialBlockTxnDownloadedEvent extends MsgReceivedEvent<PartialBlockTxnMsg> {
    public PartialBlockTxnDownloadedEvent(PeerAddress peerAddress, BitcoinMsg<PartialBlockTxnMsg> blockTxsMsg) {
        super(peerAddress, blockTxsMsg);
    }
}
