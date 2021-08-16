/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.protocol.handlers.pingPong;


import io.bitcoinsv.jcl.tools.handlers.HandlerState;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This event stores the state of the PingPong Handler at a point in time.
 * The PingPong Handler takes care of checking that the Remote Peers we are connected to are still
 * "alive". On a frequency basis, it sends a PING message to them, expecting a PONG message back. If
 * the response does not come or comes out of time, the Peer has then broken the timeout and will most
 * probably be blacklisted.
 */
public final class PingPongHandlerState extends HandlerState {
    private final long numPingInProcess;

    PingPongHandlerState(long numPingInProcess) {
        this.numPingInProcess = numPingInProcess;
    }

    public long getNumPingInProcess() {
        return this.numPingInProcess;
    }

    @Override
    public String toString() {
        return "PingPong-Handler State: " + numPingInProcess + " Pings in progress";
    }

    public PingPongHandlerStateBuilder toBuilder() {
        return new PingPongHandlerStateBuilder().numPingInProcess(this.numPingInProcess);
    }

    public static PingPongHandlerStateBuilder builder() {
        return new PingPongHandlerStateBuilder();
    }

    /**
     * Builder
     */
    public static class PingPongHandlerStateBuilder {
        private long numPingInProcess;

        PingPongHandlerStateBuilder() {}

        public PingPongHandlerState.PingPongHandlerStateBuilder numPingInProcess(long numPingInProcess) {
            this.numPingInProcess = numPingInProcess;
            return this;
        }

        public PingPongHandlerState build() {
            return new PingPongHandlerState(numPingInProcess);
        }
    }
}
