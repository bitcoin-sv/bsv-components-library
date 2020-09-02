package com.nchain.jcl.net.protocol.events;

import com.nchain.jcl.base.tools.events.Event;
import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.net.protocol.messages.BlockHeaderMsg;
import com.nchain.jcl.net.protocol.messages.TransactionMsg;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-07-29 13:22
 *
 * An Event triggered when a Set of TXs from a Block has been downloaded. If you want to get notified when all the
 * TXs have been downloaded and the block has been fully download, use the BlockDownloadedEvent.
 *
 * @see BlockDownloadedEvent
 * @see LiteBlockDownloadedEvent
 */
@Value
@Builder(toBuilder = true)
@AllArgsConstructor
public class BlockTXsDownloadedEvent extends Event {
    private PeerAddress peerAddress;
    private BlockHeaderMsg blockHeaderMsg;
    private List<TransactionMsg> txsMsg;
}
