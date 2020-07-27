package com.nchain.jcl.protocol.events;

import com.nchain.jcl.network.PeerAddress;
import com.nchain.jcl.protocol.messages.common.BitcoinMsg;
import com.nchain.jcl.tools.events.Event;
import lombok.AllArgsConstructor;
import lombok.Value;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-25 17:22
 *
 * An Event triggered when a Message is sent to a remote Peer.
 */
@Value
@AllArgsConstructor
public class MsgSentEvent extends Event {
    private PeerAddress peerAddress;
    private BitcoinMsg<?> btcMsg;

    @Override
    public String toString() {
        return "Event[Msg Sent]: " + btcMsg.getHeader().getCommand().toUpperCase() + " : from " + peerAddress.toString();
    }
}
