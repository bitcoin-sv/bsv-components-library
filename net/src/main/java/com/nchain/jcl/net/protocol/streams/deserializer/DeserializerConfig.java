package com.nchain.jcl.net.protocol.streams.deserializer;

import com.nchain.jcl.net.protocol.messages.BlockHeaderMsg;
import com.nchain.jcl.net.protocol.messages.HeadersMsg;
import com.nchain.jcl.net.protocol.messages.InvMessage;
import com.nchain.jcl.net.protocol.messages.TxMsg;

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

    /** If disabled, no chache is used at all */
    private boolean enabled = true;

    /** Maximum Size of the Cache (in Bytes) */
    /**
     * THIS PARAMETER IS NO LONGER SUPPORTED BY JCL-NET.
     * It's still here though, in case we want to re-enable it in the future.
     */
    private Long maxCacheSizeInBytes = 10_000_000L; // 10 MB

    /** Maximum Size of the Cache (in number of Messages cached) */
    private Long maxCacheSizeInNumMsgs = 50L;

    /** If an Item is idle for longer than this, it gets cleared from the cache */
    private Duration expirationTime = Duration.ofMinutes(10);

    /** Only messages Smaller than this Value will be cached: */
    private Long maxMsgSizeInBytes = 500_000L; // 10KB

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

    public DeserializerConfig(Boolean enabled,
                              Long maxCacheSizeInBytes,
                              Long maxCacheSizeInNumMsgs,
                              Duration expirationTime,
                              Long maxMsgSizeInBytes,
                              Boolean generateStats,
                              Set<String> messagesToCache) {
        if (enabled != null)                this.enabled = enabled;
        if (maxCacheSizeInBytes != null)    this.maxCacheSizeInBytes = maxCacheSizeInBytes;
        if (maxCacheSizeInNumMsgs != null)  this.maxCacheSizeInNumMsgs = maxCacheSizeInNumMsgs;
        if (expirationTime != null)         this.expirationTime = expirationTime;
        if (maxMsgSizeInBytes != null)      this.maxMsgSizeInBytes = maxMsgSizeInBytes;
        if (generateStats != null)          this.generateStats = generateStats;
        if (messagesToCache != null)        this.messagesToCache = messagesToCache;
    }

    public static DeserializerConfigBuilder builder()   { return new DeserializerConfigBuilder(); }
    public boolean isEnabled()                          { return enabled; }
    public Long getMaxCacheSizeInNumMsgs()              { return this.maxCacheSizeInNumMsgs;}
    public Long getMaxCacheSizeInBytes()                { return this.maxCacheSizeInBytes; }
    public Duration getExpirationTime()                 { return this.expirationTime; }
    public Long getMaxMsgSizeInBytes()                  { return this.maxMsgSizeInBytes; }
    public boolean isGenerateStats()                    { return this.generateStats; }
    public Set<String> getMessagesToCache()             { return this.messagesToCache; }


    @Override
    public String toString() {
        return "DeserializerConfig(maxCacheSizeInBytes=" + this.maxCacheSizeInBytes + ", maxMsgSizeInBytes=" + this.maxMsgSizeInBytes + ", generateStats=" + this.generateStats + ", messagesToCache=" + this.messagesToCache + ")";
    }

    public DeserializerConfigBuilder toBuilder() {
        return new DeserializerConfigBuilder().maxCacheSizeInBytes(this.maxCacheSizeInBytes).maxMsgSizeInBytes(this.maxMsgSizeInBytes).generateStats(this.generateStats).messagesToCache(this.messagesToCache);
    }

    /**
     * Builder
     */
    public static class DeserializerConfigBuilder {
        private Boolean enabled;
        private Long maxCacheSizeInBytes;
        private Long maxCacheSizeInNumMsgs;
        private Duration expirationTime;
        private Long maxMsgSizeInBytes;
        private boolean generateStats;
        private Set<String> messagesToCache;

        DeserializerConfigBuilder() { }

        public DeserializerConfig.DeserializerConfigBuilder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public DeserializerConfig.DeserializerConfigBuilder maxCacheSizeInNumMsgs(Long maxCacheSizeInNumMsgs) {
            this.maxCacheSizeInNumMsgs = maxCacheSizeInNumMsgs;
            return this;
        }

        public DeserializerConfig.DeserializerConfigBuilder expirationTime(Duration expirationTime) {
            this.expirationTime = expirationTime;
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
            return new DeserializerConfig(enabled,
                    maxCacheSizeInBytes,
                    maxCacheSizeInNumMsgs,
                    expirationTime,
                    maxMsgSizeInBytes,
                    generateStats,
                    messagesToCache);
        }
    }
}
