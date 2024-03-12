package io.bitcoinsv.jcl.net.protocol.handlers.blacklist;

import io.bitcoinsv.jcl.net.network.events.PeersBlacklistedEvent.BlacklistReason;
import io.bitcoinsv.jcl.net.protocol.config.ProtocolBasicConfig;
import io.bitcoinsv.jcl.tools.handlers.HandlerConfig;

import java.time.Duration;
import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2024 nChain Ltd
 *
 * It stores the configuration needed by the Blacklist Handler
 */
public class BlacklistHandlerConfig extends HandlerConfig {
    public static final List<BlacklistEntryConfiguration> DEFAULT_BLACKLIST_ENTRY_CONFIGURATIONS = List.of( // default durations & soft thresholds for each blacklist reason
        new BlacklistEntryConfiguration(BlacklistReason.CONNECTION_REJECTED, Duration.ofHours(1L).toMillis(), Duration.ofSeconds(10L).toMillis(), 1000L),
        new BlacklistEntryConfiguration(BlacklistReason.SERIALIZATION_ERROR, Duration.ofHours(4L).toMillis(), Duration.ofSeconds(30L).toMillis(), 100L),
        new BlacklistEntryConfiguration(BlacklistReason.FAILED_HANDSHAKE, Duration.ofHours(4L).toMillis(), Duration.ofMinutes(1L).toMillis(), 50L),
        new BlacklistEntryConfiguration(BlacklistReason.PINGPONG_TIMEOUT, Duration.ofHours(1L).toMillis(), Duration.ofMinutes(1L).toMillis(), 50L),
        new BlacklistEntryConfiguration(BlacklistReason.CLIENT, Duration.ofDays(1L).toMillis(), Duration.ofHours(1L).toMillis(), 10L)
    );
    private ProtocolBasicConfig basicConfig;
    private List<BlacklistEntryConfiguration> blacklistEntryConfigurations;

    public BlacklistHandlerConfig(ProtocolBasicConfig basicConfig) {
        this(basicConfig, DEFAULT_BLACKLIST_ENTRY_CONFIGURATIONS);
    }

    public BlacklistHandlerConfig(ProtocolBasicConfig basicConfig, List<BlacklistEntryConfiguration> blacklistEntryConfigurations) {
        this.basicConfig = basicConfig;
        this.blacklistEntryConfigurations = blacklistEntryConfigurations;
    }

    public ProtocolBasicConfig getBasicConfig() {
        return this.basicConfig;
    }

    public List<BlacklistEntryConfiguration> getBlacklistEntryConfigurations() {
        return this.blacklistEntryConfigurations;
    }

    public BlacklistHandlerConfigBuilder toBuilder() {
        return new BlacklistHandlerConfigBuilder()
            .basicConfig(this.basicConfig)
            .blacklistEntryConfigurations(this.blacklistEntryConfigurations);
    }

    public static BlacklistHandlerConfigBuilder builder() {
        return new BlacklistHandlerConfigBuilder();
    }

    /**
     * Builder
     */
    public static class BlacklistHandlerConfigBuilder {
        private ProtocolBasicConfig basicConfig;
        private List<BlacklistEntryConfiguration> blacklistEntryConfigurations;

        BlacklistHandlerConfigBuilder() {}

        public BlacklistHandlerConfig.BlacklistHandlerConfigBuilder basicConfig(ProtocolBasicConfig basicConfig) {
            this.basicConfig = basicConfig;
            return this;
        }

        public BlacklistHandlerConfig.BlacklistHandlerConfigBuilder blacklistEntryConfigurations(List<BlacklistEntryConfiguration> blacklistEntryConfigurations) {
            this.blacklistEntryConfigurations = blacklistEntryConfigurations;
            return this;
        }

        public BlacklistHandlerConfig build() {
            return new BlacklistHandlerConfig(basicConfig, blacklistEntryConfigurations);
        }
    }

    /**
     * Parameters for configuring blacklist behavior. This configuration defines for how long a peer should be blacklisted
     * based on the reason provided. The standard duration is meant to be the longest, while "soft" duration is meant
     * for shorter blacklist intervals, so that a peer is "given a chance to recover" in case it has connectivity issues, etc.
     * <br><br>
     * The "soft threshold" is meant to define how many "soft" blacklistings can occur for a peer before the standard
     * duration is used.
     * <br><br>
     * <i>Providing {@code null} values to durations and threshold, we should treat these values as "infinite" or "unlimited"
     * (but that may depend on the implementation requirements, this is currently supported for JCL legacy reasons).</i>
     */
    public static class BlacklistEntryConfiguration {
        private final BlacklistReason reason;
        private final Long duration;
        private final Long softDuration;
        private final Long softThreshold;

        public BlacklistEntryConfiguration(BlacklistReason reason, Long duration, Long softDuration, Long softThreshold) {
            this.reason = reason;
            this.duration = duration;
            this.softDuration = softDuration;
            this.softThreshold = softThreshold;
        }

        public BlacklistReason getReason() {
            return reason;
        }

        public Long getDuration() {
            return duration;
        }

        public Long getSoftDuration() {
            return softDuration;
        }

        public Long getSoftThreshold() {
            return softThreshold;
        }
    }
}
