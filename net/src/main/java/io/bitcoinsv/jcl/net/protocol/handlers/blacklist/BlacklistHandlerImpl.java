package io.bitcoinsv.jcl.net.protocol.handlers.blacklist;


import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.network.events.*;
import io.bitcoinsv.jcl.net.protocol.events.control.PeerHandshakeRejectedEvent;
import io.bitcoinsv.jcl.net.protocol.events.control.PingPongFailedEvent;
import io.bitcoinsv.jcl.tools.config.RuntimeConfig;
import io.bitcoinsv.jcl.tools.handlers.HandlerImpl;
import io.bitcoinsv.jcl.net.tools.LoggerUtil;
import io.bitcoinsv.jcl.tools.thread.ThreadUtils;
import io.bitcoinsv.jcl.tools.util.StringUtils;

import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Implementation of a Blacklist Handler
 */
public class BlacklistHandlerImpl extends HandlerImpl<InetAddress, BlacklistHostInfo> implements BlacklistHandler {

    // Suffix of the File that stores Peers from the Pool:
    private static final String NET_FOLDER = "net";
    private static final String FILE_BLACKLIST_SUFFIX = "-blacklist-handler.csv";

    private LoggerUtil logger;
    private BlacklistHandlerConfig config;

    // An executor, to run jobs in parallel:
    private final ScheduledExecutorService executor;
    private final BlacklistTracker blacklistTracker;

    private BlacklistHandlerState state = BlacklistHandlerState.builder().build();

    /**
     * Constructor
     */
    public BlacklistHandlerImpl(String id, RuntimeConfig runtimeConfig, BlacklistHandlerConfig config, BlacklistTracker blacklistTracker) {
        super(id, runtimeConfig);
        this.config = config;
        this.logger = new LoggerUtil(id, HANDLER_ID, this.getClass());
        this.executor = ThreadUtils.getScheduledExecutorService("JclBlacklistHandler-Whitelist");
        this.blacklistTracker = blacklistTracker;
    }

    /**
     * Constructor
     */
    public BlacklistHandlerImpl(String id, RuntimeConfig runtimeConfig, BlacklistHandlerConfig config) {
        this(id, runtimeConfig, config, new BlacklistTracker(config));
    }

    public void registerForEvents() {
        super.eventBus.subscribe(NetStartEvent.class, e -> onNetStart((NetStartEvent) e));
        super.eventBus.subscribe(NetStopEvent.class, e -> onNetStop((NetStopEvent) e));
        super.eventBus.subscribe(PeerConnectedEvent.class, e -> onPeerConnected((PeerConnectedEvent) e));
        super.eventBus.subscribe(PeerRejectedEvent.class, e -> onPeerRejected((PeerRejectedEvent) e));
        super.eventBus.subscribe(PeerHandshakeRejectedEvent.class, e -> onPeerHandshakedRejected((PeerHandshakeRejectedEvent) e));
        super.eventBus.subscribe(PingPongFailedEvent.class, e -> onPingPongFailed((PingPongFailedEvent) e));
        super.eventBus.subscribe(BlacklistPeerRequest.class, e -> onBlacklistPeerRequest((BlacklistPeerRequest) e));
        super.eventBus.subscribe(WhitelistPeerRequest.class, e -> onWhitelistPeerRequest((WhitelistPeerRequest) e));
        super.eventBus.subscribe(ClearBlacklistRequest.class, e -> onClearBlacklistRequest((ClearBlacklistRequest) e));
    }

