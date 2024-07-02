package io.bitcoinsv.jcl.net.protocol.handlers.discovery;


import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.network.events.*;
import io.bitcoinsv.jcl.net.protocol.events.data.AddrMsgReceivedEvent;
import io.bitcoinsv.jcl.net.protocol.events.data.GetAddrMsgReceivedEvent;
import io.bitcoinsv.jcl.net.protocol.events.control.InitialPeersLoadedEvent;
import io.bitcoinsv.jcl.net.protocol.events.control.PeerHandshakedEvent;
import io.bitcoinsv.jcl.net.protocol.events.control.SendMsgRequest;
import io.bitcoinsv.jcl.net.protocol.messages.AddrMsg;
import io.bitcoinsv.jcl.net.protocol.messages.GetAddrMsg;
import io.bitcoinsv.jcl.net.protocol.messages.NetAddressMsg;
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsg;
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsgBuilder;
import io.bitcoinsv.jcl.tools.config.RuntimeConfig;
import io.bitcoinsv.jcl.tools.events.EventQueueProcessor;
import io.bitcoinsv.jcl.tools.handlers.HandlerImpl;
import io.bitcoinsv.jcl.net.tools.LoggerUtil;
import io.bitcoinsv.jcl.tools.thread.ThreadUtils;
import io.bitcoinsv.jcl.tools.util.DateTimeUtils;
import io.bitcoinsv.jcl.tools.util.StringUtils;

import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2024 nChain Ltd
 *
 * Implementation of the Discovery Handler, which implements the Node-Discovery protocol.
 * The Node Discovery P2P works like this:
 *  - We send a GET_ADDR, asking for Addresses
 *  - Once we get the ADDR message, we use its content to feed our pool of addresses. Over time, Connection requests
 *    will be issued for these Peers if needed.
 *
 * Apart from the basic behaviour, the main goal of this Handler is to keep the system with a Pool of Peer Address we
 * can use to connect ot them, so we always have a constant number of connections active at any time.
 *
 * - We keep a POOL of Peers. This Pool is used to keep track of all the Addresses (through GET_ADDR) that we receive
 *   from other Peers, and its also used to send out to other Peers (through ADDR)
 * - Every time a Peer is handshaked, we start the NodeDiscovery protocol for that peer.
 * -
 */
public class DiscoveryHandlerImpl extends HandlerImpl<PeerAddress, DiscoveryPeerInfo> implements DiscoveryHandler {

    // Suffix of the File that stores Peers from the Pool:
    private static final String NET_FOLDER = "net";
    private static final String FILE_POOL_SUFFIX = "-discovery-handler-hqPeers.csv";

    // P2P ADDR Max Content
    private static final int MAX_ADDR_ADDRESSES = 1000;
    // P2P ADDR Max Content for GETADDR response
    private static final int MAX_ADDR_ADDRESSES_GETADDR = 2500;

    private LoggerUtil logger;
    private DiscoveryHandlerConfig config;
    private ScheduledExecutorService executor;

    // We keep track of all the Peers that have been handshaked during this session, so we can try to
    // re-connect to them if we eventually run out of other Peers to try on...
    private Set<PeerAddress> peersHandshaked = ConcurrentHashMap.newKeySet();

    // Ww keep track of all the Peers that have been Blacklisted:
    private Set<InetAddress> peersBlacklisted = ConcurrentHashMap.newKeySet();

    /**
     * Holds peer addresses for which and outgoing connection was made.<br>
     * Primarily used here to determine whether we initiated a connection and need to self-advertise upon receiving a VERSION msg.
     */
    private Set<PeerAddress> peersOutgoing = ConcurrentHashMap.newKeySet();

    // TRUE when NetStopEvent is detected
    private boolean isStopping = false;
    // TRUE when a ResumeConnectingRequest is detected
    private boolean isAccceptingConnections = true;

    // State:
    private DiscoveryHandlerState state = DiscoveryHandlerState.builder().build();

    // The Events captured by this Handler will  e processed in a separate Thread/s, by an EventQueueProcessor, this
    // way we won't slow down the rate at whic the eVents are published and processed in the Bus
    private EventQueueProcessor eventQueueProcessor;

