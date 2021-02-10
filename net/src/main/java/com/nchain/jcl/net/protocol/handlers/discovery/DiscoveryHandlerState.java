package com.nchain.jcl.net.protocol.handlers.discovery;


import com.nchain.jcl.tools.handlers.HandlerState;

import java.util.HashMap;
import java.util.Map;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This event stores the state of the Discovery Handler at a point in time.
 * The Discovery Handler takes care of feeding the Service with enough addresses of Remote Peers, so
 * we have a "pool" of addresses we can use when we need to connect to more Peers.
 */
public final class DiscoveryHandlerState extends HandlerState {
    // Size (number of Peers) in the Pool
    private final long poolSize;
    // We keep track of how many Peers have been handshaked, removed from the Pool, rejected...
    private final long numNodesHandshaked;
    private final long numNodesAdded;
    private final long numNodesRemoved;
    private final long numNodesRejected;
    // We also keep track of the number of GET_ADDR and DDR messages sent and received...
    private final long numGetAddrMsgsSent;
    private final long numAddrMsgsReceived;

    // The information about how much addresses go into the ADDR messages is useful, to check if they are being
    // under or over used. Wo we use a Map to store that info:
    //  Key: Number of elements in the ADDR Msg
    //  Value: Number of ADDR messages received containing that number of addresses inside
    // example: [5,20] -> we've received 20 ADDR messages containing 5 Addresses each.

    private Map<Integer, Integer> addrMsgsSize = new HashMap<>();

    DiscoveryHandlerState(long poolSize, long numNodesHandshaked, long numNodesAdded, long numNodesRemoved, long numNodesRejected, long numGetAddrMsgsSent, long numAddrMsgsReceived, Map<Integer, Integer> addrMsgsSize) {
        this.poolSize = poolSize;
        this.numNodesHandshaked = numNodesHandshaked;
        this.numNodesAdded = numNodesAdded;
        this.numNodesRemoved = numNodesRemoved;
        this.numNodesRejected = numNodesRejected;
        this.numGetAddrMsgsSent = numGetAddrMsgsSent;
        this.numAddrMsgsReceived = numAddrMsgsReceived;
        if (addrMsgsSize != null)
            this.addrMsgsSize = addrMsgsSize;
    }

    @Override
    public String toString() {
        StringBuffer result = new StringBuffer();

        result.append("Discovery State: ");
        result.append(" Pool size: " + poolSize);
        result.append(" [ ");
        result.append(numNodesHandshaked + " handshaked, ");
        result.append(numNodesAdded + " added, ");
        result.append(numNodesRemoved + " removed, ");
        result.append(numNodesRejected + " rejected");
        result.append(" ] : ");
        result.append(numGetAddrMsgsSent + " GET_ADDR sent, ");
        result.append(numAddrMsgsReceived + " ADDR received");

        return result.toString();
    }

    public long getPoolSize()                       { return this.poolSize; }
    public long getNumNodesHandshaked()             { return this.numNodesHandshaked; }
    public long getNumNodesAdded()                  { return this.numNodesAdded; }
    public long getNumNodesRemoved()                { return this.numNodesRemoved; }
    public long getNumNodesRejected()               { return this.numNodesRejected; }
    public long getNumGetAddrMsgsSent()             { return this.numGetAddrMsgsSent; }
    public long getNumAddrMsgsReceived()            { return this.numAddrMsgsReceived; }
    public Map<Integer, Integer> getAddrMsgsSize()  { return this.addrMsgsSize; }

    public DiscoveryHandlerStateBuilder toBuilder() {
        return new DiscoveryHandlerStateBuilder().poolSize(this.poolSize).numNodesHandshaked(this.numNodesHandshaked).numNodesAdded(this.numNodesAdded).numNodesRemoved(this.numNodesRemoved).numNodesRejected(this.numNodesRejected).numGetAddrMsgsSent(this.numGetAddrMsgsSent).numAddrMsgsReceived(this.numAddrMsgsReceived).addrMsgsSize(this.addrMsgsSize);
    }

    public static DiscoveryHandlerStateBuilder builder() {
        return new DiscoveryHandlerStateBuilder();
    }

    /**
     * Builder
     */
    public static class DiscoveryHandlerStateBuilder {
        private long poolSize;
        private long numNodesHandshaked;
        private long numNodesAdded;
        private long numNodesRemoved;
        private long numNodesRejected;
        private long numGetAddrMsgsSent;
        private long numAddrMsgsReceived;
        private Map<Integer, Integer> addrMsgsSize;

        DiscoveryHandlerStateBuilder() {
        }

        public DiscoveryHandlerState.DiscoveryHandlerStateBuilder poolSize(long poolSize) {
            this.poolSize = poolSize;
            return this;
        }

        public DiscoveryHandlerState.DiscoveryHandlerStateBuilder numNodesHandshaked(long numNodesHandshaked) {
            this.numNodesHandshaked = numNodesHandshaked;
            return this;
        }

        public DiscoveryHandlerState.DiscoveryHandlerStateBuilder numNodesAdded(long numNodesAdded) {
            this.numNodesAdded = numNodesAdded;
            return this;
        }

        public DiscoveryHandlerState.DiscoveryHandlerStateBuilder numNodesRemoved(long numNodesRemoved) {
            this.numNodesRemoved = numNodesRemoved;
            return this;
        }

        public DiscoveryHandlerState.DiscoveryHandlerStateBuilder numNodesRejected(long numNodesRejected) {
            this.numNodesRejected = numNodesRejected;
            return this;
        }

        public DiscoveryHandlerState.DiscoveryHandlerStateBuilder numGetAddrMsgsSent(long numGetAddrMsgsSent) {
            this.numGetAddrMsgsSent = numGetAddrMsgsSent;
            return this;
        }

        public DiscoveryHandlerState.DiscoveryHandlerStateBuilder numAddrMsgsReceived(long numAddrMsgsReceived) {
            this.numAddrMsgsReceived = numAddrMsgsReceived;
            return this;
        }

        public DiscoveryHandlerState.DiscoveryHandlerStateBuilder addrMsgsSize(Map<Integer, Integer> addrMsgsSize) {
            this.addrMsgsSize = addrMsgsSize;
            return this;
        }

        public DiscoveryHandlerState build() {
            return new DiscoveryHandlerState(poolSize, numNodesHandshaked, numNodesAdded, numNodesRemoved, numNodesRejected, numGetAddrMsgsSent, numAddrMsgsReceived, addrMsgsSize);
        }
    }
}
