package com.nchain.jcl.net.protocol.handlers.blacklist;


import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.net.network.events.*;
import com.nchain.jcl.net.protocol.events.control.PeerHandshakeRejectedEvent;
import com.nchain.jcl.net.protocol.events.control.PingPongFailedEvent;
import com.nchain.jcl.tools.config.RuntimeConfig;
import com.nchain.jcl.tools.handlers.HandlerImpl;
import com.nchain.jcl.tools.log.LoggerUtil;
import com.nchain.jcl.tools.thread.ThreadUtils;
import com.nchain.jcl.tools.util.StringUtils;

import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Implementation of a Blacklist Handler
 */
public class BlacklistHandlerImpl extends HandlerImpl implements BlacklistHandler {

    // Suffix of the File that stores Peers from the Pool:
    private static final String NET_FOLDER = "net";
    private static final String FILE_BLACKLIST_SUFFIX = "-blacklist-handler.csv";

    private LoggerUtil logger;
    private BlacklistHandlerConfig config;

    // Pool of Hosts. They might be blacklisted or not. We keep track of ALL The different IP addresses (hosts)
    // we connect to during the session:
    private Map<InetAddress, BlacklistHostInfo> hostsInfo = new ConcurrentHashMap<>();

    // An executor, to run jobs in parallel:
    private ExecutorService executor;


    private BlacklistHandlerState state = BlacklistHandlerState.builder().build();

    /** Constructor */
    public BlacklistHandlerImpl(String id, RuntimeConfig runtimeConfig, BlacklistHandlerConfig config) {
        super(id, runtimeConfig);
        this.config = config;
        this.logger = new LoggerUtil(id, HANDLER_ID, this.getClass());
        this.executor = ThreadUtils.getSingleThreadExecutorService("JclBlacklistHandler-Whitelist");
    }

    public void registerForEvents() {
        super.eventBus.subscribe(NetStartEvent.class, e -> this.onNetStart((NetStartEvent) e));;
        super.eventBus.subscribe(NetStopEvent.class, e -> this.onNetStop((NetStopEvent) e));
        super.eventBus.subscribe(PeerConnectedEvent.class, e -> this.onPeerConnected((PeerConnectedEvent) e));
        super.eventBus.subscribe(PeerRejectedEvent.class, e -> this.onPeerRejected((PeerRejectedEvent) e));
        super.eventBus.subscribe(PeerHandshakeRejectedEvent.class, e -> onPeerHandshakedRejected((PeerHandshakeRejectedEvent) e));
        super.eventBus.subscribe(PingPongFailedEvent.class, e -> onPingPongFailed((PingPongFailedEvent) e));
    }

    @Override
    public void init() {
        registerForEvents();

        // We load the list of blacklist hosts form disk
        loadBlacklistFromDisk();

        // Now that the list os loaded, we request a Blacklist event for them all...
        Map<InetAddress, PeersBlacklistedEvent.BlacklistReason> hostsToBlacklist = new HashMap<>();
        hostsInfo.keySet().forEach(ip -> hostsToBlacklist.put(ip, hostsInfo.get(ip).getBlacklistReason()));
        super.eventBus.publish(new PeersBlacklistedEvent(hostsToBlacklist));

        // We start the Job to look over the Blacklist IPs and whitelist them if needed:
        executor.submit(this::jobCheckBlacklist);
    }

    // Event Handler:
    private void onNetStart(NetStartEvent event) {
        logger.debug("Starting...");
    }

