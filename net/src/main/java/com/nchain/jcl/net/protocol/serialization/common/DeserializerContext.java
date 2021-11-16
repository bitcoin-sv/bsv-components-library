package com.nchain.jcl.net.protocol.serialization.common;


import com.nchain.jcl.net.protocol.config.ProtocolBasicConfig;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This class contains information used by a Deserializer in order to do its job. It stores
 * system/environment-level variables.
 *
 * This class is immutable and safe for Multithreading
 */

public final class DeserializerContext {

    //calculate the hashes when deserializing block data
    private boolean calculateHashes;

    // Global P2P Configuration
    private ProtocolBasicConfig protocolBasicConfig;

    // Indicates whether the current Serialization is happening inside a Version Message (since some
    // serialization logic is slightly different in that case)
    private boolean insideVersionMsg;

    // If specified, this value indicates the maximum number of bytes that can be read during Deserialization.
    // If you try to read more bytes than this from the Source, the result might either be wrong or the execution
    // fails. If this value is NOT specified, that means that its value is not needed to execute the
    // Deserialization, but you still need to be careful not to read more bytes than needed from the source.
    private Long maxBytesToRead;

    // Override default batch size
    // FIXME: REMOVE THIS FIELD AND USE THE "partialSerializationMsgSize" in DeserializerConfig instead
    private final Integer batchSize;

    // If true, the checksum of the message is calculated out of its bytes and populated in the message itself
    // This field is only informative: each serializer can calculate the checksum or not regardless of this. But
    // in some cases, a Serializer does NOT know if it should calculate the checksum or not, for example the
    // TxMsgSerializer: this serializer should calculate the checksum when deserializing individual Txs, but it
    // should NOT calculate it when deserializing Txs WITHIN a BlockMsg.
    private boolean calculateChecksum;

    public DeserializerContext(boolean calculateHashes,
                               ProtocolBasicConfig protocolBasicConfig,
                               boolean insideVersionMsg,
                               Long maxBytesToRead,
                               Integer batchSize,
                               boolean calculateChecksum) {
        this.calculateHashes = calculateHashes;
        this.protocolBasicConfig = protocolBasicConfig;
        this.insideVersionMsg = insideVersionMsg;
        this.maxBytesToRead = maxBytesToRead;
        this.batchSize = batchSize;
        this.calculateChecksum = calculateChecksum;
    }

    public boolean isCalculateHashes()                                          { return this.calculateHashes; }
    public ProtocolBasicConfig getProtocolBasicConfig()                         { return this.protocolBasicConfig; }
    public boolean isInsideVersionMsg()                                         { return this.insideVersionMsg; }
    public Long getMaxBytesToRead()                                             { return this.maxBytesToRead; }
    public Integer getBatchSize()                                               { return this.batchSize; }
    public boolean isCalculateChecksum()                                        { return this.calculateChecksum;}

    public void setCalculateHashes(boolean calculateHashes)                     { this.calculateHashes = calculateHashes; }
    public void setProtocolBasicConfig(ProtocolBasicConfig protocolBasicConfig) { this.protocolBasicConfig = protocolBasicConfig; }
    public void setInsideVersionMsg(boolean insideVersionMsg)                   { this.insideVersionMsg = insideVersionMsg; }
    public void setMaxBytesToRead(Long maxBytesToRead)                          { this.maxBytesToRead = maxBytesToRead; }
    public void setCalculateChecksum(boolean calculateChecksum)                 { this.calculateChecksum = calculateChecksum;}

    public DeserializerContextBuilder toBuilder() {
        return new DeserializerContextBuilder()
                .calculateHashes(this.calculateHashes)
                .protocolBasicConfig(this.protocolBasicConfig)
                .insideVersionMsg(this.insideVersionMsg)
                .maxBytesToRead(this.maxBytesToRead)
                .calculateChecksum(this.calculateChecksum);
    }

    public static DeserializerContextBuilder builder() {
        return new DeserializerContextBuilder();
    }

    /**
     * Builder
     */
    public static class DeserializerContextBuilder {
        private boolean calculateHashes;
        private ProtocolBasicConfig protocolBasicConfig;
        private boolean insideVersionMsg;
        private Long maxBytesToRead;
        private Integer batchSize;
        private boolean calculateChecksum = true;

        DeserializerContextBuilder() { }

        public DeserializerContext.DeserializerContextBuilder calculateHashes(boolean calculateHashes) {
            this.calculateHashes = calculateHashes;
            return this;
        }

        public DeserializerContext.DeserializerContextBuilder protocolBasicConfig(ProtocolBasicConfig protocolBasicConfig) {
            this.protocolBasicConfig = protocolBasicConfig;
            return this;
        }

        public DeserializerContext.DeserializerContextBuilder insideVersionMsg(boolean insideVersionMsg) {
            this.insideVersionMsg = insideVersionMsg;
            return this;
        }

        public DeserializerContext.DeserializerContextBuilder maxBytesToRead(Long maxBytesToRead) {
            this.maxBytesToRead = maxBytesToRead;
            return this;
        }

        public DeserializerContext.DeserializerContextBuilder batchSize(Integer batchSize) {
            this.batchSize = batchSize;
            return this;
        }

        public DeserializerContext.DeserializerContextBuilder calculateChecksum(boolean calculateChecksum) {
            this.calculateChecksum = calculateChecksum;
            return this;
        }

        public DeserializerContext build() {
            return new DeserializerContext(calculateHashes, protocolBasicConfig, insideVersionMsg, maxBytesToRead, batchSize, calculateChecksum);
        }
    }
}
