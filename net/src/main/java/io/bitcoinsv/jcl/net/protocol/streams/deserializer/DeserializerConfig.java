/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.protocol.streams.deserializer;

import io.bitcoinsv.jcl.net.protocol.messages.BlockHeaderMsg;
import io.bitcoinsv.jcl.net.protocol.messages.HeadersMsg;
import io.bitcoinsv.jcl.net.protocol.messages.TxMsg;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayReaderRealTime;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Configuration class for the Deserializer.
 */
public final class DeserializerConfig {

    /**
     *  Initial size of each Buffer assigned to each Peer for Deserialization.
     *  The Buffer can still expand/collapse over time if needed, this is just the initial size
     */
    private Integer bufferInitialSizeInBytes = 5_000_000; // 5 MB by default

    /** Indicates the minimum speed of the bytes coming from the remote Peer */
    private int minBytesPerSecForLargeMessages = ByteArrayReaderRealTime.DEFAULT_SPEED_BYTES_PER_SECOND;

    /** If disabled, no cache is used at all */
    private boolean cacheEnabled = true;

    /** Maximum Size of the Cache (in Bytes) */
    /**
     * THIS PARAMETER IS NO LONGER SUPPORTED BY JCL-NET.
     * It's still here though, in case we want to re-enable it in the future.
     */
    private Long maxCacheSizeInBytes = 10_000_000L; // 10 MB

    /** Maximum Size of the Cache (in number of Messages cached) */
    private Long maxCacheSizeInNumMsgs = 50L;

    /** If an Item is idle for longer than this, it gets cleared from the cache */
    private Duration cacheExpirationTime = Duration.ofMinutes(10);

    /** Only messages Smaller than this Value will be cached: */
    private Long cacheMaxMsgSizeInBytes = 500_000L; // 10KB

    /** If TRUE; statistics of the Cache are generating in real-time */
    private boolean generateStats = false;

    // Default List of Messages to Cache...
    private static final String[] DEFAULT_MSGS_TO_CACHE = {
            HeadersMsg.MESSAGE_TYPE.toUpperCase(),
            TxMsg.MESSAGE_TYPE.toUpperCase(),
            BlockHeaderMsg.MESSAGE_TYPE.toUpperCase()
    };

    /** If the Message is NOT part of this List, then it won't be cached */
    private Set<String> messagesToCache = new HashSet<>(Arrays.asList(DEFAULT_MSGS_TO_CACHE));

    public DeserializerConfig(Integer bufferInitialSizeInBytes,
                              Integer minBytesPerSecForLargeMessages,
                              Boolean cacheEnabled,
                              Long maxCacheSizeInBytes,
                              Long maxCacheSizeInNumMsgs,
                              Duration expirationTime,
                              Long maxMsgSizeInBytes,
                              Boolean generateStats,
                              Set<String> messagesToCache) {
        if (bufferInitialSizeInBytes != null)       this.bufferInitialSizeInBytes = bufferInitialSizeInBytes;
        if (minBytesPerSecForLargeMessages != null) this.minBytesPerSecForLargeMessages = minBytesPerSecForLargeMessages;
        if (cacheEnabled != null)                   this.cacheEnabled = cacheEnabled;
        if (maxCacheSizeInBytes != null)            this.maxCacheSizeInBytes = maxCacheSizeInBytes;
        if (maxCacheSizeInNumMsgs != null)          this.maxCacheSizeInNumMsgs = maxCacheSizeInNumMsgs;
        if (expirationTime != null)                 this.cacheExpirationTime = expirationTime;
        if (maxMsgSizeInBytes != null)              this.cacheMaxMsgSizeInBytes = maxMsgSizeInBytes;
        if (generateStats != null)                  this.generateStats = generateStats;
        if (messagesToCache != null)                this.messagesToCache = messagesToCache;
    }

    public static DeserializerConfigBuilder builder()   { return new DeserializerConfigBuilder(); }
    public int getBufferInitialSizeInBytes()            { return this.bufferInitialSizeInBytes;}
    public int getMinBytesPerSecForLargeMessages()      { return this.minBytesPerSecForLargeMessages;}
    public boolean isCacheEnabled()                     { return cacheEnabled; }
    public Long getMaxCacheSizeInNumMsgs()              { return this.maxCacheSizeInNumMsgs;}
    public Long getMaxCacheSizeInBytes()                { return this.maxCacheSizeInBytes; }
    public Duration getCacheExpirationTime()            { return this.cacheExpirationTime; }
    public Long getCacheMaxMsgSizeInBytes()             { return this.cacheMaxMsgSizeInBytes; }
    public boolean isGenerateStats()                    { return this.generateStats; }
    public Set<String> getMessagesToCache()             { return this.messagesToCache; }


