package com.nchain.jcl.net.protocol.events;


import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.net.protocol.messages.BlockHeaderMsg;
import com.nchain.jcl.tools.events.Event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-07-29 13:21
 *
 * An Event triggered when a Block Header has been downloaded.
 */
@Value
@Builder(toBuilder = true)
@AllArgsConstructor
public class BlockHeaderDownloadedEvent extends Event {
    private PeerAddress peerAddress;
    private BlockHeaderMsg blockHeaderMsg;
}
