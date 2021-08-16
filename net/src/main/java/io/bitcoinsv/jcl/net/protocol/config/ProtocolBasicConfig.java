/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.protocol.config;


import io.bitcoinsv.jcl.tools.handlers.HandlerConfig;
import io.bitcoinj.params.NetworkParameters;

import java.util.OptionalInt;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This class stores some basic variables. they are the ones suitable to be changed when we changed from one
 * network to a different one.
 */
public class ProtocolBasicConfig extends HandlerConfig {

    private String id;
    private long magicPackage;
    private int  port;

    private int protocolVersion = NetworkParameters.ProtocolVersion.CURRENT.getBitcoinProtocolVersion();
    private OptionalInt maxPeers = OptionalInt.empty();
    private OptionalInt minPeers = OptionalInt.empty();

    public ProtocolBasicConfig(String id, long magicPackage, int port, Integer protocolVersion, OptionalInt maxPeers, OptionalInt minPeers) {
        this.id = id;
        this.magicPackage = magicPackage;
        this.port = port;
        if (protocolVersion != null) this.protocolVersion = protocolVersion;
        if (maxPeers != null) this.maxPeers = maxPeers;
        if (minPeers != null) this.minPeers = minPeers;
    }

    public ProtocolBasicConfig() {}


    public String getId()               { return this.id; }
    public long getMagicPackage()       { return this.magicPackage; }
    public int getPort()                { return this.port; }
    public int getProtocolVersion()     { return this.protocolVersion; }
    public OptionalInt getMaxPeers()    { return this.maxPeers; }
    public OptionalInt getMinPeers()    { return this.minPeers; }

    public static ProtocolBasicConfigBuilder builder() {
        return new ProtocolBasicConfigBuilder();
    }

    public ProtocolBasicConfigBuilder toBuilder() {
        return new ProtocolBasicConfigBuilder().id(this.id).magicPackage(this.magicPackage).port(this.port).protocolVersion(this.protocolVersion).maxPeers(this.maxPeers).minPeers(this.minPeers);
    }

    /**
     * Builder
     */
    public static class ProtocolBasicConfigBuilder {
        private String id;
        private long magicPackage;
        private int port;
        private Integer protocolVersion;
        private OptionalInt maxPeers;
        private OptionalInt minPeers;

        ProtocolBasicConfigBuilder() {}

        public ProtocolBasicConfig.ProtocolBasicConfigBuilder id(String id) {
            this.id = id;
            return this;
        }

        public ProtocolBasicConfig.ProtocolBasicConfigBuilder magicPackage(long magicPackage) {
            this.magicPackage = magicPackage;
            return this;
        }

        public ProtocolBasicConfig.ProtocolBasicConfigBuilder port(int port) {
            this.port = port;
            return this;
        }

        public ProtocolBasicConfig.ProtocolBasicConfigBuilder protocolVersion(int protocolVersion) {
            this.protocolVersion = protocolVersion;
            return this;
        }

        public ProtocolBasicConfig.ProtocolBasicConfigBuilder maxPeers(OptionalInt maxPeers) {
            this.maxPeers = maxPeers;
            return this;
        }

        public ProtocolBasicConfig.ProtocolBasicConfigBuilder minPeers(OptionalInt minPeers) {
            this.minPeers = minPeers;
            return this;
        }

        public ProtocolBasicConfig build() {
            return new ProtocolBasicConfig(id, magicPackage, port, protocolVersion, maxPeers, minPeers);
        }
    }
}