    /** Constructor */
    public DiscoveryHandlerImpl(String id, RuntimeConfig runtimeConfig, DiscoveryHandlerConfig config) {
        super(id, runtimeConfig);
        this.config = config;
        this.logger = new LoggerUtil(id, HANDLER_ID, this.getClass());
        this.executor = ThreadUtils.getSingleThreadScheduledExecutorService("JclDiscoveryHandler-Renew");

        // We start the EventQueueProcessor. We do not expect many messages (compared to the rest of traffic), so a
        // single Thread will do...
        this.eventQueueProcessor = new EventQueueProcessor("JclDiscoveryHandler", ThreadUtils.getSingleThreadScheduledExecutorService("JclDiscoveryHandler-EventsConsumers"));
    }

    @Override
    public void init() {
        registerForEvents();

        // We schedule the Job to renew the Addresses...
        if (config.getADDRFrequency().isPresent())
            executor.scheduleAtFixedRate(this::jobRenewAddresses,
                    config.getADDRFrequency().get().toMillis(),
                    config.getADDRFrequency().get().toMillis(), TimeUnit.MILLISECONDS);

        // We schedule the Job to re-connect the Lost Handshaked Peers:
        if (config.getRecoveryHandshakeFrequency().isPresent()
            && config.getRecoveryHandshakeThreshold().isPresent()) {
            logger.debug("Scheduling job to renew once-handshaked peers every "
                    + config.getRecoveryHandshakeFrequency().get().toSeconds() + " seconds."
                    + " Every Peer disconnected after " + config.getRecoveryHandshakeThreshold().get().toSeconds()
                    + " seconds will be re-connected again). ");
            executor.scheduleAtFixedRate(this::jobRenewLostHandshakedPeers,
                    config.getRecoveryHandshakeFrequency().get().toMillis(),
                    config.getRecoveryHandshakeFrequency().get().toMillis(),
                    TimeUnit.MILLISECONDS);
        }

        if (isSelfAdvertisementAvailable(true)) {
            var frequencyPeriod = config.getAddrBroadcastFrequency().get();

            logger.debug(String.format("Scheduling job to self-advertise (as ADDR message broadcast) every %s seconds.", frequencyPeriod.toSeconds()));
            executor.scheduleAtFixedRate(this::jobBroadcastOwnAddrMsg,
                    frequencyPeriod.toMillis(),
                    frequencyPeriod.toMillis(), TimeUnit.MILLISECONDS);
        } else {
            logger.warm("Cannot schedule self-advertisement job - unsatisfied configuration");
        }
    }

    private void registerForEvents() {

        this.eventQueueProcessor.addProcessor(NetStartEvent.class, e -> this.onStart((NetStartEvent)e));
        this.eventQueueProcessor.addProcessor(NetStopEvent.class, e -> this.onStop((NetStopEvent)e));
        this.eventQueueProcessor.addProcessor(PeerHandshakedEvent.class, e -> this.onPeerHandshaked((PeerHandshakedEvent) e));
        this.eventQueueProcessor.addProcessor(PeerConnectedEvent.class, e -> this.onPeerConnected((PeerConnectedEvent) e));
        this.eventQueueProcessor.addProcessor(PeersBlacklistedEvent.class, e -> this.onPeerBlacklisted((PeersBlacklistedEvent) e));
        this.eventQueueProcessor.addProcessor(PeerDisconnectedEvent.class, e -> this.onPeerDisconnected((PeerDisconnectedEvent) e));
        this.eventQueueProcessor.addProcessor(GetAddrMsgReceivedEvent.class, e -> this.onGetAddrMsg((GetAddrMsgReceivedEvent) e));
        this.eventQueueProcessor.addProcessor(AddrMsgReceivedEvent.class, e -> this.onAddrMsg((AddrMsgReceivedEvent) e));
        this.eventQueueProcessor.addProcessor(ResumeConnectingRequest.class, e -> this.onResumeConnecting((ResumeConnectingRequest) e));
        this.eventQueueProcessor.addProcessor(StopConnectingRequest.class, e -> this.onStopConnecting((StopConnectingRequest) e));

        super.eventBus.subscribe(NetStartEvent.class, e -> this.eventQueueProcessor.addEvent(e));
        super.eventBus.subscribe(NetStopEvent.class, e -> this.eventQueueProcessor.addEvent(e));
        super.eventBus.subscribe(PeerHandshakedEvent.class, e -> this.eventQueueProcessor.addEvent(e));
        super.eventBus.subscribe(PeerConnectedEvent.class, e -> this.eventQueueProcessor.addEvent(e));
        super.eventBus.subscribe(PeersBlacklistedEvent.class, e -> this.eventQueueProcessor.addEvent(e));
        super.eventBus.subscribe(PeerDisconnectedEvent.class, e -> this.eventQueueProcessor.addEvent(e));
        super.eventBus.subscribe(GetAddrMsgReceivedEvent.class, e -> this.eventQueueProcessor.addEvent(e));
        super.eventBus.subscribe(AddrMsgReceivedEvent.class, e -> this.eventQueueProcessor.addEvent(e));
        super.eventBus.subscribe(ResumeConnectingRequest.class, e -> this.eventQueueProcessor.addEvent(e));
        super.eventBus.subscribe(StopConnectingRequest.class, e -> this.eventQueueProcessor.addEvent(e));

        this.eventQueueProcessor.start();
    }

