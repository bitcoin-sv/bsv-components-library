package io.bitcoinsv.jcl.net.protocol.events.data;


import com.google.common.base.Objects;
import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.protocol.messages.RawMsg;
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsg;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when a Raw Message is received from a Remote Peer.
 */
public class RawMsgReceivedEvent<T extends RawMsg> extends MsgReceivedEvent {

    public RawMsgReceivedEvent(PeerAddress peerAddress, BitcoinMsg<T> btcMsg) {
        super(peerAddress, btcMsg);
    }

    @Override
    public String toString() {
        return "Raw " + super.toString();
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode());
    }
}