    @Override
    public void init() {
        registerForEvents();

        // We load the list of blacklist hosts form disk
        loadBlacklistFromDisk();

        // Now that the list os loaded, we request a Blacklist event for them all...
        Map<InetAddress, PeersBlacklistedEvent.BlacklistReason> hostsToBlacklist = new HashMap<>();
        handlerInfo.keySet().forEach(ip -> hostsToBlacklist.put(ip, handlerInfo.get(ip).getBlacklistReason()));
        super.eventBus.publish(new PeersBlacklistedEvent(hostsToBlacklist));

        // We start the Job to look over the Blacklist IPs and whitelist them if needed:
        // rate is determined from the shortest duration in order to accommodate it
        var shortestDurationMs = findShortestDurationMs();

        logger.trace(format("Blacklist check job scheduled at a rate of %s milliseconds...", shortestDurationMs));
        executor.scheduleAtFixedRate(this::jobCheckBlacklist, 0, shortestDurationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Attempts to find the lowest possible duration from blacklist configuration. If no duration is configured,
     * a default value of 1 minute will be used. The returned value is always in milliseconds.
     * <br><br>
     * <i>NOTE: {@code null} values are supported for legacy purposes</i>
     *
     * @return The lowest possible duration in milliseconds
     */
    private Long findShortestDurationMs() {
        var possibleSoftDuration = config.getBlacklistEntryConfigurations().stream()
            .map(BlacklistHandlerConfig.BlacklistEntryConfiguration::getSoftDuration)
            .min(Comparator.nullsLast(Comparator.naturalOrder()))
            .orElse(null);
        var possibleDuration = config.getBlacklistEntryConfigurations().stream()
            .map(BlacklistHandlerConfig.BlacklistEntryConfiguration::getDuration)
            .min(Comparator.nullsLast(Comparator.naturalOrder()))
            .orElse(null);
        var duration = Duration.ofMinutes(1).toMillis(); // 1 minute by default (legacy)

        if (possibleSoftDuration != null && possibleDuration != null) {
            duration = Math.min(possibleSoftDuration, possibleDuration);
        } else if (possibleSoftDuration != null) {
            duration = possibleSoftDuration;
        } else if (possibleDuration != null) {
            duration = possibleDuration;
        }

        return duration;
    }

    // Event Handler:
    private void onNetStart(NetStartEvent event) {
        logger.trace("Starting...");
    }

    // Event Handler:
    private void onNetStop(NetStopEvent event) {
        // We save the blacklist to disk...
        saveBlacklistToDisk();
        // We stop the Whitelist Job...
        if (executor != null) executor.shutdownNow();
        logger.trace("Stop.");
    }

    // Event Handler:
    private void onPeerConnected(PeerConnectedEvent event) {
        processHostAndCheckBlacklisting(event.getPeerAddress(), null);
    }

    // Event Handler:
    private void onPeerRejected(PeerRejectedEvent event) {
        processHostAndCheckBlacklisting(event.getPeerAddress(), h -> h.addConnRejections());

    }

    // Event Handler:
    private void onPeerHandshakedRejected(PeerHandshakeRejectedEvent event) {
        processHostAndCheckBlacklisting(event.getPeerAddress(), h -> h.addFailedHandshakes());
    }

    // Event Handler:
    private void onPingPongFailed(PingPongFailedEvent event) {
        processHostAndCheckBlacklisting(event.getPeerAddress(), h -> h.addFailedPingPongs());
    }

    // Event Handler:
    private void onBlacklistPeerRequest(BlacklistPeerRequest event) {
        InetAddress ip = event.getAddress();
        BlacklistHostInfo hostInfo = this.handlerInfo.putIfAbsent(ip, new BlacklistHostInfo(ip)); // add new host info if not present

        blacklist(
            hostInfo != null ? hostInfo : this.handlerInfo.get(ip), // if host info is null, get the new value
            event.getReason(),
            event.getDuration());
    }

    // Event Handler:
    private void onWhitelistPeerRequest(WhitelistPeerRequest event) {
        InetAddress ip = event.getAddress();
        BlacklistHostInfo hostInfo = this.handlerInfo.get(ip);
        if (hostInfo != null) {
            whitelist(List.of(hostInfo));
        }
    }

    // Event Handler:
    private void onClearBlacklistRequest(ClearBlacklistRequest request) {
        var whitelist = handlerInfo.values().stream()
            .filter(BlacklistHostInfo::isBlacklisted)
            .collect(Collectors.toList());
        whitelist(whitelist);
    }

    private void loadBlacklistFromDisk() {
        String csvFileName = StringUtils.fileNamingFriendly(config.getBasicConfig().getId()) + FILE_BLACKLIST_SUFFIX;
        Path csvPath = Paths.get(runtimeConfig.getFileUtils().getRootPath().toString(), NET_FOLDER, csvFileName);
        if (Files.exists(csvPath)) {
            List<BlacklistHostInfo> hosts = runtimeConfig.getFileUtils().readCV(csvPath, () -> new BlacklistHostInfo());
            hosts.forEach(h -> handlerInfo.put(h.getIp(), h));
        }
    }

    private void saveBlacklistToDisk() {
        String csvFileName = StringUtils.fileNamingFriendly(config.getBasicConfig().getId()) + FILE_BLACKLIST_SUFFIX;
        Path csvPath = Paths.get(runtimeConfig.getFileUtils().getRootPath().toString(), NET_FOLDER, csvFileName);
        List<BlacklistHostInfo> hostsToSave = handlerInfo.values().stream().filter(h -> h.isBlacklisted()).collect(Collectors.toList());
        runtimeConfig.getFileUtils().writeCSV(csvPath, hostsToSave);
    }

    /**
     * It updates the State of this Handler
     *
     * @param newReason Reason of the new Blacklisted Host
     */
    private synchronized void updateState(PeersBlacklistedEvent.BlacklistReason newReason) {
        Map<PeersBlacklistedEvent.BlacklistReason, Integer> blacklistedReasons = this.state.getBlacklistedReasons();
        blacklistedReasons.merge(newReason, 1, (oldValue, newValue) -> Math.max(oldValue, newValue) + 1);
        this.state = this.state.toBuilder()
            .numTotalBlacklisted(state.getNumTotalBlacklisted() + 1)
            .blacklistedReasons(blacklistedReasons)
            .build();
    }

    /**
     * It blacklists the Host given for the reason specified.
     */
    private void blacklist(BlacklistHostInfo hostInfo, PeersBlacklistedEvent.BlacklistReason reason) {
        this.blacklist(hostInfo, reason, null); // null Optional is intentional
    }

    /**
     * It blacklists the Host given for the reason specified.<br>
     * The duration is optional and can be used in the following way:
     * <ul>
     *     <li>{@code null} - the duration will be calculated using {@link #blacklistTracker}</li>
     *     <li>{@link Optional#empty()} - the duration is infinite</li>
     *     <li>Value contained in {@link Optional} - the duration is applied using the contained value</li>
     * </ul>
     *
     * @param hostInfo Host to blacklist
     * @param reason Reason for blacklisting
     * @param duration Duration of the blacklist entry (see possible values above)
     */
    private void blacklist(BlacklistHostInfo hostInfo, PeersBlacklistedEvent.BlacklistReason reason, Optional<Duration> duration) {
        var ip = hostInfo.getIp();
        var existingReasonCount = blacklistTracker.findReasonCount(ip, reason);
        var durationToUse = duration != null ?
            duration :
            blacklistTracker.findConfiguredDuration(reason, existingReasonCount);

        logger.trace(format("IP Blacklisted: %s, reason=%s, duration=%s ms, existing count=%s",
            ip,
            reason,
            durationToUse.map(value -> String.valueOf(value.toMillis())).orElse("undefined"),
            existingReasonCount != null ? existingReasonCount.toString() : "none"));
        hostInfo.blacklist(reason, durationToUse);
        // We trigger an Event:
        var event = new PeersBlacklistedEvent(ip, reason);

        super.eventBus.publish(event);
        updateState(reason);
        blacklistTracker.increaseBlacklistReasonCounter(ip, reason, existingReasonCount);
    }

    /**
     * It whitelists the Host given, making it eligible again for connection
     */
    private void whitelist(List<BlacklistHostInfo> hostInfos) {
        if (!hostInfos.isEmpty()) {
            logger.trace(format("Whitelisting %s IPs...", hostInfos.size()));
            hostInfos.forEach(h -> h.whitelist());
            // We publish the event to the Bus:
            super.eventBus.publish(new PeersWhitelistedEvent(hostInfos.stream().map(h -> h.getIp()).collect(Collectors.toList())));
        } else {
            logger.trace("No IPs to whitelist...");
        }
    }

    /**
     * This method implements the business logic to determine whether this Peer should be blacklisted or not, based
     * on the status and the info stored for that Peer.
     * <p>
     * first, it updates the BlacklistHostInfo object related to this Peer, executing the update Expression on it.
     * Then, it checks if the object (after being updated by the expression) has any reason to be blacklisted, and if
     * so it gets blacklisted.
     *
     * @param peerAddress Peer Address
     * @param updateExpr  An update expression that will be executed on the BlacklistHostInfo Object stored for
     *                    this Peer.
     */
    private void processHostAndCheckBlacklisting(PeerAddress peerAddress, Consumer<BlacklistHostInfo> updateExpr) {
        InetAddress ip = peerAddress.getIp();
        BlacklistHostInfo hostInfo = handlerInfo.keySet().contains(ip) ? handlerInfo.get(ip) : new BlacklistHostInfo(ip);
        if (updateExpr != null) updateExpr.accept(hostInfo);

        // We add this Host to the Pool
        handlerInfo.put(ip, hostInfo);

        // We check if this Host should be blacklisted
        if (!hostInfo.isBlacklisted()) {
            Optional<PeersBlacklistedEvent.BlacklistReason> reasonToBlacklist = hasReasonToBeBlacklisted(hostInfo);
            reasonToBlacklist.ifPresent(r -> blacklist(hostInfo, r));
        }
    }

    /**
     * This method implements the business logic to determine whether this Peer should be blacklisted or not, based
     * on the status and the info stored for that Peer.
     */
    private Optional<PeersBlacklistedEvent.BlacklistReason> hasReasonToBeBlacklisted(BlacklistHostInfo hostInfo) {

        Optional<PeersBlacklistedEvent.BlacklistReason> result = Optional.empty();

        if (hostInfo.getNumFailedHandshakes() > 0)
            result = Optional.of(PeersBlacklistedEvent.BlacklistReason.FAILED_HANDSHAKE);
        else if (hostInfo.getNumSerializationErrors() > 0)
            result = Optional.of(PeersBlacklistedEvent.BlacklistReason.SERIALIZATION_ERROR);
        else if (hostInfo.getNumConnRejections() > 0)
            result = Optional.of(PeersBlacklistedEvent.BlacklistReason.CONNECTION_REJECTED);

        return result;
    }

    /**
     * This process looks constantly over the Blacklist and check if each individual IP can be whitelisted
     * again. It also published all those IPs that have been blacklisted but not notified yet, and notifies
     * them in a single Event
     */
    private void jobCheckBlacklist() {
        try {
            // we check if any of the Blacklisted Peers can be whitelisted...
            long numBlacklisted = handlerInfo.values().stream().filter(h -> h.isBlacklisted()).count();
            List<BlacklistHostInfo> hostsToWhitelist = handlerInfo.values().stream()
                .filter(h -> h.isBlacklistExpired())
                .collect(Collectors.toList());
            whitelist(hostsToWhitelist);
            long numBlacklistedAfter = handlerInfo.values().stream().filter(h -> h.isBlacklisted()).count();
            logger.debug("Blacklist reviewed: " + (numBlacklisted - numBlacklistedAfter) + " IPs have been WHITELISTED.");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    public BlacklistHandlerConfig getConfig() {
        return this.config;
    }

    public BlacklistHandlerState getState() {
        return this.state;
    }

    public List<BlacklistView> getBlacklistedHosts() {
        List<BlacklistView> blacklistedPeers = new ArrayList<>();
        handlerInfo.values().stream()
            .filter(BlacklistHostInfo::isBlacklisted).forEach(p ->
                blacklistedPeers.add(new BlacklistView(p.getIp(),
                    p.getBlacklistReason(),
                    p.getBlacklistTimestamp(),
                    p.getExpirationTime()))
            );
        return blacklistedPeers;
    }

    public BlacklistTracker getBlacklistTracker() {
        return this.blacklistTracker;
    }
}