    /**
     * It feeds the Pool of Addresses with 2 sources:
     *  - the initial Set of Peers returned by a InitialPeersFinder
     *  - the peers stored on disk (if any) from  the Pool from previous executions.
     */
    private void initPool() {
        logger.debug("Loading Pool...");
        try {
            // We load the initial set of Peers. Depending on the Configuration, we'll have different methods to
            // load the initial set of Peers. This logic is implemented by the "PeerFinder".

            InitialPeersFinder peersFinder = config.getDiscoveryMethod().equals(DiscoveryHandlerConfig.DiscoveryMethod.DNS)
                    ? new InitialPeersFinderSeed(this.config)
                    : new InitialPeersFinderCSV(super.runtimeConfig.getFileUtils(), this.config);
            logger.debug("Loading Pool with Peers Finder: " + peersFinder.getClass().getSimpleName() + "...");
            List<PeerAddress> initialPeers = peersFinder.findPeers();

            // We trigger the Event:
            super.eventBus.publish(new InitialPeersLoadedEvent(initialPeers.size(), config.getDiscoveryMethod()));
            logger.debug(initialPeers.size() + " peers found.");

            // Now we load the Peers from the POOL from previous execution, stored in a CSV File:
            List<PeerAddress> poolPeers = new ArrayList<>();
            String csvFileName = StringUtils.fileNamingFriendly(config.getBasicConfig().getId()) + FILE_POOL_SUFFIX;
            Path csvPath = Paths.get(runtimeConfig.getFileUtils().getRootPath().toString(), NET_FOLDER, csvFileName);
            logger.debug("looking for High Quality Peers file in: " + csvPath.toString());
            if (Files.exists(csvPath)) {
                poolPeers = runtimeConfig.getFileUtils().readCV(csvPath, () -> new DiscoveryPeerInfo()).stream()
                        .map(d -> d.getPeerAddress())
                        .collect(toList());
                logger.debug(poolPeers.size() + " peers loaded from file.");
            } else logger.debug(" No file found.");

            // We also check if some initial Connections have been defined directly in the Configuration:
            List<PeerAddress> initialConnections = config.getInitialConnections();

            // we put them both all into the Main Pool:
            List<PeerAddress> peersToConnect = new ArrayList<>();
            peersToConnect.addAll(initialConnections);
            peersToConnect.addAll(initialPeers);
            peersToConnect.addAll(poolPeers);

            // We register all of them for connection:
            super.eventBus.publish(new ConnectPeersRequest(peersToConnect));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * It saves the high quality Peers form our ool into a CSV file, so it can be used to load up the Pool on startup
     * in future executions.
     * NOTE: A high-Quality Peer is a Peer that is in our Pool and has ever been handshaked before.
     */
    private void savePoolToDisk() {
        List<DiscoveryPeerInfo> hqPeers = handlerInfo.values().stream()
                .filter(p -> peersHandshaked.contains(p.getPeerAddress()))
                .collect(toList());
        String csvFileName = StringUtils.fileNamingFriendly(config.getBasicConfig().getId()) + FILE_POOL_SUFFIX;
        Path csvPath = Paths.get(runtimeConfig.getFileUtils().getRootPath().toString(), NET_FOLDER, csvFileName);
        super.runtimeConfig.getFileUtils().writeCSV(csvPath, hqPeers);
    }

    // Event Handler:
    public void onStart(NetStartEvent event) {
        logger.trace("Starting...");
        initPool();
    }

    // Event Handler:
    public void onStop(NetStopEvent event) {
        logger.debug("Saving High-Quality Peers to disk...");
        savePoolToDisk();

        // We stop the concurrent Jobs...
        if (executor != null) executor.shutdownNow();

        // We stop the EventQueueProcessor...
        this.eventQueueProcessor.stop();

        logger.trace("Stop.");
        isStopping = true;
    }
    // Event Handler:
    public void onPeerHandshaked(PeerHandshakedEvent event) {
        // This peer might or not be in the Main pool.
        // In case it's not, we first try to add it. If we couldn't add it (maybe because the main Pool is
        // already full, we try to replace it for other Peer that is not handshaked

        PeerAddress peerAddress = event.getPeerAddress();
        // We add it to our "historic" of handshaked peers:
        peersHandshaked.add(peerAddress);

        // Now we add this Peer to our Pool. If it's not possible to Add (most probably because the Pool has already
        // reached the maximum size), we still try to add it, since a handshaked Peer is a high-quality Peer that we
        // want to keep. so in that case, we look for another Peer within the Peer we might replace (like a Peer in
        // the pool that is NOT handshaked)

        DiscoveryPeerInfo peerInfo = handlerInfo.get(peerAddress);
        if (peerInfo == null) {
            peerInfo = new DiscoveryPeerInfo(peerAddress, System.currentTimeMillis(), event.getVersionMsg().getServices());
            boolean addedOK = addToPool(peerInfo);
            if (!addedOK) {
                Optional<PeerAddress> peerAddressToReplace = handlerInfo.values().stream()
                        .filter(p ->  !peersHandshaked.contains(p)).findFirst().map(p -> p.getPeerAddress());
                if (peerAddressToReplace.isPresent()) {
                    logger.trace( peerAddressToReplace.get().toString(),
                            "Removing this Peer from the main pool, to make room for  " + peerAddress.toString() + "...");
                    handlerInfo.remove(peerAddressToReplace.get());
                    handlerInfo.put(peerAddress, peerInfo);
                }
            }
        }

        peerInfo.updateHandshake(event.getVersionMsg());
        logger.trace(peerAddress, "Handshaked Peer added to the Pool", "(" + handlerInfo.size() + " peers currently in pool)");

        // at this point, the version msg appears to be accepted - now try to advertise own address to the node
        // conditions: self-advertisement must be configured and we started the peer connection
        if (isSelfAdvertisementAvailable(false) && peersOutgoing.contains(peerAddress)) {
            advertiseOwnAddrMsg(peerAddress);
        }
        // We start the Discovery protocol....
        if (!isStopping && isAccceptingConnections) startDiscovery(peerInfo);

    }

    /**
     * Convenient listen method for {@link PeerConnectedEvent}s that hold information about whether a connection is
     * outgoing or incoming.
     *
     * @param event {@link PeerConnectedEvent} to inspect
     */
    public void onPeerConnected(PeerConnectedEvent event) {
        if (event.isOutgoingConnection()) {
            peersOutgoing.add(event.getPeerAddress());
        }
    }

    // Event Handler:
    public void onPeerBlacklisted(PeersBlacklistedEvent event) {
        this.peersBlacklisted.addAll(event.getInetAddresses().keySet());

        // We remove from the Pool all the Peers using this IP:
        List<PeerAddress> toRemoveFromMainPool = handlerInfo.keySet().stream()
                .filter(p -> event.getInetAddresses().keySet().contains(p.getIp()))
                .collect(toList());

        for (PeerAddress peerAddress : toRemoveFromMainPool) {
            removeFromPool(peerAddress);
        }

    }
    // Event Handler:
    public void onPeerDisconnected(PeerDisconnectedEvent event) {
        DiscoveryPeerInfo peerInfo = handlerInfo.get(event.getPeerAddress());
        if (peerInfo != null) peerInfo.reset();
    }
    // Event Handler:
    public void onResumeConnecting(ResumeConnectingRequest event) {
        this.isAccceptingConnections = true;
    }
    // Event Handler:
    public void onStopConnecting(StopConnectingRequest event) {
        this.isAccceptingConnections = false;
    }

    // Event Handler:
    /**
     * It process the incoming GETADDR Message,a according to the Rules defined in the Bitcoin P2P.
     * The business logic of processing incoming GETADDR is implemented as it's described in the Satoshi client:
     * <a href="https://en.bitcoin.it/wiki/Satoshi_Client_Node_Discovery">Satoshi Client Node Discovery</a>,
     * section {@code Handling Message "getaddr"}.
     */
    private void onGetAddrMsg(GetAddrMsgReceivedEvent event) {
        PeerAddress peerAddress = event.getPeerAddress();

        if (!peersHandshaked.contains(event.getPeerAddress())) return; //only prepare/send ADDR if peer is handshaked

        logger.debug(peerAddress, "Processing incoming GET_ADDR...");
        // We check that we have enough of them to send them out:
        if (config.getRelayMinAddresses().isPresent() && (handlerInfo.size() < config.getRelayMinAddresses().getAsInt())) {
            logger.warm(peerAddress, "GETADDR Ignored (not enough Addresses to send");
            return;
        }

        // If we reach this far, we prepare the ADDR Message in reply to this GET_ADDR and send it out:
        // We only add those Addr which Timestamps is within the last 3 Hours.
        // List of addresses:
        List<NetAddressMsg> netAddressMsgs = new ArrayList<>();
        //we are interested in recent peer/addr infos - only those from last 3 hours
        List<DiscoveryPeerInfo> recentAddrInfos = handlerInfo.values().stream()
            .filter(addrInfo -> addrInfo.getTimestamp() >= System.currentTimeMillis() - Duration.ofHours(3).toMillis())
            .collect(Collectors.toList());

        //if the max addr addresses limit (for GETADDR response) is exceeded, we use the limit and randomly select recent addresses
        if (recentAddrInfos.size() > MAX_ADDR_ADDRESSES_GETADDR) {
            Collections.shuffle(recentAddrInfos);
            recentAddrInfos = recentAddrInfos.subList(0, MAX_ADDR_ADDRESSES_GETADDR - 1);
        }

        recentAddrInfos.forEach(addrinfo -> netAddressMsgs.add(
            NetAddressMsg.builder()
                .address(addrinfo.getPeerAddress())
                .timestamp(Instant.ofEpochMilli(addrinfo.getTimestamp()).getEpochSecond()) //note: must be in epoch seconds!
                .services(addrinfo.getServices())
                .build()));

        // We only send the Message if we have collected a minimum Set of Addresses.
        if ((config.getRelayMinAddresses().isEmpty()) ||
                (netAddressMsgs.size() >= (config.getRelayMinAddresses().getAsInt()))) {
            AddrMsg addrMsg = AddrMsg.builder().addrList(netAddressMsgs).build();
            BitcoinMsg<AddrMsg> btcMsg = new BitcoinMsgBuilder<>(config.getBasicConfig(), addrMsg).build();
            super.eventBus.publish(new SendMsgRequest(peerAddress, btcMsg));
        }
    }

    // Event Handler:
    /**
     * It process the incoming ADDR Message, according to the Rules defined in the Bitcoin P2P.
     * The business logic of processing incoming ADDR is implemented as it's described in the Satoshi client:
     * https://en.bitcoin.it/wiki/Satoshi_Client_Node_Discovery#Ongoing_.22addr.22_advertisements
     */
    private void onAddrMsg(AddrMsgReceivedEvent event) {
        try {
            DiscoveryPeerInfo peerInfo = getOrWaitForHandlerInfo(event.getPeerAddress());
            if (peerInfo == null) return;

            AddrMsg msg = event.getBtcMsg().getBody();
            logger.trace(peerInfo.getPeerAddress(), "Processing incoming ADDR [" + msg.getAddrList().size() + " addresses]...");

            // Should never happen!!
            if (peerInfo == null) {
                logger.warm(peerInfo.getPeerAddress(), "ADDR Message coming from a Peer not registered in the Main Pool!. Discarding...");
                return;
            }

            // Should never happen!!!
            if (!peerInfo.isHandshaked()) {
                logger.warm(peerInfo.getPeerAddress(), "ADDR Message coming from a Peer not Handshaked in the Main Pool!. Discarding...");
                return;
            }

            // We update the State:
            updateState(0,0,0,0,1, (int) msg.getCount().getValue());

            // Check: Max entries in the ADDR MSG
            if (config.getMaxAddresses().isPresent() && (msg.getCount().getValue() > config.getMaxAddresses().getAsInt())) {
                logger.trace(peerInfo.getPeerAddress(), "Ignoring ADDR (too many in the Message)");
                return;
            }

            // Check: P2P used by the client is too low
            if (config.getMinVersion().isPresent() && peerInfo.getVersionMsg().getVersion() < config.getMinVersion().getAsInt()) {
                logger.trace(peerInfo.getPeerAddress(), "Ignoring ADDR (connection Version too low)");
                return;
            }

            // If we reach this far, we add the Addresses to the Main Pool and request a connection Request:
            List<PeerAddress> peersToConnect = new ArrayList<>();
            for (NetAddressMsg netAddressMsg : msg.getAddrList()) {
                DiscoveryPeerInfo addPeerInfo = new DiscoveryPeerInfo(
                    netAddressMsg.getAddress(),
                    //note: timestamp in NetAddressMsg arrives in epoch seconds! We must first convert it to epoch milliseconds to avoid comparison errors later on!
                    Instant.ofEpochSecond(netAddressMsg.getTimestamp()).toEpochMilli(),
                    netAddressMsg.getServices());
                addToPool(addPeerInfo);
                peersToConnect.add(addPeerInfo.getPeerAddress());
            }
            logger.debug(peerInfo.getPeerAddress(), msg.getCount().getValue() + " addresses received in ADDR, " + peersToConnect.size() + " added to the Pool...");
            super.eventBus.publish(new ConnectPeersRequest(peersToConnect));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * It Starts the Discovery P2P, sending a GETADDR Message to this Remote Peer
     */
    private void startDiscovery(DiscoveryPeerInfo peerInfo) {
        logger.debug(peerInfo.getPeerAddress(), "Starting Node Discovery...");

        // We Request to send a GET_ADDR Message
        GetAddrMsg getAddrMsg = GetAddrMsg.builder().build();
        BitcoinMsg<GetAddrMsg> btcMsg = new BitcoinMsgBuilder<>(config.getBasicConfig(), getAddrMsg).build();
        super.eventBus.publish(new SendMsgRequest(peerInfo.getPeerAddress(), btcMsg));

        // We update the State:
        updateState(0,0,0,1,0, null);
    }

    // Update the State of this Handler
    private synchronized void updateState(int addedToPool, int removedFromPool, int rejectedFromPool,
                                          int getAddrMsgsSent, int addrMsgsReceived,
                                          Integer numAddressInNewADDRMsg) {

        DiscoveryHandlerState.DiscoveryHandlerStateBuilder builder = this.state.toBuilder();
        builder.poolSize(this.handlerInfo.size())
               .numNodesAdded(state.getNumNodesAdded() + addedToPool)
               .numNodesRemoved(state.getNumNodesRemoved() + removedFromPool)
               .numNodesRejected(state.getNumNodesRejected() + rejectedFromPool)
               .numGetAddrMsgsSent(state.getNumGetAddrMsgsSent() + getAddrMsgsSent)
               .numAddrMsgsReceived(state.getNumAddrMsgsReceived() + addrMsgsReceived);

        if (numAddressInNewADDRMsg != null) {
            Map<Integer, Integer> addrMsgsSize = state.getAddrMsgsSize();
            addrMsgsSize.merge(numAddressInNewADDRMsg, 1, (v1, v2) -> Math.max(v1, v2) + 1);
            builder.addrMsgsSize(addrMsgsSize);
        }
        this.state = builder.build();
    }

    /**
     * Adds a new Address + Timestamp to the MAIN POOL.
     * It performs some changes on the timestamp, based on the Satoshi client Implementation.
     * Some verifications are also performed, the Peer is only added when it meets them all. The result of this method
     * will indicate whether the Peer has been successfully added.
     */
    private boolean addToPool(DiscoveryPeerInfo peerInfo) {
        boolean result = true;

        PeerAddress peerAddress = peerInfo.getPeerAddress();
        Long timestamp = peerInfo.getTimestamp();

        // If the Peer is already included, we quit:
        if (handlerInfo.containsKey(peerInfo.getPeerAddress())) result =  false;

        // If it's blacklisted, we discard it:
        if (peersBlacklisted.contains(peerInfo.getPeerAddress().getIp())) result =  false;

        // If the main Pool is already full, we discard it:

        if (config.getMaxAddresses().isPresent() && handlerInfo.size() >= config.getMaxAddresses().getAsInt())
            result =  false;

        // Finally, we either add it to the mainPool or not:
        if (result) {
            // We update the Status:
            updateState(1, 0, 0, 0, 0,null);

            // We perform some changes on the timestamp according to the rules specified in the Satoshi client:
            // https://en.bitcoin.it/wiki/Satoshi_Client_Node_Discovery#Ongoing_.22addr.22_advertisements
            // - If the timestamp is too low or too high, it is set to 5 days ago.
            // - We subtract 2 hours from the timestamp and add the address.
            // - If the address has been seen in the last 24 hours and the timestamp is currently over 60 minutes old,
            //   then it is updated to 60 minutes ago
            // - If the address has NOT been seen in the last 24 hours, and the timestamp is currently over 24 hours
            //   old, then it is updated to 24 hours ago.

            Long now = System.currentTimeMillis();
            Long last5Days = now - Duration.ofDays(5).toMillis();
            Long lastDay = now - Duration.ofDays(1).toMillis();
            Long lastHour = now - Duration.ofHours(1).toMillis();

            if ((timestamp > now) || (timestamp < last5Days)) timestamp = last5Days;
            timestamp = timestamp - Duration.ofHours(2).toMillis();
            if ((timestamp >= lastDay) && (timestamp < lastHour)) timestamp = lastHour;
            if (timestamp < lastDay) timestamp = lastDay;

            // We update and put the Address in the MAIN POOL
            peerInfo.updateTimestamp(timestamp);
            handlerInfo.put(peerAddress, peerInfo);

        } else {
            // We update the Status:
            updateState(0, 0, 1, 0,0,null);
        }

        return result;
    }

    private void removeFromPool(PeerAddress peerAddress) {
        boolean actuallyRemoved = (handlerInfo.remove(peerAddress) != null);
        if (actuallyRemoved) updateState(0,1,0,0,0,null);
    }

    /**
     * This methods runs on a schedule basis. Its goal is to refresh our main pool of addresses from time to time, so
     * we can be sure that the Connection and Network Handlers always have Peers to connect to, in case they might
     * run out of them...
     */
    private void jobRenewAddresses() {
        try {
            logger.debug("Renewing Pool of Addresses...");
            List<DiscoveryPeerInfo> peersToAsk = this.handlerInfo.values().stream()
                    .filter( p -> peersHandshaked.contains(p.getPeerAddress()))
                    .filter( p -> (new Random().nextInt(100) <= config.getADDRPercentage().getAsInt()))
                    .collect(toList());
            if (peersToAsk != null && peersToAsk.size() > 0) {
                logger.debug("Renewing Pool of Address, asking " + peersToAsk.size() + " peers for new Addresses...");
                Collections.shuffle(peersToAsk);
                peersToAsk.forEach( p -> this.startDiscovery(p));
            } else logger.debug("Impossible to Renew Addresses, Main pool is empty");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
        }
    }

    /**
     * This method runs on a schedule basis. Its goal is to check on those Peers that were handshaked but their connection
     * was lost.
     */
    private void jobRenewLostHandshakedPeers() {
        logger.debug( "Trying to recover connections from Lost Handshakes...");
        try {
            // This is the point in time
            Duration waitingDuration = config.getRecoveryHandshakeThreshold().get();
            List<DiscoveryPeerInfo> handshakesToRecover = handlerInfo.values().stream()
                    .filter(p -> !p.isHandshaked())
                    .filter(p -> p.getLastHandshakeTime() != null)
                    .filter(p -> peersHandshaked.contains(p.getPeerAddress()))
                    .filter(p -> Duration.between(p.getLastHandshakeTime(), DateTimeUtils.nowDateTimeUTC()).compareTo(waitingDuration) > 0)
                    .collect(toList());

            // For each of them we request a connection:
            handshakesToRecover.forEach(p -> super.eventBus.publish(new ConnectPeerRequest(p.getPeerAddress())));
            logger.debug( "Recovering handshake with " + handshakesToRecover.size()
                    +  " peers, " + (peersHandshaked.size() - handshakesToRecover.size())  + " still lost...");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
        } catch (Throwable th) {
            logger.error(th.getMessage(), th);
            th.printStackTrace();
        }
    }

    public DiscoveryHandlerConfig getConfig() {
        return this.config;
    }

    public DiscoveryHandlerState getState() {
        return this.state;
    }

    /**
     * Performs a self-advertisement to all handshaked peers.
     */
    private void jobBroadcastOwnAddrMsg() {
        peersHandshaked.forEach(this::advertiseOwnAddrMsg);
    }

    /**
     * Advertises own address in a form of {@link AddrMsg} to the target peer.<br>
     * If the advertised IP is not configured, a warning will be logged instead. Additionally, if the advertised IP
     * is misconfigured, the {@link PeerAddress#fromIp(String)} method will raise an exception which will be logged - if
     * this happens, no self-advertisement is done.
     *
     * @param peerAddress Target {@link PeerAddress} to send the self-advertisement to
     */
    private void advertiseOwnAddrMsg(PeerAddress peerAddress) {
        Optional<String> advertisedIp = config.getAdvertisedIp();

        try {
            logger.debug("Broadcasting own addr to peer...", peerAddress);
            NetAddressMsg netAddrMsg = NetAddressMsg.builder()
                .address(PeerAddress.fromIp(advertisedIp.get()))
                .services(config.getAdvertisedServices().get())
                .timestamp(Instant.now().getEpochSecond()) //note: epoch seconds!
                .build();
            AddrMsg addrMsg = AddrMsg.builder()
                .addrList(List.of(netAddrMsg))
                .build();
            BitcoinMsg<AddrMsg> btcMsg = new BitcoinMsgBuilder<>(config.getBasicConfig(), addrMsg).build();

            super.eventBus.publish(new SendMsgRequest(peerAddress, btcMsg));
        } catch (Exception e) {
            logger.error(e, e.getMessage());
        }
    }

    /**
     * Checks if self-advertisement is available/allowed. The following conditions are checked:
     * <ul>
     *     <li>Advertised IP is configured</li>
     *     <li>Advertised IP is NOT blank</li>
     *     <li>(with doCheckBroadcastFrequency) Broadcast frequency is configured</li>
     *     <li>(with doCheckBroadcastFrequency) Broadcast frequency is greater than 0 seconds</li>
     * </ul>
     *
     * @param doCheckBroadcastFrequency Flag that determines if broadcast frequency should be included in the check
     * @return {@code true} if self-advertisement is available according to the config, otherwise {@code false}
     */
    private boolean isSelfAdvertisementAvailable(boolean doCheckBroadcastFrequency) {
        var advertisedIp = config.getAdvertisedIp();
        var isAdvertisementAllowed = advertisedIp.isPresent(); //primary condition: advertised IP value must be present first

        if (isAdvertisementAllowed) { //if the advertised IP value is present, we want to ensure the value is not a blank string
            isAdvertisementAllowed = !advertisedIp.get().isBlank();
        } else {
            logger.warm("Self-advertisement not possible - advertised address is not configured or missing");
        }
        if (doCheckBroadcastFrequency) {
            var addrBroadcastFrequency = config.getAddrBroadcastFrequency();

            return isAdvertisementAllowed && addrBroadcastFrequency.isPresent() && addrBroadcastFrequency.get().toSeconds() != 0L;
        }

        return isAdvertisementAllowed;
    }
}
