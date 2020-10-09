package com.nchain.jcl.net.protocol.events;

import com.nchain.jcl.base.tools.events.Event;
import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.net.protocol.messages.VersionMsg;
import lombok.Builder;
import lombok.Value;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when the Handshake with a Remote Peer has been rejected.
 */
@Value
@Builder(toBuilder = true)
public class PeerHandshakeRejectedEvent extends Event {
    /**
     * Definition of the possible reasons why a Handshake might be rejected
     */
    public enum HandshakedRejectedReason {
        PROTOCOL_MSG_DUPLICATE,
        PROTOCOL_MSG_TIMEOUT,
        WRONG_VERSION,
        WRONG_START_HEIGHT,
        WRONG_USER_AGENT
    }

    private PeerAddress peerAddress;
    private VersionMsg versionMsg;
    private HandshakedRejectedReason reason;
    private String detail;
}