    @Override
    public String toString() {
        return "DeserializerConfig(bufferInitialSizeInBytes=" + bufferInitialSizeInBytes + ",minBytesPerSecForLargeMessages=" + minBytesPerSecForLargeMessages + ", maxCacheSizeInBytes=" + this.maxCacheSizeInBytes + ", maxMsgSizeInBytes=" + this.cacheMaxMsgSizeInBytes + ", generateStats=" + this.generateStats + ", messagesToCache=" + this.messagesToCache + ")";
    }

    public DeserializerConfigBuilder toBuilder() {
        return new DeserializerConfigBuilder()
                .bufferInitialSizeInBytes(this.bufferInitialSizeInBytes)
                .minBytesPerSecForLargeMessages(this.minBytesPerSecForLargeMessages)
                .cacheEnabled(this.cacheEnabled)
                .maxCacheSizeInNumMsgs(this.maxCacheSizeInNumMsgs)
                .maxCacheSizeInBytes(this.maxCacheSizeInBytes)
                .maxMsgSizeInBytes(this.cacheMaxMsgSizeInBytes)
                .cacheExpirationTime(this.cacheExpirationTime)
                .generateStats(this.generateStats)
                .messagesToCache(this.messagesToCache);
    }

    /**
     * Builder
     */
    public static class DeserializerConfigBuilder {
        private Integer bufferInitialSizeInBytes;
        private Integer minBytesPerSecForLargeMessages;
        private Boolean cacheEnabled;
        private Long maxCacheSizeInBytes;
        private Long maxCacheSizeInNumMsgs;
        private Duration cacheExpirationTime;
        private Long maxMsgSizeInBytes;
        private boolean generateStats;
        private Set<String> messagesToCache;

        DeserializerConfigBuilder() { }


        public DeserializerConfig.DeserializerConfigBuilder bufferInitialSizeInBytes(int bufferInitialSizeInBytes) {
            this.bufferInitialSizeInBytes = bufferInitialSizeInBytes;
            return this;
        }

        public DeserializerConfig.DeserializerConfigBuilder minBytesPerSecForLargeMessages(int minBytesPerSecForLargeMessages) {
            this.minBytesPerSecForLargeMessages = minBytesPerSecForLargeMessages;
            return this;
        }

        public DeserializerConfig.DeserializerConfigBuilder cacheEnabled(boolean cacheEnabled) {
            this.cacheEnabled = cacheEnabled;
            return this;
        }

        public DeserializerConfig.DeserializerConfigBuilder maxCacheSizeInNumMsgs(Long maxCacheSizeInNumMsgs) {
            this.maxCacheSizeInNumMsgs = maxCacheSizeInNumMsgs;
            return this;
        }

        public DeserializerConfig.DeserializerConfigBuilder cacheExpirationTime(Duration cacheExpirationTime) {
            this.cacheExpirationTime = cacheExpirationTime;
            return this;
        }

        public DeserializerConfig.DeserializerConfigBuilder maxCacheSizeInBytes(Long maxCacheSizeInBytes) {
            this.maxCacheSizeInBytes = maxCacheSizeInBytes;
            return this;
        }

        public DeserializerConfig.DeserializerConfigBuilder maxMsgSizeInBytes(Long maxMsgSizeInBytes) {
            this.maxMsgSizeInBytes = maxMsgSizeInBytes;
            return this;
        }

        public DeserializerConfig.DeserializerConfigBuilder generateStats(boolean generateStats) {
            this.generateStats = generateStats;
            return this;
        }

        public DeserializerConfig.DeserializerConfigBuilder messagesToCache(Set<String> messagesToCache) {
            this.messagesToCache = messagesToCache;
            return this;
        }

        public DeserializerConfig build() {
            return new DeserializerConfig(
                    bufferInitialSizeInBytes,
                    minBytesPerSecForLargeMessages,
                    cacheEnabled,
                    maxCacheSizeInBytes,
                    maxCacheSizeInNumMsgs,
                    cacheExpirationTime,
                    maxMsgSizeInBytes,
                    generateStats,
                    messagesToCache);
        }
    }
}
