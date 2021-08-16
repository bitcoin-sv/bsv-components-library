package io.bitcoinsv.jcl.net.protocol.config;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
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
