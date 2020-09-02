package com.nchain.jcl.net.protocol.config;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2019-07-15
 *
 * A repository for all the P2P Version that migh be used during Serialization or message processing.
 */
public enum ProtocolVersion {
    ENABLE_TIMESTAMP_ADDRESS(31402),
    ENABLE_VERSION(31800),
    CURRENT(70013),
    ENABLE_RELAY(70001);

    private final int bitcoinProtocol;
    
    ProtocolVersion(final int bitcoinProtocol) {
        this.bitcoinProtocol = bitcoinProtocol;
    }
    public int getBitcoinProtocolVersion() {
        return bitcoinProtocol;
    }

}
