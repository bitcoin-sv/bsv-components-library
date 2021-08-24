package com.nchain.jcl.net.protocol.handlers.message;

import java.time.Duration;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * @date 23/08/2021
 *
 * a Configuration for Batch Messages. It can be used to store how the messages are broadcast to the rest of the System
 * after being Deserialized, or how the messages are sent out to the network. The specific MEsage/s types where this
 * config is applied will be specified eternally.
 *
 */
public class MessageBatchConfig {
    private Integer maxMsgsInBatch;
    private Duration maxIntervalBetweenBatches;
    private Integer maxBatchSizeInBytes;

    /** Constructor */
    private MessageBatchConfig(Integer maxMsgsInBatch, Duration maxIntervalBetweenBatches, Integer maxBatchSizeInBytes) {
        this.maxMsgsInBatch = maxMsgsInBatch;
        this.maxIntervalBetweenBatches = maxIntervalBetweenBatches;
        this.maxBatchSizeInBytes = maxBatchSizeInBytes;
    }

    public Integer getMaxMsgsInBatch()              { return this.maxMsgsInBatch;}
    public Duration getMaxIntervalBetweenBatches()  { return this.maxIntervalBetweenBatches;}
    public Integer getMaxBatchSizeInbytes()         { return this.maxBatchSizeInBytes;}

    public MessageBatchBuilder toBuilder() {
        return new MessageBatchBuilder()
                .maxMsgsInBatch(this.maxMsgsInBatch)
                .maxIntervalBetweenBatches(this.maxIntervalBetweenBatches)
                .maxBatchSizeInBytes(this.maxBatchSizeInBytes);
    }

    /** Builder */
    public static class MessageBatchBuilder {
        private Integer maxMsgsInBatch;
        private Duration maxIntervalBetweenBatches;
        private Integer maxBatchSizeInBytes;

        public MessageBatchBuilder maxMsgsInBatch(Integer maxMsgsInBatch) {
            this.maxMsgsInBatch = maxMsgsInBatch;
            return this;
        }

        public MessageBatchBuilder maxIntervalBetweenBatches(Duration maxIntervalBetweenBatches) {
            this.maxIntervalBetweenBatches = maxIntervalBetweenBatches;
            return this;
        }

        public MessageBatchBuilder maxBatchSizeInBytes(Integer maxBatchSizeInBytes) {
            this.maxBatchSizeInBytes = maxBatchSizeInBytes;
            return this;
        }

        public MessageBatchConfig build() {
            return new MessageBatchConfig(this.maxMsgsInBatch, this.maxIntervalBetweenBatches, this.maxBatchSizeInBytes);
        }
    }

}
