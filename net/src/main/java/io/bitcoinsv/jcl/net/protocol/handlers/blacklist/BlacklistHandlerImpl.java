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

    private BlacklistHandlerState state = BlacklistHandlerState.builder().build();

    /**
     * Constructor
     */
    public BlacklistHandlerImpl(String id, RuntimeConfig runtimeConfig, BlacklistHandlerConfig config) {
        super(id, runtimeConfig);
        this.config = config;
        this.logger = new LoggerUtil(id, HANDLER_ID, this.getClass());
        this.executor = ThreadUtils.getScheduledExecutorService("JclBlacklistHandler-Whitelist");
    }

    public void registerForEvents() {
        super.eventBus.subscribe(NetStartEvent.class, e -> onNetStart((NetStartEvent) e));
        ;
        super.eventBus.subscribe(NetStopEvent.class, e -> onNetStop((NetStopEvent) e));
        super.eventBus.subscribe(PeerConnectedEvent.class, e -> onPeerConnected((PeerConnectedEvent) e));
        super.eventBus.subscribe(PeerRejectedEvent.class, e -> onPeerRejected((PeerRejectedEvent) e));
        super.eventBus.subscribe(PeerHandshakeRejectedEvent.class, e -> onPeerHandshakedRejected((PeerHandshakeRejectedEvent) e));
        super.eventBus.subscribe(PingPongFailedEvent.class, e -> onPingPongFailed((PingPongFailedEvent) e));
        super.eventBus.subscribe(BlacklistPeerRequest.class, e -> onBlacklistPeerRequest((BlacklistPeerRequest) e));
        super.eventBus.subscribe(RemovePeerFromBlacklistRequest.class, e -> onWhitelistPeerRequest((RemovePeerFromBlacklistRequest) e));
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

        executor.scheduleAtFixedRate(this::jobCheckBlacklist, 0, 1L, TimeUnit.MINUTES);
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
        BlacklistHostInfo hostInfo = this.handlerInfo.get(ip);
        if (hostInfo != null) {
            blacklist(hostInfo, event.getReason(), event.getDuration());
        }
    }

    // Event Handler:
    private void onWhitelistPeerRequest(RemovePeerFromBlacklistRequest event) {
        InetAddress ip = event.getAddress();
        BlacklistHostInfo hostInfo = this.handlerInfo.get(ip);
        if (hostInfo != null) {
            removeFromBlacklist(List.of(hostInfo));
        }
    }

    // Event Handler:
    private void onClearBlacklistRequest(ClearBlacklistRequest request) {
        var whitelist = handlerInfo.values().stream()
            .filter(BlacklistHostInfo::isBlacklisted)
            .collect(Collectors.toList());
        removeFromBlacklist(whitelist);
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
     * NOTE: The "duration" specified the duration of the blacklist, overriding the one defined in the reason itself.
     */
    private void blacklist(BlacklistHostInfo hostInfo, PeersBlacklistedEvent.BlacklistReason reason, Optional<Duration> duration) {
        logger.trace(hostInfo.getIp(), "IP Blacklisted", reason);
        hostInfo.blacklist(reason, duration);
        // We trigger an Event:
        PeersBlacklistedEvent event = new PeersBlacklistedEvent(hostInfo.getIp(), reason);
        super.eventBus.publish(event);
        updateState(reason);
    }

    /**
     * It blacklists the Host given for the reason specified.
     */
    private void blacklist(BlacklistHostInfo hostInfo, PeersBlacklistedEvent.BlacklistReason reason) {
        blacklist(hostInfo, reason, reason.getExpirationTime());
    }

    /**
     * It whitelists the Host given, making it eligible again for connection
     */
    private void removeFromBlacklist(List<BlacklistHostInfo> hostInfos) {
        logger.trace("Removing from Blacklist {} Ips...", hostInfos.size());
        hostInfos.forEach(h -> h.removeFromBacklist());
        // We publish the event to the Bus:
        super.eventBus.publish(new PeersRemovedFromBlacklistEvent(hostInfos.stream().map(h -> h.getIp()).collect(Collectors.toList())));
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
            removeFromBlacklist(hostsToWhitelist);
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
}

