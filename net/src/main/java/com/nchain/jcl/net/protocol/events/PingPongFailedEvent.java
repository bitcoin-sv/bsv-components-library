package com.nchain.jcl.net.protocol.events;

import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.tools.events.Event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when a Peer has failed to perform the Ping/Pong P2P
 */
@Value
@Builder(toBuilder = true)
@AllArgsConstructor
public class PingPongFailedEvent extends Event {

    /** Stores the different reason why a Ping-Pong might fail */
    public enum PingPongFailedReason {
        MISSING_PING,
        TIMEOUT,
        WRONG_NONCE
    }

    private PeerAddress peerAddress;
    private PingPongFailedReason reason;
}
