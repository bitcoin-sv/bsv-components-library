/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.protocol.serialization.common;


import io.bitcoinsv.jcl.net.protocol.config.ProtocolBasicConfig;

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

    //calculate the hashes when deserialzing block data
    private boolean calculateHashes;

    // Global P2P Configuration
    private ProtocolBasicConfig protocolBasicConfig;

    // Indicates whether the current Serialization is happening inside a Version Message (since some
    // serialization logic is slightly different in that case)
    private boolean insideVersionMsg;

    // If specified, this value indicates the maximum number of bytes that can be read during Deserialization.
    // If you try to read more bytes than this from the Source, the result might either be wrong or the execution
    // fails. If this value is NOT specified, that measn that its value is not needed to execute the
    // Deserialization, but you still need to be careful not to read more bytes than needed from the source.
    private Long maxBytesToRead;

    // Override default batch size
    private final Integer batchSize;

    public DeserializerContext(boolean calculateHashes, ProtocolBasicConfig protocolBasicConfig, boolean insideVersionMsg, Long maxBytesToRead, Integer batchSize) {
        this.calculateHashes = calculateHashes;
        this.protocolBasicConfig = protocolBasicConfig;
        this.insideVersionMsg = insideVersionMsg;
        this.maxBytesToRead = maxBytesToRead;
        this.batchSize = batchSize;
    }


    public boolean isCalculateHashes()                  { return this.calculateHashes; }
    public ProtocolBasicConfig getProtocolBasicConfig() { return this.protocolBasicConfig; }
    public boolean isInsideVersionMsg()                 { return this.insideVersionMsg; }
    public Long getMaxBytesToRead()                     { return this.maxBytesToRead; }
    public Integer getBatchSize()                       { return this.batchSize; }

    public void setCalculateHashes(boolean calculateHashes)                     { this.calculateHashes = calculateHashes; }
    public void setProtocolBasicConfig(ProtocolBasicConfig protocolBasicConfig) { this.protocolBasicConfig = protocolBasicConfig; }
    public void setInsideVersionMsg(boolean insideVersionMsg)                   { this.insideVersionMsg = insideVersionMsg; }
    public void setMaxBytesToRead(Long maxBytesToRead)                          { this.maxBytesToRead = maxBytesToRead; }

    public DeserializerContextBuilder toBuilder() {
        return new DeserializerContextBuilder().calculateHashes(this.calculateHashes).protocolBasicConfig(this.protocolBasicConfig).insideVersionMsg(this.insideVersionMsg).maxBytesToRead(this.maxBytesToRead);
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

        public DeserializerContext build() {
            return new DeserializerContext(calculateHashes, protocolBasicConfig, insideVersionMsg, maxBytesToRead, batchSize);
        }
    }
}
