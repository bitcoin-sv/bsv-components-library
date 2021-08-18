/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.protocol.events.data;

import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.protocol.messages.PartialBlockTxnMsg;
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsg;

/**
 * @author j.pomer@nchain.com
 * Copyright (c) 2018-2021 nChain Ltd
 */
public final class PartialBlockTxnDownloadedEvent extends MsgReceivedEvent<PartialBlockTxnMsg> {
    public PartialBlockTxnDownloadedEvent(PeerAddress peerAddress, BitcoinMsg<PartialBlockTxnMsg> blockTxsMsg) {
        super(peerAddress, blockTxsMsg);
    }
}