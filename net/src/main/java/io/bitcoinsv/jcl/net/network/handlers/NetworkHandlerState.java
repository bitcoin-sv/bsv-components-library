package io.bitcoinsv.jcl.net.network.handlers;

import io.bitcoinsv.jcl.tools.handlers.HandlerState;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This Event stores the current State of the Connection/Network Handler. The Network Handler implements the
 * physical and low-level connection to a remote Peers, and handles all the incoming/outcoming data between
 * the 2 parties.
 */
public final class NetworkHandlerState extends HandlerState {
    private final int numActiveConns;
    private final int numInProgressConns;
    private final int numPendingToOpenConns;
    private final int numPendingToCloseConns;
    private final long numConnsFailed;
    private final long numInProgressConnsExpired;
    private final int numPeersBlacklisted;

    private final boolean server_mode;
    private final boolean keep_connecting;

    private int numConnsTried;

    NetworkHandlerState(int numActiveConns, int numInProgressConns, int numPendingToOpenConns, int numPendingToCloseConns,
                        boolean server_mode, boolean keep_connecting,
                        long numConnsFailed, long numInProgressConnsExpired,
                        int numPeersBlacklisted,
                        int numConnsTried) {
        this.numActiveConns = numActiveConns;
        this.numInProgressConns = numInProgressConns;
        this.numPendingToOpenConns = numPendingToOpenConns;
        this.numPendingToCloseConns = numPendingToCloseConns;
        this.server_mode = server_mode;
        this.keep_connecting = keep_connecting;
        this.numConnsFailed = numConnsFailed;
        this.numInProgressConnsExpired = numInProgressConnsExpired;
        this.numPeersBlacklisted = numPeersBlacklisted;
        this.numConnsTried = numConnsTried;
    }


    @Override
    public String toString() {
        StringBuffer result = new StringBuffer();
        result.append("Network Handler State: ");
        result.append("Connections: ");
        result.append(numActiveConns).append(" active, ");
        result.append(numInProgressConns).append(" in progress, ");
        result.append(numPeersBlacklisted).append(" blacklisted, ");
        result.append(numConnsTried).append(" tried since last state, ");
        result.append(numPendingToOpenConns).append( " pending to Open, ");
        result.append(numPendingToCloseConns).append(" pending to Close, ");
        result.append(numConnsFailed).append(" failed, ");
        result.append(numInProgressConnsExpired).append(" in-progress expired, ");
        result.append(": ").append((server_mode)? "Running in Server Mode" : "Running in Client Mode");
        result.append(": ").append((keep_connecting)? "connecting": "connections stable");

        return result.toString();
    }

    public int getNumActiveConns()          { return this.numActiveConns; }
    public int getNumInProgressConns()      { return this.numInProgressConns; }
    public int getNumPendingToOpenConns()   { return this.numPendingToOpenConns; }
    public int getNumPendingToCloseConns()  { return this.numPendingToCloseConns; }
    public int getNumPeersBlacklisted()     { return this.numPeersBlacklisted;}
    public boolean isServer_mode()          { return this.server_mode; }
    public boolean isKeep_connecting()      { return this.keep_connecting; }
    public int getNumCopnnsTried()          { return this.numConnsTried; }

    public static NetworkHandlerStateBuilder builder() {
        return new NetworkHandlerStateBuilder();
    }
    public NetworkHandlerStateBuilder toBuilder() {
        return new NetworkHandlerStateBuilder()
                .numActiveConns(this.numActiveConns)
                .numInProgressConns(this.numInProgressConns)
                .numPendingToOpenConns(this.numPendingToOpenConns)
                .numPendingToCloseConns(this.numPendingToCloseConns)
                .server_mode(this.server_mode)
                .keep_connecting(this.keep_connecting)
                .numConnsFailed(this.numConnsFailed)
                .numInProgressConnsExpired(this.numInProgressConnsExpired)
                .numConnsTried(this.numConnsTried);
    }

    /**
     * Builder
     */
    public static class NetworkHandlerStateBuilder {
        private int numActiveConns;
        private int numInProgressConns;
        private int numPendingToOpenConns;
        private int numPendingToCloseConns;
        private long numConnsFailed;
        private long numInProgressConnsExpired;
        private int numPeersBlacklisted;
        private boolean server_mode;
        private boolean keep_connecting;
        private int numConnsTried;

        NetworkHandlerStateBuilder() {}

        public NetworkHandlerState.NetworkHandlerStateBuilder numActiveConns(int numActiveConns) {
            this.numActiveConns = numActiveConns;
            return this;
        }

        public NetworkHandlerState.NetworkHandlerStateBuilder numInProgressConns(int numInProgressConns) {
            this.numInProgressConns = numInProgressConns;
            return this;
        }

        public NetworkHandlerState.NetworkHandlerStateBuilder numPendingToOpenConns(int numPendingToOpenConns) {
            this.numPendingToOpenConns = numPendingToOpenConns;
            return this;
        }

        public NetworkHandlerState.NetworkHandlerStateBuilder numPendingToCloseConns(int numPendingToCloseConns) {
            this.numPendingToCloseConns = numPendingToCloseConns;
            return this;
        }

        public NetworkHandlerState.NetworkHandlerStateBuilder numConnsFailed(long numConnsFailed) {
            this.numConnsFailed = numConnsFailed;
            return this;
        }

        public NetworkHandlerState.NetworkHandlerStateBuilder numInProgressConnsExpired(long numInProgressConnsExpired) {
            this.numInProgressConnsExpired = numInProgressConnsExpired;
            return this;
        }

        public NetworkHandlerState.NetworkHandlerStateBuilder numPeersBlacklisted(int numPeersBlacklisted) {
            this.numPeersBlacklisted = numPeersBlacklisted;
            return this;
        }

        public NetworkHandlerState.NetworkHandlerStateBuilder server_mode(boolean server_mode) {
            this.server_mode = server_mode;
            return this;
        }

        public NetworkHandlerState.NetworkHandlerStateBuilder keep_connecting(boolean keep_connecting) {
            this.keep_connecting = keep_connecting;
            return this;
        }

        public NetworkHandlerState.NetworkHandlerStateBuilder numConnsTried(int numConnsTried) {
            this.numConnsTried = numConnsTried;
            return this;
        }

        public NetworkHandlerState build() {
            return new NetworkHandlerState(
                    numActiveConns,
                    numInProgressConns,
                    numPendingToOpenConns,
                    numPendingToCloseConns,
                    server_mode,
                    keep_connecting,
                    numConnsFailed,
                    numInProgressConnsExpired,
                    numPeersBlacklisted,
                    numConnsTried);
        }
    }
}
