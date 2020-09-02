package com.nchain.jcl.net.protocol.events;

import com.nchain.jcl.base.tools.events.Event;
import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.net.protocol.messages.BlockHeaderMsg;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.time.Duration;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-07-27 15:46
 *
 * An Event triggered when a Block has been fully downloaded, including its TXs. Since a Block could potentially
 * use more memory than the HW available, this Event only notifies the FACT that the block has been downloaded, and
 * also provides some info about it (like the Block Header, or the Peer it's been downloaded from).
 *
 * If you need to process the actual content of the block while it's being downloaded, you can listen to other
 * events, like the following:
 *
 * @see LiteBlockDownloadedEvent
 * @see MsgReceivedEvent (when the Msg is of type PartialBlockHeaderMSg or PartialBlockTXsMsg)
 *
 */
@Value
@Builder(toBuilder = true)
@AllArgsConstructor
public class BlockDownloadedEvent extends Event {
    private PeerAddress peerAddress;
    private BlockHeaderMsg blockHeader;
    private Duration downloadingTime;
    private Long blockSize;
}
