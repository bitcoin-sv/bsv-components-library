package com.nchain.jcl.protocol.handlers.pingPong;

import com.nchain.jcl.network.PeerAddress;
import com.nchain.jcl.protocol.messages.PingMsg;
import lombok.Getter;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2019-08-27 16:51
 *
 * This class stores information for each Peer that is needed in order to implement the
 * Ping-Pong P2P
 */

public class PingPongPeerInfo {
    @Getter private PeerAddress peerAddress;
    @Getter private Long timeLastActivity;
    @Getter private Long timePingSent;
    @Getter private Long noncePingSent;

    // If enabled, the Ping/Pong protocol will be disabled for this Peer: The incoming PING/PONG
    // messages from this Peer will be processed, but no verification on timeouts will be made.
    @Getter private boolean pingPongDisabled;

    /** Constructor */
    public PingPongPeerInfo(PeerAddress peerAddress) {
        this.peerAddress = peerAddress;
        this.timeLastActivity = System.currentTimeMillis();
    }

    /** It resets the info, so it can be used in a new Ping-Pong P2P */
    public synchronized void reset() {
        timePingSent = null;
        noncePingSent = null;
    }
    /** It resets the time for the last Activity to NOW */
    public synchronized void updateActivity() {
        timeLastActivity = System.currentTimeMillis();
    }

    public synchronized void updatePingStarted(PingMsg pingMsg) {
        timePingSent = System.currentTimeMillis();
        noncePingSent = pingMsg.getNonce();
    }

    /** Enables the PingPong Verification for this Peer */
    public synchronized void enablePingPong() {
        this.pingPongDisabled = false;
    }
    /** Disables the PingPong Verification for this Peer */
    public synchronized void disablePingPong() {
        this.pingPongDisabled = true;
        reset();
    }
}