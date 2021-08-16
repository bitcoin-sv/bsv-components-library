package io.bitcoinsv.jcl.net.protocol.serialization.common;


import io.bitcoinsv.jcl.net.protocol.config.ProtocolBasicConfig;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This class contains information used by a Serializer in order to do its job. It stores
 * system/environemnt-level variables.
 *
 * This class is immutable and safe for Multithreading
 */
public final class SerializerContext {

    //The Magic Value used in the Messages Header, to identify the Network
    private long magicPackage;

    // The P2P Version used
    private int handshakeProtocolVersion;

    // Global P2P Configuration
    private ProtocolBasicConfig protocolBasicConfig;

    // Indicates whether the current Serialization is happening inside a Version Message (since some
    // serialization logic is slightly different in that case)
    private boolean insideVersionMsg;

    public SerializerContext(long magicPackage, int handshakeProtocolVersion, ProtocolBasicConfig protocolBasicConfig, boolean insideVersionMsg) {
        this.magicPackage = magicPackage;
        this.handshakeProtocolVersion = handshakeProtocolVersion;
        this.protocolBasicConfig = protocolBasicConfig;
        this.insideVersionMsg = insideVersionMsg;
    }

    public long getMagicPackage()                       { return this.magicPackage; }
    public int getHandshakeProtocolVersion()            { return this.handshakeProtocolVersion; }
    public ProtocolBasicConfig getProtocolBasicConfig() { return this.protocolBasicConfig; }
    public boolean isInsideVersionMsg()                 { return this.insideVersionMsg; }

    public void setMagicPackage(long magicPackage)                              { this.magicPackage = magicPackage; }
    public void setHandshakeProtocolVersion(int handshakeProtocolVersion)       { this.handshakeProtocolVersion = handshakeProtocolVersion; }
    public void setProtocolBasicConfig(ProtocolBasicConfig protocolBasicConfig) { this.protocolBasicConfig = protocolBasicConfig; }
    public void setInsideVersionMsg(boolean insideVersionMsg)                   { this.insideVersionMsg = insideVersionMsg; }

    public SerializerContextBuilder toBuilder() {
        return new SerializerContextBuilder().magicPackage(this.magicPackage).handshakeProtocolVersion(this.handshakeProtocolVersion).protocolBasicConfig(this.protocolBasicConfig).insideVersionMsg(this.insideVersionMsg);
    }

    public static SerializerContextBuilder builder() {
        return new SerializerContextBuilder();
    }

    /**
     * Builder
     */
    public static class SerializerContextBuilder {
        private long magicPackage;
        private int handshakeProtocolVersion;
        private ProtocolBasicConfig protocolBasicConfig;
        private boolean insideVersionMsg;

        SerializerContextBuilder() {}

        public SerializerContext.SerializerContextBuilder magicPackage(long magicPackage) {
            this.magicPackage = magicPackage;
            return this;
        }

        public SerializerContext.SerializerContextBuilder handshakeProtocolVersion(int handshakeProtocolVersion) {
            this.handshakeProtocolVersion = handshakeProtocolVersion;
            return this;
        }

        public SerializerContext.SerializerContextBuilder protocolBasicConfig(ProtocolBasicConfig protocolBasicConfig) {
            this.protocolBasicConfig = protocolBasicConfig;
            return this;
        }

        public SerializerContext.SerializerContextBuilder insideVersionMsg(boolean insideVersionMsg) {
            this.insideVersionMsg = insideVersionMsg;
            return this;
        }

        public SerializerContext build() {
            return new SerializerContext(magicPackage, handshakeProtocolVersion, protocolBasicConfig, insideVersionMsg);
        }
    }
}
