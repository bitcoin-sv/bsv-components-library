package io.bitcoinsv.jcl.net.protocol.events.data;

import com.google.common.base.Objects;
import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.protocol.messages.GetCompactBlockMsg;
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsg;

/**
 * An Event triggered when a GETCMPCTBLCK message is sent to a remote peer..
 */
public final class GetCompactBlockMsgSentEvent extends MsgSentEvent<GetCompactBlockMsg> {
    public GetCompactBlockMsgSentEvent(PeerAddress peerAddress, BitcoinMsg<GetCompactBlockMsg> message) {
        super(peerAddress, message);
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