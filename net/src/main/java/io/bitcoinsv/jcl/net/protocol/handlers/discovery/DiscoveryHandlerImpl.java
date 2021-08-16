/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
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
import io.bitcoinsv.jcl.tools.log.LoggerUtil;
import io.bitcoinsv.jcl.tools.thread.ThreadUtils;
import io.bitcoinsv.jcl.tools.util.DateTimeUtils;
import io.bitcoinsv.jcl.tools.util.StringUtils;

import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
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

    private LoggerUtil logger;
    private DiscoveryHandlerConfig config;
    private ScheduledExecutorService executor;

    // We keep track of all the Peers that have been handshaked during this session, so we can try to
    // re-connect to them if we eventually run out of other Peers to try on...
    private Set<PeerAddress> peersHandshaked = ConcurrentHashMap.newKeySet();

    // Ww keep track of all the Peers that have been Blacklisted:
    private Set<InetAddress> peersBlacklisted = ConcurrentHashMap.newKeySet();

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
            executor.scheduleAtFixedRate(this::jobRenewLostHandshakedPeers,
                    config.getRecoveryHandshakeFrequency().get().toMillis(),
                    config.getRecoveryHandshakeFrequency().get().toMillis(),
                    TimeUnit.MILLISECONDS);
        }


    }

    private void registerForEvents() {

        this.eventQueueProcessor.addProcessor(NetStartEvent.class, e -> this.onStart((NetStartEvent)e));
        this.eventQueueProcessor.addProcessor(NetStopEvent.class, e -> this.onStop((NetStopEvent)e));
        this.eventQueueProcessor.addProcessor(PeerHandshakedEvent.class, e -> this.onPeerHandshaked((PeerHandshakedEvent) e));
        this.eventQueueProcessor.addProcessor(PeersBlacklistedEvent.class, e -> this.onPeerBlacklisted((PeersBlacklistedEvent) e));
        this.eventQueueProcessor.addProcessor(PeerDisconnectedEvent.class, e -> this.onPeerDisconnected((PeerDisconnectedEvent) e));
        this.eventQueueProcessor.addProcessor(GetAddrMsgReceivedEvent.class, e -> this.onGetAddrMsg((GetAddrMsgReceivedEvent) e));
        this.eventQueueProcessor.addProcessor(AddrMsgReceivedEvent.class, e -> this.onAddrMsg((AddrMsgReceivedEvent) e));
        this.eventQueueProcessor.addProcessor(ResumeConnectingRequest.class, e -> this.onResumeConnecting((ResumeConnectingRequest) e));
        this.eventQueueProcessor.addProcessor(StopConnectingRequest.class, e -> this.onStopConnecting((StopConnectingRequest) e));

        super.eventBus.subscribe(NetStartEvent.class, e -> this.eventQueueProcessor.addEvent(e));
        super.eventBus.subscribe(NetStopEvent.class, e -> this.eventQueueProcessor.addEvent(e));
        super.eventBus.subscribe(PeerHandshakedEvent.class, e -> this.eventQueueProcessor.addEvent(e));
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
            List<DiscoveryPeerInfo> initialPeers = peersFinder.findPeers().stream()
                    .map(p -> new DiscoveryPeerInfo(p))
                    .collect(Collectors.toList());
            // We trigger the Event:
            super.eventBus.publish(new InitialPeersLoadedEvent(initialPeers.size(), config.getDiscoveryMethod()));
            logger.debug(initialPeers.size() + " peers found.");

            // Now we load the Peers from the POOL from previous execution, stored in a CSV File:
            logger.debug("Loading High-Quality Peers from file...");
            List<DiscoveryPeerInfo> poolPeers = new ArrayList<>();
            String csvFileName = StringUtils.fileNamingFriendly(config.getBasicConfig().getId()) + FILE_POOL_SUFFIX;
            Path csvPath = Paths.get(runtimeConfig.getFileUtils().getRootPath().toString(), NET_FOLDER, csvFileName);
            logger.debug("looking for High Quality Peers file in: " + csvPath.toString());
            if (Files.exists(csvPath)) {
                poolPeers = runtimeConfig.getFileUtils().readCV(csvPath, () -> new DiscoveryPeerInfo());
                logger.debug(poolPeers.size() + " peers loaded from file.");
            } else logger.debug(" No file found.");

            // we put them both all into the Main Pool:
            List<DiscoveryPeerInfo> totalPeers = new ArrayList<>();
            totalPeers.addAll(initialPeers);
            totalPeers.addAll(poolPeers);

            // We register all of them for connection:
            List<PeerAddress> peersToConnect = totalPeers.stream().map(p -> p.getPeerAddress()).collect(Collectors.toList());
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
                .collect(Collectors.toList());
        String csvFileName = StringUtils.fileNamingFriendly(config.getBasicConfig().getId()) + FILE_POOL_SUFFIX;
        Path csvPath = Paths.get(runtimeConfig.getFileUtils().getRootPath().toString(), NET_FOLDER, csvFileName);
        super.runtimeConfig.getFileUtils().writeCSV(csvPath, hqPeers);
    }

    // Event Handler:
    public void onStart(NetStartEvent event) {
        logger.debug("Starting...");
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

        logger.debug("Stop.");
        isStopping = true;
    }
    // Event Handler:
    public void onPeerHandshaked(PeerHandshakedEvent event) {
        // This peer might or not be in the Main pool.
        // In case it's not, we first try to addBytes it. If we couldn't addBytes it (maybe because the main Pool is
        // already full, we try to replace it for other Peer that is not handshaked

        PeerAddress peerAddress = event.getPeerAddress();
        // We addBytes it to our "historic" of handshaked peers:
        peersHandshaked.add(peerAddress);

        // Now we addBytes this Peer to our Pool. If it's not possible to Add (most probably because the Pool has already
        // reached the maximum size), we still try to addBytes it, since a handshaked Peer is a high-quality Peer that we
        // want to keep. so in that case, we look for another Peer within the Peer we might replace (like a Peer in
        // the pool that is NOT handshaked)

        DiscoveryPeerInfo peerInfo = handlerInfo.get(peerAddress);
        if (peerInfo == null) {
            peerInfo = new DiscoveryPeerInfo(peerAddress);
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
        logger.trace(peerAddress, "Handshaked Peer added to the Pool (version:" + peerInfo.getVersionMsg().getVersion() + ")");

        // We start the Discovery protocol....
        if (!isStopping && isAccceptingConnections) startDiscovery(peerInfo);

    }
    // Event Handler:
    public void onPeerBlacklisted(PeersBlacklistedEvent event) {
        this.peersBlacklisted.addAll(event.getInetAddresses().keySet());

        // We remove from the Pool all the Peers using this IP:
        List<PeerAddress> toRemoveFromMainPool = handlerInfo.keySet().stream()
                .filter(p -> event.getInetAddresses().keySet().contains(p.getIp()))
                .collect(Collectors.toList());

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
     * The business logic of processing incoming ADDR is implemented as it's described in the Satoshi client:
     * https://en.bitcoin.it/wiki/Satoshi_Client_Node_Discovery#Ongoing_.22addr.22_advertisements
     */
    private void onGetAddrMsg(GetAddrMsgReceivedEvent event) {

        DiscoveryPeerInfo peerInfo = getOrWaitForHandlerInfo(event.getPeerAddress());
        if (peerInfo == null) return;
        logger.debug(peerInfo.getPeerAddress().toString() + " :: Processing incoming GET_ADDR...");
        // We check that we have enough of them to send them out:
        if (config.getRelayMinAddresses().isPresent() && (handlerInfo.size() > config.getRelayMinAddresses().getAsInt())) {
            logger.debug(peerInfo.getPeerAddress(), "GETADDR Ignored (not enough Addresses to send");
            return;
        }

        // If we reach this far, we prepare the ADDR Message in reply to this GET_ADDR and send it out:
        // We only addBytes those Addr which Timestamps is within the last Hour.
        // List of addresses:
        List<NetAddressMsg> netAddressMsgs = new ArrayList<NetAddressMsg>() ;

        int numAddrToAdd = 0;
        NetAddressMsg netAddrMsg;
        for (DiscoveryPeerInfo addrInfo : handlerInfo.values()) {
            Long within1Hour = System.currentTimeMillis() - Duration.ofHours(1).toMillis();

            if (addrInfo.getTimestamp() >= within1Hour && netAddressMsgs.size() < MAX_ADDR_ADDRESSES) {
                netAddrMsg = NetAddressMsg.builder().address(addrInfo.getPeerAddress()).timestamp(addrInfo.getTimestamp()).build();
                netAddressMsgs.add(netAddrMsg);
                numAddrToAdd++;
            }
        }

        // We only send the Message if we have collected a minimum Set of Addresses.
        if ((config.getRelayMinAddresses().isEmpty()) ||
                (numAddrToAdd >= (config.getRelayMinAddresses().getAsInt()))) {
            AddrMsg addrMsg = AddrMsg.builder().addrList(netAddressMsgs).build();
            BitcoinMsg<AddrMsg> btcMsg = new BitcoinMsgBuilder<>(config.getBasicConfig(), addrMsg).build();
            super.eventBus.publish(new SendMsgRequest(peerInfo.getPeerAddress(), btcMsg));
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
            logger.debug(peerInfo.getPeerAddress().toString() + " :: Processing incoming ADDR...");

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

            // If we reach this far, we addBytes the Addresses to the Main Pool and request a connection Request:
            List<PeerAddress> peersToConnect = new ArrayList<>();
            for (NetAddressMsg netAddressMsg : msg.getAddrList()) {
                DiscoveryPeerInfo addPeerInfo = new DiscoveryPeerInfo(netAddressMsg.getAddress(), netAddressMsg.getTimestamp());
                addToPool(addPeerInfo);
                peersToConnect.add(addPeerInfo.getPeerAddress());
            }
            super.eventBus.publish(new ConnectPeersRequest(peersToConnect));


            logger.debug(peerInfo.getPeerAddress(),msg.getCount().getValue() + " Addresses received via ADDR. ");
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
        builder.numNodesAdded(state.getNumNodesAdded() + addedToPool)
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
     * Some verifications are alos performed, the Peer is only added when it meets them all. The result of this method
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

        // Finally, we either addBytes it to the mainPool or not:
        if (result) {
            // We update the Status:
            updateState(1, 0, 0, 0, 0,null);

            // We perform some changes on the timestamp according to the rules specified in the Satoshi client:
            // https://en.bitcoin.it/wiki/Satoshi_Client_Node_Discovery#Ongoing_.22addr.22_advertisements
            // - If the timestamp is too low or too high, it is set to 5 days ago.
            // - We subtract 2 hours from the timestamp and addBytes the address.
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
                    .collect(Collectors.toList());
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
                    .filter(p -> peersHandshaked.contains(p.getPeerAddress()))
                    .filter(p -> Duration.between(p.getLastHandshakeTime(), DateTimeUtils.nowDateTimeUTC()).compareTo(waitingDuration) > 0)
                    .collect(Collectors.toList());

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
}