    // Event Handler:
    private void onNetStop(NetStopEvent event) {
        // We save the blacklist to disk...
        saveBlacklistToDisk();
        // We stop the Whitelist Job...
        if (executor != null) executor.shutdownNow();
        logger.debug("Stop.");
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


    private void loadBlacklistFromDisk() {
        String csvFileName = StringUtils.fileNamingFriendly(config.getBasicConfig().getId()) + FILE_BLACKLIST_SUFFIX;
        Path csvPath = Paths.get(runtimeConfig.getFileUtils().getRootPath().toString(), NET_FOLDER, csvFileName);
        if (Files.exists(csvPath)) {
            List<BlacklistHostInfo> hosts = runtimeConfig.getFileUtils().readCV(csvPath, () -> new BlacklistHostInfo());
            hosts.forEach(h -> hostsInfo.put(h.getIp(), h));
        }
    }

    private void saveBlacklistToDisk() {
        String csvFileName = StringUtils.fileNamingFriendly(config.getBasicConfig().getId()) + FILE_BLACKLIST_SUFFIX;
        Path csvPath = Paths.get(runtimeConfig.getFileUtils().getRootPath().toString(), NET_FOLDER, csvFileName);
        List<BlacklistHostInfo> hostsToSave = hostsInfo.values().stream().filter(h -> h.isBlacklisted()).collect(Collectors.toList());
        runtimeConfig.getFileUtils().writeCSV(csvPath, hostsToSave);
    }

    /**
     * It updates the State of this Handler
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

    /** It blacklists the Host given for the reason specified. */
    private void blacklist(BlacklistHostInfo hostInfo, PeersBlacklistedEvent.BlacklistReason reason) {
        logger.trace("Blacklisting " + hostInfo.getIp(), reason);
        hostInfo.blacklist(reason);
        updateState(reason);
    }

    /** It whitelists the Host given, making it eligible again for connection */
    private void whitelist(List<BlacklistHostInfo> hostInfos) {
        logger.trace("Whitelisting " + hostInfos.size() + " Ips...");
        hostInfos.forEach(h -> h.whitelist());
        // We publish the event to the Bus:
        super.eventBus.publish(new PeersWhitelistedEvent(hostInfos.stream().map(h -> h.getIp()).collect(Collectors.toList())));
    }

    /**
     * This method implements the business logic to determine whether this Peer should be blacklisted or not, based
     * on the status and the info stored for that Peer.
     *
     * first, it updates the BlacklistHostInfo object related to this Peer, executing the update Expression on it.
     * Then, it checks if the object (after being updated by the expresion) has any reason to be blacklisted, and if
     * so it gets blacklisted.
     *
     * @param peerAddress   Peer Address
     * @param updateExpr     An update expression that will be executed on the BlacklistHostInfo Object sotred for
     *                      this Peer.
     */
    private void processHostAndCheckBlacklisting(PeerAddress peerAddress, Consumer<BlacklistHostInfo> updateExpr) {
        InetAddress ip = peerAddress.getIp();
        BlacklistHostInfo hostInfo = hostsInfo.keySet().contains(ip) ? hostsInfo.get(ip) : new BlacklistHostInfo(ip);
        if (updateExpr != null) updateExpr.accept(hostInfo);

        // We add this Host to the Pool
        hostsInfo.put(ip, hostInfo);

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
     * This process looks constantly over the Balcklist and check if each individual IP canbe whitelisted
     * again.
     */
    private void jobCheckBlacklist() {
        try {

            while(true) {
                // First, we check if any of the Blacklisted Peers can be whitelisted...
                long numBlacklisted = hostsInfo.values().stream().filter( h -> h.isBlacklisted()).count();
                List<BlacklistHostInfo> hostsToWhitelist = hostsInfo.values().stream()
                        .filter(h -> h.isBlacklistExpired())
                        .collect(Collectors.toList());
                whitelist(hostsToWhitelist);
                long numBlacklistedAfter = hostsInfo.values().stream().filter( h -> h.isBlacklisted()).count();
                logger.debug( "Blacklist reviewed: " + (numBlacklisted - numBlacklistedAfter) + " IPs have been WHITELISTED.");

                // Now we publish those blacklisted peers that have not been published yet:
                // First we get them...
                Map<InetAddress, PeersBlacklistedEvent.BlacklistReason> hostsBlacklistedToPublish = new HashMap<>();
                hostsInfo.values().stream()
                        .filter(h -> h.isBlacklisted() && !h.isPublished())
                        .forEach(h -> hostsBlacklistedToPublish.put(h.getIp(), h.getBlacklistReason()));
                // Now we publish them...
                super.eventBus.publish(new PeersBlacklistedEvent(hostsBlacklistedToPublish));
                // And now we update their "published" state
                hostsBlacklistedToPublish.keySet().forEach(i -> hostsInfo.get(i).publish());

                logger.debug( "Publishing Blacklist of: " + hostsBlacklistedToPublish.size() + " IPs.");
                Thread.sleep(300000); // 5 minutes
            } // while..

        } catch(InterruptedException ie) {
            // In case of an interrupted exception we do nothing, since this will be caused most probably by this
            // handlers stopping (when it stops, it kills all the Threads it launched).
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
}

