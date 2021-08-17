/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.protocol.config;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A Repository for the possible values of the "services" field in the Bitcoin P2P.
 */
public enum ProtocolServices {
        NODE_NETWORK(1 << 0),
        NODE_GETUTXOS(1 << 1),
        NODE_BLOOM(1 << 2),
        NODE_WITNESS(1 << 3),
        NODE_NETWORK_LIMITED(0x400);

        private final int services;

        ProtocolServices(int services) {
            this.services = services;
        }
        public int getProtocolServices() {
            return services;
        }
}
