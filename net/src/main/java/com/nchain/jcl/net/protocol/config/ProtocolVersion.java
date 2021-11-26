package com.nchain.jcl.net.protocol.config;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A repository for all the P2P Version that migh be used during Serialization or message processing.
 */
public enum ProtocolVersion {
    ENABLE_TIMESTAMP_ADDRESS(31402),
    ENABLE_VERSION(31800),
    ENABLE_RELAY(70001),
    ENABLE_ASSOCIATION_ID(70015),
    ENABLE_EXT_MSGS(70016),
    CURRENT(70015)
    ;

    private final int version;
    
    ProtocolVersion(final int version) {
        this.version = version;
    }
    public int getVersion() {
        return version;
    }

}
