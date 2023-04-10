package io.bitcoinsv.bsvcl.net.protocol.handlers.handshake;


import io.bitcoinsv.bsvcl.net.network.PeerAddress;
import io.bitcoinsv.bsvcl.tools.handlers.HandlerState;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This event stores the state of the Handshake Handler at a point in time.
 * the Handshake Handler takes care of implementing the Handshake Protocol, which takes place
 * right after connecting to a new Peer. It consists of a exchange of messages between the 2
 * parties to verify that they are protocol-compatible.
 *
 * This event stores the number of Peers currently handshaked or failed, and also some flags
 * that indicate if the Service has been requested to look for more Peers or to stop new connections
 * instead (these request to resume/stop connections are always triggered when the nuymber of
 * Peer handshakes go above or below some thresholds).
 */
public final class HandshakeHandlerState extends HandlerState {
    private  int numCurrentHandshakes = 0;
    private  int numHandshakesInProgress = 0;
    private  BigInteger numHandshakesFailed = BigInteger.ZERO;
    private  boolean moreConnsRequested = true;
    private  boolean stopConnsRequested;

    // Peers that were correctly handshaked but dropped the connection:
    private Set<PeerAddress> peersHandshakedLost = new HashSet<>();

    HandshakeHandlerState(int numCurrentHandshakes, int numHandshakesInProgress,
                          BigInteger numHandshakesFailed,
                          Boolean moreConnsRequested, Boolean stopConnsRequested,
                          Set<PeerAddress> peersHandshakedLost) {
        this.numCurrentHandshakes = numCurrentHandshakes;
        this.numHandshakesInProgress = numHandshakesInProgress;
        if (numHandshakesFailed != null)    this.numHandshakesFailed = numHandshakesFailed;
        if (moreConnsRequested != null)     this.moreConnsRequested = moreConnsRequested;
        if (stopConnsRequested != null)     this.stopConnsRequested = stopConnsRequested;
        this.peersHandshakedLost = peersHandshakedLost;
    }

    @Override
    public String toString() {
        StringBuffer result = new StringBuffer();

        result.append("Handshake Handler State: ");
        result.append(numCurrentHandshakes + " current Handshakes, ");
        result.append(numHandshakesInProgress + " in progress, ");
        result.append(numHandshakesFailed + " failed. ");
        result.append(peersHandshakedLost.size() + " lost.");
        if (moreConnsRequested) result.append(" More Connections requested");
        if (stopConnsRequested) result.append(" Stop Connections requested");
        return result.toString();
    }

    public int getNumCurrentHandshakes()                { return this.numCurrentHandshakes; }
    public int getNumHandshakesInProgress()             { return this.numHandshakesInProgress;}
    public BigInteger getNumHandshakesFailed()          { return this.numHandshakesFailed; }
    public boolean isMoreConnsRequested()               { return this.moreConnsRequested; }
    public boolean isStopConnsRequested()               { return this.stopConnsRequested; }
    public Set<PeerAddress> getPeersHandshakedLost()    { return this.peersHandshakedLost;}

    public HandshakeHandlerStateBuilder toBuilder() {
        return new HandshakeHandlerStateBuilder().numCurrentHandshakes(this.numCurrentHandshakes).numHandshakesFailed(this.numHandshakesFailed).moreConnsRequested(this.moreConnsRequested).stopConnsRequested(this.stopConnsRequested);
    }

    public static HandshakeHandlerStateBuilder builder() {
        return new HandshakeHandlerStateBuilder();
    }

    /**
     * Builder
     */
    public static class HandshakeHandlerStateBuilder {
        private int numCurrentHandshakes;
        private  int numHandshakesInProgress;
        private BigInteger numHandshakesFailed;
        private Boolean moreConnsRequested;
        private Boolean stopConnsRequested;
        private Set<PeerAddress> peersHandshakedLost = new HashSet<>();

        HandshakeHandlerStateBuilder() {}

        public HandshakeHandlerState.HandshakeHandlerStateBuilder numCurrentHandshakes(int numCurrentHandshakes) {
            this.numCurrentHandshakes = numCurrentHandshakes;
            return this;
        }

        public HandshakeHandlerState.HandshakeHandlerStateBuilder numHandshakesInProgress(int numHandshakesInProgress) {
            this.numHandshakesInProgress = numHandshakesInProgress;
            return this;
        }

        public HandshakeHandlerState.HandshakeHandlerStateBuilder numHandshakesFailed(BigInteger numHandshakesFailed) {
            this.numHandshakesFailed = numHandshakesFailed;
            return this;
        }

        public HandshakeHandlerState.HandshakeHandlerStateBuilder moreConnsRequested(boolean moreConnsRequested) {
            this.moreConnsRequested = moreConnsRequested;
            return this;
        }

        public HandshakeHandlerState.HandshakeHandlerStateBuilder stopConnsRequested(boolean stopConnsRequested) {
            this.stopConnsRequested = stopConnsRequested;
            return this;
        }

        public HandshakeHandlerState.HandshakeHandlerStateBuilder peersHandshakedLost(Set<PeerAddress> peersHandshakedLost) {
            this.peersHandshakedLost = peersHandshakedLost;
            return this;
        }

        public HandshakeHandlerState build() {
            return new HandshakeHandlerState(numCurrentHandshakes, numHandshakesInProgress, numHandshakesFailed, moreConnsRequested, stopConnsRequested, peersHandshakedLost);
        }
    }
}
