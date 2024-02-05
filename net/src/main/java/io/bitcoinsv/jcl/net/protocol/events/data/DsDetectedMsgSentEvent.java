package io.bitcoinsv.jcl.net.protocol.events.data;

import com.google.common.base.Objects;
import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.protocol.messages.CompactBlockMsg;
import io.bitcoinsv.jcl.net.protocol.messages.DsDetectedMsg;
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsg;

/**
 * @author nChain Ltd
 * Copyright (c) 2018-2024 nChain Ltd
 * <p>
 * An Event triggered when a DSDETECTED Message is sent to a remote Peer.
 * </p>
 */
public final class DsDetectedMsgSentEvent extends MsgSentEvent<DsDetectedMsg> {
    public DsDetectedMsgSentEvent(PeerAddress peerAddress, BitcoinMsg<DsDetectedMsg> btcMsg) {
        super(peerAddress, btcMsg);
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
