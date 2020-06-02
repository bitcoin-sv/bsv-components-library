package com.nchain.jcl.protocol.config;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2019-07-15
 *
 * A Repository for the possible values of the "services" field in the Bitcoin Protocol.
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
