package com.nchain.jcl.net.protocol.events;


import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg;
import com.nchain.jcl.tools.events.Event;
import lombok.AllArgsConstructor;
import lombok.Value;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when a Message is received from a Remote Peer.
 */
@Value
@AllArgsConstructor
public class MsgReceivedEvent extends Event {
    private PeerAddress peerAddress;
    private BitcoinMsg<?> btcMsg;

    @Override
    public String toString() {
        return "Event[Msg Received]: " + btcMsg.getHeader().getCommand().toUpperCase() + " : from " + peerAddress.toString();
    }
}
