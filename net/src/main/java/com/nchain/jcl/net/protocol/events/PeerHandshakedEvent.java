package com.nchain.jcl.net.protocol.events;

import com.nchain.jcl.base.tools.events.Event;
import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.net.protocol.messages.VersionMsg;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-07-06 13:14
 *
 * An Event triggered when A Peer has been handshaked and it's ready to communicate with.
 */
@Value
@Builder(toBuilder = true)
@AllArgsConstructor
public class PeerHandshakedEvent extends Event {
    private PeerAddress peerAddress;
    // Version Msg sent by the remote Peer during the Handshake process:
    private VersionMsg versionMsg;

    @Override
    public String toString() {
        StringBuffer result = new StringBuffer();
        result.append("Event[Peer Handshaked]: " + peerAddress + " : " + versionMsg.getUser_agent().getStr());
        return result.toString();
    }
}
