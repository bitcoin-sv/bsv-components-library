package com.nchain.jcl.protocol.events;

import com.nchain.jcl.network.PeerAddress;
import com.nchain.jcl.protocol.messages.VersionMsg;
import com.nchain.jcl.tools.events.Event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-07-15 12:31
 *
 * An Event triggered when a Peer that was currently handshaked, disconnects
 */
@Value
@Builder(toBuilder = true)
@AllArgsConstructor
public class PeerHandshakedDisconnectedEvent extends Event {
    private PeerAddress peerAddress;
    private VersionMsg versionMsg;
}
