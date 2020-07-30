package com.nchain.jcl.protocol.events;

import com.nchain.jcl.network.PeerAddress;
import com.nchain.jcl.protocol.messages.BlockMsg;
import com.nchain.jcl.protocol.messages.common.BitcoinMsg;
import com.nchain.jcl.tools.events.Event;
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
 * An Event triggered when a "Lite" Block has been downloaded. A Lite block is a block which size is small enough
 * to be put into memory without risking running out of it.
 *
 * NOTE:
 * When listening for Events regarding Blocks downloaded, the best approach is to listen to "BlockDownloadedEvent"
 * instead, which is always triggered regardless of the block size. This event here thought is only triggered when
 * the Block is NOT Big
 *
 * @see BlockDownloadedEvent
 */
@Value
@Builder(toBuilder = true)
@AllArgsConstructor
public class LiteBlockDownloadedEvent extends Event {
    private PeerAddress peerAddress;
    private BitcoinMsg<BlockMsg> block; // Whole Downloaded Block.
    private Duration downloadingTime;

}
