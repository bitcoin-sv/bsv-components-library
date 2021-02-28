package com.nchain.jcl.net.protocol.events;

import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg;
import com.nchain.jcl.tools.events.Event;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when a Message is sent to a remote Peer.
 */
public final class MsgSentEvent extends Event {
    private final PeerAddress peerAddress;
    private final BitcoinMsg<?> btcMsg;

    public MsgSentEvent(PeerAddress peerAddress, BitcoinMsg<?> btcMsg) {
        this.peerAddress = peerAddress;
        this.btcMsg = btcMsg;
    }

    public PeerAddress getPeerAddress() { return this.peerAddress; }
    public BitcoinMsg<?> getBtcMsg()    { return this.btcMsg; }

    @Override
    public String toString() {
        return "Event[Msg Sent]: " + btcMsg.getHeader().getCommand().toUpperCase() + " : to " + peerAddress.toString();
    }
}
