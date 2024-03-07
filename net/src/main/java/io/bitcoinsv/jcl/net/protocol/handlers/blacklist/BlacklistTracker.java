package io.bitcoinsv.jcl.net.protocol.handlers.blacklist;

import io.bitcoinsv.jcl.net.network.events.PeersBlacklistedEvent;

import java.net.InetAddress;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Copyright (c) 2024 nChain Ltd
 *
 * Used to track reasons & times/counts peers were blacklisted with. Can also be used to obtain duration for blacklist
 * reasons from the config
 *
 * @author nChain Ltd
 */
public class BlacklistTracker {

    private final Map<InetAddress, Map<PeersBlacklistedEvent.BlacklistReason, Long>> blacklistReasonCounters = new ConcurrentHashMap<>();
    private final List<BlacklistHandlerConfig.BlacklistEntryConfiguration> blacklistEntryConfigurations;

    private BlacklistTracker() { //hidden
        this(null);
    }

    public BlacklistTracker(BlacklistHandlerConfig config) {
        this.blacklistEntryConfigurations = config != null ?
            config.getBlacklistEntryConfigurations() :
            BlacklistHandlerConfig.DEFAULT_BLACKLIST_ENTRY_CONFIGURATIONS;
    }

    public Long findReasonCount(InetAddress inetAddress, PeersBlacklistedEvent.BlacklistReason reason) {
        return blacklistReasonCounters.computeIfAbsent(inetAddress, key -> new ConcurrentHashMap<>()) //get or create a new reason-counter map for given IP address
            .get(reason);
    }

    /**
     * Inspects {@link io.bitcoinsv.jcl.net.protocol.handlers.blacklist.BlacklistHandlerConfig.BlacklistEntryConfiguration}s
     * to find duration configured for the given reason and number of times the peer has been blacklisted.
     *
     * @param reason           {@link PeersBlacklistedEvent.BlacklistReason} to match
     * @param timesBlacklisted Number of times the peer has been blacklisted
     * @return Configured {@link Duration} if matched for the given parameters, otherwise an empty {@link Optional}
     */
    public Optional<Duration> findConfiguredDuration(PeersBlacklistedEvent.BlacklistReason reason, Long timesBlacklisted) {
        var counter = timesBlacklisted != null ? timesBlacklisted : 1L;

        return blacklistEntryConfigurations.stream()
            .filter(entryConfig -> Objects.equals(entryConfig.getReason(), reason))
            .findAny()
            .map(entryConfig -> { //compare soft threshold (if it exists), otherwise default to normal/full duration
                var durationMs = entryConfig.getSoftThreshold() != null && counter < entryConfig.getSoftThreshold() ?
                    entryConfig.getSoftDuration() :
                    entryConfig.getDuration();

                return durationMs != null ? Duration.ofMillis(durationMs) : null;
            });
    }

    /**
     * Increases the counter for the blacklist reason given for the target peer. If no counter is already present, it
     * will start with 1. Each counter is then tracked in the {@link #blacklistReasonCounters} map.<br><br>
     *
     * <i>NOTE: a counter's value is capped at {@link Long#MAX_VALUE} and cannot exceed this value.</i>
     *
     * @param inetAddress     IP address of the peer we are updating the counter for
     * @param reason          {@link io.bitcoinsv.jcl.net.network.events.PeersBlacklistedEvent.BlacklistReason} we are updating the counter for
     * @param existingCounter Previous counter to increment, if it exists
     */
    public void increaseBlacklistReasonCounter(InetAddress inetAddress, PeersBlacklistedEvent.BlacklistReason reason, Long existingCounter) {
        var counter = 1L;

        // if existing counter is not present, we create a new one starting at 1, otherwise we just increase existing counter by 1
        try {
            counter = existingCounter != null ? Math.incrementExact(existingCounter) : counter;
        } catch (ArithmeticException e) { // we want to avoid long overflow
            counter = Long.MAX_VALUE;
        }

        blacklistReasonCounters.computeIfAbsent(inetAddress, key -> new ConcurrentHashMap<>()) //get or create a new reason-counter map for given IP address
            .put(reason, counter);
    }
}
