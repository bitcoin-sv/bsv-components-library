package io.bitcoinsv.jcl.net.protocol.events.data;


import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.network.events.P2PEvent;
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsg;
import io.bitcoinsv.jcl.net.protocol.messages.common.Message;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when a Message is received from a Remote Peer.
 */
public class MsgReceivedEvent<T extends Message> extends P2PEvent {
    private final PeerAddress peerAddress;
    private final BitcoinMsg<T> btcMsg;

    public MsgReceivedEvent(PeerAddress peerAddress, BitcoinMsg<T> btcMsg) {
        this.peerAddress = peerAddress;
        this.btcMsg = btcMsg;
    }

    public PeerAddress getPeerAddress() { return this.peerAddress; }
    public BitcoinMsg<T> getBtcMsg()    { return this.btcMsg; }

    @Override
    public String toString() {
        return "Event[" + btcMsg.getHeader().getCommand().toUpperCase() + " Received]: from " + peerAddress.toString();
    }
}
