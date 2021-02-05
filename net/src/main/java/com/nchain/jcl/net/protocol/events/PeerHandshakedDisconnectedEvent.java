package com.nchain.jcl.net.protocol.events;

import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.net.protocol.messages.VersionMsg;
import com.nchain.jcl.tools.events.Event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
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
