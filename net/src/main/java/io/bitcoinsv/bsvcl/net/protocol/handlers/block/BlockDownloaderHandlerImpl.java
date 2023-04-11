package io.bitcoinsv.bsvcl.net.protocol.handlers.block;


import io.bitcoinsv.bsvcl.net.protocol.events.control.*;
import io.bitcoinsv.bsvcl.net.protocol.events.data.*;
import io.bitcoinsv.bsvcl.net.protocol.handlers.block.strategies.AnnouncersStrategy;
import io.bitcoinsv.bsvcl.net.protocol.handlers.block.strategies.DownloadResponse;
import io.bitcoinsv.bsvcl.net.protocol.handlers.block.strategies.IBDStrategy;
import io.bitcoinsv.bsvcl.net.protocol.handlers.block.strategies.PriorityStrategy;
import io.bitcoinsv.bsvcl.net.protocol.handlers.message.streams.deserializer.DeserializerStream;
import io.bitcoinsv.bsvcl.net.protocol.messages.*;
import io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsg;
import io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsgBuilder;
import io.bitcoinsv.bsvcl.net.network.PeerAddress;
import io.bitcoinsv.bsvcl.net.network.events.NetStartEvent;
import io.bitcoinsv.bsvcl.net.network.events.NetStopEvent;
import io.bitcoinsv.bsvcl.net.network.events.PeerDisconnectedEvent;
import io.bitcoinsv.bsvcl.common.config.RuntimeConfig;
import io.bitcoinsv.bsvcl.common.events.EventBus;
import io.bitcoinsv.bsvcl.common.handlers.HandlerConfig;
import io.bitcoinsv.bsvcl.common.handlers.HandlerImpl;
import io.bitcoinsv.bsvcl.net.tools.LoggerUtil;
import io.bitcoinsv.bsvcl.common.thread.ThreadUtils;
import io.bitcoinsv.bitcoinjsv.core.Sha256Hash;
import io.bitcoinsv.bitcoinjsv.core.Utils;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Implementation of the BlockDownloader Handler.
 * This handler allows to download multiple Blocks at the same time. The Blocks can be downloaded and
 * deserialized in 2 ways:
 *
 * - A "normal" way: When the Block is NOT a BIG Block (A Block is considered BIG when its size is bigger than
 *   the property "msgSizeInBytesForRealTimeProcessing" in the Runtime Configuration). In this mode, the Block
 *   content (bytes) is downloaded first, an then it's deserialized. The whole Block is notified as a Java Object.
 *
 * - A "real time" way: When the Block is BIG. In this case, the Whole block is deserialized at the same time as the
 *   bytes arrive down the wire, and its not deserialized into a Java Object, instead different notifications are
 *   triggered when different parts of the Block are deserialized.
 *
 * This Handler is always running. At every moment, it loops over the Connected Peers and, if some blocks are still
 * pending to download and there are Peers availa le, we start downloading blocks from them. In case there is a
 * problem while downloading a Block from a Peer (or the Peer has disconnected in the middle of the process), that
 * block is assigned to another availbale Peer. When a block has failed to be download for a certains number of times
 * (configurable), the Block is discarded.
 *
 * @see BlockDownloadedEvent
 * @see LiteBlockDownloadedEvent
 * @see BlockDiscardedEvent
 */
public class BlockDownloaderHandlerImpl extends HandlerImpl<PeerAddress, BlockPeerInfo> implements BlockDownloaderHandler {

    /**
     * States this handler can be into. RUNNING is the normal mode. In PAUSED mode, no new blocks will be downloaded,
     * but the ones that were in the middle of downloading when the state changed, will still finish.
     */
    public enum DonwloadingState { RUNNING, PAUSED }

    // Handler Download State:
    private DonwloadingState downloadingState;
    // We store in this list some "Notes" or "Events" that we might want to TRACE. These will be stored in the State
    private List<String> downloadEvents = new ArrayList<>();

    // If this is FALSE, it means that at this very moment no MORE downloads are allowed, until some blocks are finish
    private boolean moreDownloadsAllowed = true;

    // if we reach the Maximum amount of data to download at a given time (by config), then we set this to TRUE.
    // If TRUE, no more downloads are available until the amount of data downloaded decreases
    private boolean bandwidthRestricted = false;

    // We keep track of the Block Download History:
    private BlocksDownloadHistory blocksDownloadHistory;

    // Basic Config:
    private LoggerUtil logger;
    private BlockDownloaderHandlerConfig config;

    // En Executor and a Listener to trigger jobs in parallels.
    private final ExecutorService executor;

    // This Manager stores the list of Pending Blocks and has some logic inside to decide what is the "best" block
    // to download from a specific Peer, based on configuration:
    private final BlocksPendingManager blocksPendingManager;


    // We keep track of all the block and their States:
    private final Set<String> blocksDownloaded            = ConcurrentHashMap.newKeySet();
    private final Map<String, Instant> blocksDiscarded    = new ConcurrentHashMap<>();    // we record time of discarding
    private final Set<String> blocksInLimbo               = ConcurrentHashMap.newKeySet();
    private final Set<String> blocksPendingToCancel       = ConcurrentHashMap.newKeySet();
    private final Set<String> blocksCancelled             = ConcurrentHashMap.newKeySet();
    private Map<String, Instant> blocksLastActivity       = new ConcurrentHashMap<>();

    // Specific information for LITE Blocks:
    private final Set<String> liteBlocksDownloaded        = ConcurrentHashMap.newKeySet();

    // Specific information for BIG Blocks:
    private final Map<String, PartialBlockHeaderMsg>   bigBlocksHeaders       = new ConcurrentHashMap<>();
    private final Map<String, Long>                    bigBlocksCurrentTxs    = new ConcurrentHashMap();

    // Info about Peers:
    private final Set<PeerAddress> disconnectedPeers = ConcurrentHashMap.newKeySet();

    // LOCK fort Multithread Sanity:
    Lock lock = new ReentrantLock();

    // We keep track of some indicators:
    private final AtomicLong totalReattempts = new AtomicLong();
    private final AtomicInteger busyPercentage = new AtomicInteger();

    // This handler can pause/resume itself based on some state (number of Peers connected, MB being downloaded, etc)
    // But it can also pause/resume by specific events triggered by the client. So the order of preference is this:
    //  - If a specific Event requests to PAUSE, then we PAUSE no matter what.
    //  - If a specific event asks to Resume,then we resume as long as our internal state allows us to

    private boolean resumedByClient = true;
    private boolean resumedByInternalState = false;

    // We keep track also of the list of Rejections obtained for each Peer that can NOT download any block at all
    private Map<PeerAddress, DownloadResponse> downloadRejections = new ConcurrentHashMap<>();

    /** Constructor */
    public BlockDownloaderHandlerImpl(String id, RuntimeConfig runtimeConfig, BlockDownloaderHandlerConfig config) {
        super(id, runtimeConfig);
        this.config = config;
        this.logger = new LoggerUtil(id, HANDLER_ID, this.getClass());
        this.executor = ThreadUtils.getSingleThreadExecutorService("JclBlockDownloaderHandler");
        this.downloadingState = DonwloadingState.RUNNING;
        this.blocksDownloadHistory = new BlocksDownloadHistory();
        this.blocksDownloadHistory.setCleaningTimeout(config.getBlockHistoryTimeout());

        // We configure the Blocks-Pending Manager as we assign a Download Strategy:
        this.blocksPendingManager = new BlocksPendingManager();
        setDownloadStrategy(this.config, this.blocksPendingManager, this.eventBus);
    }

    /*
        It assigns a Download Strategy that will be used to determine who Blocks are assigned to Peers to download
     */
    private void setDownloadStrategy(BlockDownloaderHandlerConfig config, BlocksPendingManager blocksPendingManager, EventBus eventBus) {
        boolean isAnnouncersStrategy = config.getBestMatchCriteria().equals(BlockDownloaderHandlerConfig.BestMatchCriteria.FROM_ANNOUNCERS);
        PriorityStrategy downloadStrategy = (isAnnouncersStrategy)
                ? new AnnouncersStrategy(eventBus, config.getBestMatchNotAvailableAction(), config.getNoBestMatchAction())
                : new IBDStrategy(eventBus);
        blocksPendingManager.setDownloadStrategy(downloadStrategy);
    }

    /*
        We subscribe to Events from the JCL - Bus
     */
    private void registerForEvents() {

        // Network Events:
        subscribe(NetStartEvent.class, this::onNetStart);
        subscribe(NetStopEvent.class, this::onNetStop);
        subscribe(PeerMsgReadyEvent.class, this::onPeerMsgReady);
        subscribe(PeerHandshakedEvent.class, this::onPeerHandshaked);
        subscribe(PeerDisconnectedEvent.class, this::onPeerDisconnected);
        subscribe(MinHandshakedPeersReachedEvent.class, this::onMinHandshakeReached);
        subscribe(MinHandshakedPeersLostEvent.class, this::onMinHandshakeLost);

        // Lite Blocks downloaded in a single go:
        subscribe(BlockMsgReceivedEvent.class, this::onBlockMsgReceived);
        subscribe(RawBlockMsgReceivedEvent.class, this::onBlockMsgReceived);

        // Big Blocks received in Chunks:
        subscribe(BlockHeaderDownloadedEvent.class, this::onPartialBlockHeaderMsgReceived);
        subscribe(BlockTXsDownloadedEvent.class, this::onPartialBlockTxsMsgReceived);
        subscribe(BlockRawTXsDownloadedEvent.class, this::onPartialBlockTxsMsgReceived);

        // Download/Cancel requests:
        subscribe(BlocksDownloadRequest.class, e -> this.download(
                e.getBlockHashes(),
                e.isWithPriority(),
                e.isForceDownload(),
                e.getFromThisPeerOnly(),
                e.getFromThisPeerPreferably()
        ));

        subscribe(BlocksCancelDownloadRequest.class, e -> this.cancelDownload(e.getBlockHashes()));
        subscribe(BlocksDownloadStartRequest.class, this::onBlocksDownloadStartRequest);
        subscribe(BlocksDownloadPauseRequest.class, this::onBlocksDownloadPauseRequest);
        subscribe(NotFoundMsgReceivedEvent.class, this::onNotFoundMsg);
    }

    /*
        Returns the total size (in bytes) of all the blocks being downloaded at this moment
     */
    private long getCurrentDownloadingBlocksSize() {
        return this.bigBlocksHeaders.values().stream().mapToLong(h -> h.getTxsSizeInbytes().getValue()).sum();
    }

    /*
        Returns the number of Peers currently downloading blocks:
     */
    private int getCurrentPeersDownloading() {
        return (int) handlerInfo.values().stream()
                .filter(p -> p.getWorkingState().equals(BlockPeerInfo.PeerWorkingState.PROCESSING))
                .count();
    }

    /*
        Returns the list of Blocks being downloaded at this moment
     */
    private List<String> getBlocksBeingDownloaded() {
        return handlerInfo.values().stream()
                .filter(p -> p.isProcessing())
                .map(p -> p.getCurrentBlockInfo().getHash())
                .collect(Collectors.toList());
    }

    /*
        Returns Info of the Peer downloading te block given, if it exists
    */
    public Optional<BlockPeerInfo> getPeerDownloadingThisBlock(String blockHash) {
        return handlerInfo.values().stream().filter(b -> b.isDownloading(blockHash)).findFirst();
    }

    /*
        This method calculates the percentage of Thread occupation.
        This value is an accumulative one, and it resets every time the "getState()" method is called.
        It compares the number of blocks being downloaded to the maximum allowed.
     */
    private int getUpdatedBusyPercentage() {
        int numBlocksInProgress = getCurrentPeersDownloading();
        int percentage = (int) (numBlocksInProgress * 100) / config.getMaxBlocksInParallel();
        int result = Math.max(this.busyPercentage.get(), percentage);
        return result;
    }

    /*
        It pauses the Download Process.
        The Blocks already being attempted will still be finished.
     */
    private void pause() {
        logger.debug("Pausing download...");
        this.downloadingState = DonwloadingState.PAUSED;
        this.blocksPendingManager.switchToRestrictedMode();
    }

    /*
        It Resumes the downloading process
     */
    private void resume() {
        logger.debug("Resuming download...");
        this.downloadingState = DonwloadingState.RUNNING;
        this.blocksPendingManager.switchToNormalMode();
    }

    /*
        Event Handler:
        This is request to PAUSE the download from the client.
        We Pause right away
     */
    private void onBlocksDownloadPauseRequest(BlocksDownloadPauseRequest event) {
        logger.debug("Pausing download by Client...");
        this.resumedByClient = false;
        pause();
    }

    /*
        Event Handler:
        We Resume/Start the Downloading, as along as our internal state allows for it
     */
    private void onBlocksDownloadStartRequest(BlocksDownloadStartRequest event) {
        logger.debug("Resuming download by Client...");
        this.resumedByClient = true;
        if (this.resumedByInternalState) {
            resume();
        }
    }

    /*
        Event Handler:
        We Resume the Download, unless the client had previously requested to PAUSE
     */
    private void onMinHandshakeReached(MinHandshakedPeersReachedEvent event) {
        logger.debug("Min number of Peers reached ({}). Resuming download ...", event.getNumPeers());
        this.resumedByInternalState = true;
        if (this.resumedByClient) {
            resume();
        }
    }

    /*
        Event Handler:
        We PAUSE the Download
     */
    private void onMinHandshakeLost(MinHandshakedPeersLostEvent event) {
        logger.debug("Min number of Peers lost ({}). Pausing download...", event.getNumPeers());
        this.resumedByInternalState = false;
        pause();
    }

    /*
        Event Handler:
        A Peer notifies that it doesn't have the block is being requested.
        We move immediately the block back to the pending pool, and we reset the Peer
        We also inform BlocksPendingManager about this, so it doesn't try this Peer for this Block anymore
     */
    private void onNotFoundMsg(NotFoundMsgReceivedEvent event) {
        // We check the Msg integrity:
        if (event.getBtcMsg().getBody().getCount().getValue() > 1) {
            logger.warm("NotFoundMsg received but more than 1 Block specified!");
            return;
        }
        // We check if this msg is related to any Peer downloading a block...
        String blockHash = Sha256Hash.wrapReversed(event.getBtcMsg().getBody().getInvVectorList().get(0).getHashMsg().getHashBytes()).toString();
        Optional<BlockPeerInfo> peerInfoOpt = this.handlerInfo.values().stream().filter(p -> p.isDownloading(blockHash)).findFirst();
        if (peerInfoOpt.isEmpty()) {
            logger.warm("NotFound Msg received for block #{} but no Peer is downloading it!", blockHash);
            return;
        }

        // We log it:
        logger.info("Block #" + blockHash + " NOT FOUND by Peer [" + peerInfoOpt.get().getPeerAddress() + "]..." );
        blocksDownloadHistory.register(blockHash,   "NOT FOUND Msg received from Peer [" + event.getPeerAddress() + "]");
        downloadEvents.add(Instant.now().toString() + " : Block #" + blockHash + " Not Found by [" + event.getPeerAddress() + "]");

        // we process it as a Failure:
        processDownloadFailure(blockHash);

    }

    private boolean isRunning() { return this.downloadingState.equals(DonwloadingState.RUNNING); }
    private boolean isPaused()  { return this.downloadingState.equals(DonwloadingState.PAUSED); }

    @Override
    public BlockDownloaderHandlerState getState() {
        // We run some calculations first:

        // We get the percentage and we reset it right after that:
        int percentage = getUpdatedBusyPercentage();
        this.busyPercentage.set(0);

        // total of Blocks size being downloaded:
        long blocksDownloadingSize = this.bigBlocksHeaders.values().stream()
                .mapToLong(h -> h.getTxsSizeInbytes().getValue())
                .sum();

        return BlockDownloaderHandlerState.builder()

                // Direct references:
                .pendingBlocks(new ArrayList<>(this.blocksPendingManager.getPendingBlocks()))
                .downloadedBlocks(this.blocksDownloaded)
                .discardedBlocks(new ArrayList<>(this.blocksDiscarded.keySet()))
                .pendingToCancelBlocks(new ArrayList<>(this.blocksPendingToCancel))
                .cancelledBlocks(new ArrayList<>(this.blocksCancelled))
                .blocksHistory(this.blocksDownloadHistory.getBlocksHistory())
                .downloadEvents(this.downloadEvents)

                // Defensive Copies:
                .downloadRejections(new HashMap<>(this.downloadRejections))
                .peersInfo(this.handlerInfo.values().stream().filter(BlockPeerInfo::isHandshaked).toList())
                .totalReattempts(this.totalReattempts.get())
                .blocksNumDownloadAttempts(new HashMap<>(blocksPendingManager.getBlockDownloadAttempts()))
                .blocksInLimbo(new HashSet<>(this.blocksInLimbo))
                .blocksLastActivity(new HashMap<>(this.blocksLastActivity))

                // Static/Primitive properties
                .config(this.config)
                .downloadingState(this.downloadingState)
                .busyPercentage(percentage)
                .bandwidthRestricted(this.bandwidthRestricted)
                .blocksDownloadingSize(blocksDownloadingSize)
                .build();
    }

    @Override
    public void init() {
        registerForEvents();
    }

    @Override
    public void download(List<String> blockHashes) {
        download(blockHashes, false, false, null, null);
    }

    @Override
    public void download(List<String> blockHashes, boolean withPriority) {
        download(blockHashes, false, false, null, null);
    }

    @Override
    public void download(List<String> blockHashes, boolean withPriority, boolean forceDownload, PeerAddress fromThisPeerOnly, PeerAddress fromThisPeerPreferably) {
        try {
            lock.lock();
            // First we add the list of Block Hashes to the Pending Pool:
            logger.info("Adding " + blockHashes.size() + " blocks to download (priority: " + withPriority + ", forceDownload: " + forceDownload + ")");

            // We filter out those blocks cancelled, etc...
            List<String> blocksBeingDownloadedNow = this.getBlocksBeingDownloaded();
            List<String> blockHashesToAdd =  blockHashes.stream()
                    .filter(h -> !blocksCancelled.contains(h))
                    .filter(h -> !blocksPendingToCancel.contains(h))
                    .filter(h -> !blocksBeingDownloadedNow.contains(h))
                    .collect(Collectors.toList());

            // Now we add them all to the BlocksPendingManager:
            if (withPriority) {
                blocksPendingManager.addWithPriority(blockHashesToAdd);
            } else {
                blocksPendingManager.add(blockHashesToAdd);
            }

            // If the blocks have priorities specified, we fed them into the Download Strategy.
            // Now we add the Priorities Peers, if specified:
            if (fromThisPeerOnly != null) {
                blocksPendingManager.getStrategy().registerBlockExclusivity(blockHashesToAdd, fromThisPeerOnly);
            }
            if (fromThisPeerPreferably != null) {
                blocksPendingManager.getStrategy().registerBlockPriority(blockHashesToAdd, fromThisPeerPreferably);
            }
            // If the "forceDownload" flag is active, this means that we need to Download this block even if
            // we are PAUSED. We already have logic in place according to which those Blocks already attempted will
            // always be downloaded even in PAUSED mode, so here we make a trick: We pretend this block has been
            // attempted already by registering an attempt:
            if (forceDownload) {
                blocksPendingManager.registerBlockAsAlreadyAttempted(blockHashesToAdd);
            }
        } finally {
            lock.unlock();
        }
    }


    @Override
    public void cancelDownload(List<String> blockHashes) {
        try {
            lock.lock();
            logger.debug("Cancelling " + blockHashes.size() + " blocks from download: ");
            blockHashes.forEach(h -> cancelDownload(h));
        } finally {
            lock.unlock();
        }
    }

    /*
        Event Handler:
     */
    public void onNetStart(NetStartEvent event) {
        logger.trace("Starting...");
        this.blocksDownloadHistory.start();
        executor.execute(this::jobProcessCheckDownloadingProcess);
    }

    /*
        Event Handler:
     */
    public void onNetStop(NetStopEvent event) {
        this.blocksDownloadHistory.stop();
        if (this.executor != null) executor.shutdownNow();
        logger.trace("Stop.");
    }

    /*
        Event Handler:
     */
    public void onPeerMsgReady(PeerMsgReadyEvent event) {
        logger.debug(event.getStream().getPeerAddress(), "New Peer Connected and Ready");
        try {
            lock.lock();
            // If the Peer is already in our Pool, that means it's been used before, so we just reset it, otherwise
            // we create a new one...
            PeerAddress peerAddress = event.getStream().getPeerAddress();
            BlockPeerInfo peerInfo = handlerInfo.get(peerAddress);

            if (peerInfo == null) {
                peerInfo = new BlockPeerInfo(peerAddress, (DeserializerStream) event.getStream().input());
            } else {
                // The Peer is in our Pool. 2 Options:
                //  - it might be a previous Peer that disconnected in the past, and connected again.
                //  - it must be a Peer that we are using already, but this Event has come in the wrong order, the peer
                //    might even be downloading a block already...
                if (!peerInfo.getWorkingState().equals(BlockPeerInfo.PeerWorkingState.PROCESSING)) {
                    peerInfo.connect((DeserializerStream)event.getStream().input());
                }
            }
            handlerInfo.put(peerAddress, peerInfo);
        } finally {
            lock.unlock();
        }
    }

    /*
        Event Handler:
     */
    public void onPeerHandshaked(PeerHandshakedEvent event) {
        logger.debug(event.getPeerAddress(), "New Peer Handshaked");
        try {
            lock.lock();
            BlockPeerInfo peerInfo = handlerInfo.get(event.getPeerAddress());
            if (peerInfo.isConnected()) {
                peerInfo.handshake();
            } else {
                logger.debug(event.getPeerAddress(), "Peer Handshaked ignored (Its already Disconnected)");
            }
        } finally {
            lock.unlock();
        }
    }
    /*
        Event Handler:
     */
    public void onPeerDisconnected(PeerDisconnectedEvent event) {
        try {
            lock.lock();
            BlockPeerInfo peerInfo = handlerInfo.get(event.getPeerAddress());
            if (peerInfo != null) {
                disconnectedPeers.add(peerInfo.getPeerAddress());
                if (peerInfo.isProcessing()) {
                    logger.trace(peerInfo.getPeerAddress(),  "Peer Disconnected but still downloading. Sent to Limbo", peerInfo.toString());
                    // If this Peer was in the middle of downloading a block, we put this block in the LIMBO:
                    blocksDownloadHistory.register(peerInfo.getCurrentBlockInfo().getHash(), peerInfo.getPeerAddress(), "Peer has disconnected");
                    // We put the block into Limbo so we wait a while until we retry...
                    putDownloadIntoLimbo(peerInfo);
                } else {
                    logger.trace(peerInfo.getPeerAddress(),  "Peer Disconnected. Disconnecting", peerInfo.toString());
                    peerInfo.disconnect();
                }
            }
            downloadRejections.remove(event.getPeerAddress());
        } finally {
            lock.unlock();
        }
    }

    /*
        Event Handler:
        A WHOLE Block in Deserialized Format has been Received
     */
    public void onBlockMsgReceived(BlockMsgReceivedEvent event) {
        try {
            lock.lock();
            if (!handlerInfo.containsKey(event.getPeerAddress())) return;
            processWholeBlockReceived(handlerInfo.get(event.getPeerAddress()), event.getBtcMsg());
        } finally {
            lock.unlock();
        }
    }

    /*
        Event Handler:
        A WHOLE Block in RAW Format (without Deserialization) has been Received
     */
    public void onBlockMsgReceived(RawBlockMsgReceivedEvent event) {
        try {
            lock.lock();
            if (!handlerInfo.containsKey(event.getPeerAddress())) return;
            processWholeRawBlockReceived(handlerInfo.get(event.getPeerAddress()), event.getBtcMsg());
        } finally {
            lock.unlock();
        }
    }

    /*
        Event Handler:
     */
    public void onPartialBlockHeaderMsgReceived(BlockHeaderDownloadedEvent event) {
        try {
            lock.lock();
            if (!handlerInfo.containsKey(event.getPeerAddress())) return;
            processPartialBlockReceived(handlerInfo.get(event.getPeerAddress()), event.getBtcMsg());
        } finally {
            lock.unlock();
        }
    }

    /*
        Event Handler:
     */
    public void onPartialBlockTxsMsgReceived(MsgReceivedEvent event) {
        try {
            lock.lock();
            if (!handlerInfo.containsKey(event.getPeerAddress())) return;
            processPartialBlockReceived(handlerInfo.get(event.getPeerAddress()), event.getBtcMsg());
        } finally {
            lock.unlock();
        }
    }

    /*
        It returns if the Download has been completed for a Block given
     */
    private boolean isDownloadComplete(String blockHash) {
        boolean result = false;

        // If the block has been registered as download, then we are done:
        if (this.blocksDownloaded.contains(blockHash)) return true;

        // If its a BIG BLock, we check if the Header has been downloaded, and if it is, then we check if
        // all the Txs have been downloading:
        PartialBlockHeaderMsg partialHeaderMsg = this.bigBlocksHeaders.get(blockHash);
        if (partialHeaderMsg != null) {
            // We check that all the Txs of this Big Block have been downloaded:
            if (this.bigBlocksCurrentTxs.containsKey(blockHash)) {
                result = this.bigBlocksCurrentTxs.get(blockHash).equals(partialHeaderMsg.getBlockHeader().getTransactionCount().getValue());
            }
        }
        return result;
    }


    /*
        This method is triggered when we receive some info coming from a Peer and related to a specific Block.
        It performs some verifications on the Peer and block given.
        If everything is correct, it returns TRUE.
        If something is wrong, it adjusts the internal structures (moving the Peer/Block to Limbo, etc) and returns
        false
     */
    private boolean checkDownloadFromPeer(String blockHash, PeerAddress peerAddress) {

        // Sanity check:
        if (!handlerInfo.containsKey(peerAddress)) {
            logger.warm("Info coming from unknown Peer ({})", peerAddress);
            return false;
        }

        // We register last activity for this Block:
        blocksLastActivity.put(blockHash, Instant.now());

        // We check if this Peer is actually being downloaded:
        boolean isOfficiallyDownloading = getBlocksBeingDownloaded().contains(blockHash);

        if (isOfficiallyDownloading) {

            // This Block is being tracked and being downloaded by a Peer.
            // If that Peer is this one, then it's Fine. But IF this Peer is ANOTHER Peer, then we just IGNORE it and
            // DROP the connection

            BlockPeerInfo peerInfo = getPeerDownloadingThisBlock(blockHash).get();
            if (!peerInfo.getPeerAddress().equals(peerAddress)) {
                logger.warm(peerInfo.getPeerAddress(), "data for block " + blockHash + " received BUT is is ALREADY being Downloaded by [" + peerInfo.getPeerAddress() + "], Ignoring...");
                blocksDownloadHistory.register(blockHash, peerInfo.getPeerAddress(), "data received but Ignored");
                // We disconnect from this Peer:
                //peerInfo.discard();
                //this.eventBus.publish(new DisconnectPeerRequest(peerInfo.getPeerAddress(), PeerDisconnectedEvent.DisconnectedReason.DISCONNECTED_BY_LOCAL));
                return false;
            } else return true;

        } else {
            // If its NOT registered as being Downloaded by any Peer, it might be because:
            // - A Peer sending an unrequested Block
            // - A Peer in LIMBO sending a block previously requested to this Peer
            logger.warm(peerAddress, "Sending data from unrequested block ("+ blockHash + ")");

            // We remove if from LIMBO, if its there:
            blocksInLimbo.remove(blockHash);

            // We "Officially" register this Peer as it is Downloading this Block:
            handlerInfo.get(peerAddress).startDownloading(blockHash, 1);
            blocksPendingManager.registerNewDownloadAttempt(blockHash);

            return true;
        }
    }

    private void processPartialBlockReceived(BlockPeerInfo peerInfo, BitcoinMsg<?> msg) {

        try {
            lock.lock();

            // The client of this library can expect that for every block downloaded, 3 types of Events are propagated:
            // - A: 1 "PartialBlockHeaderMsg" event when the Header is received
            // - B: several "BlockTXsDownloadedEvent" events, depending on the Block Size.
            // - C: 1 "BlockDownloadedEvent" event when the Block has been downloaded completely

            // This Handler receives the blocks from the EventBus in 2 ways:
            // - for BIG Blocks, we already receive the events "PartialBlockHeaderMsg" and "BlockTXsDownloadedEvent".
            // - For LITE Blocks, we only receive a "BlockMsgReceivedEvent" event with the whole block.


            String blockHash = (msg.is(PartialBlockHeaderMsg.MESSAGE_TYPE))
                    ? Utils.HEX.encode(((PartialBlockHeaderMsg) msg.getBody()).getBlockHeader().getHash().getBytes())
                    : (msg.is(PartialBlockTXsMsg.MESSAGE_TYPE))
                    ? Utils.HEX.encode(((PartialBlockTXsMsg) msg.getBody()).getBlockHeader().getHash().getBytes())
                    : Utils.HEX.encode(((PartialBlockRawTxMsg) msg.getBody()).getBlockHeader().getHash().getBytes());

            // In order to provide a consistent API to the User for both LITE blocks and BIG Blocks the same Events
            // are triggered: "PartialBlockHeaderMsg" and "PartialBlockHeaderMsg". for LITE blocks, they are triggered
            // manually for this class, so in order not to fall into an infinite-loop, we check and exit here:
            if (liteBlocksDownloaded.contains(blockHash)) return;

            // We perform some Sanity Checks:
            boolean checkOK = checkDownloadFromPeer(blockHash, peerInfo.getPeerAddress());
            if (!checkOK) return;

            // We process the incoming Msg:
            if (msg.is(PartialBlockHeaderMsg.MESSAGE_TYPE)) {
                // We update the info about the Header this block:
                PartialBlockHeaderMsg partialMsg = (PartialBlockHeaderMsg) msg.getBody();
                bigBlocksHeaders.put(blockHash, partialMsg);
                blocksDownloadHistory.register(blockHash, peerInfo.getPeerAddress(), "Header downloaded");
            } else if (msg.is(PartialBlockTXsMsg.MESSAGE_TYPE)) {
                // We update the info about the Txs of this block:
                PartialBlockTXsMsg partialMsg = (PartialBlockTXsMsg) msg.getBody();
                bigBlocksCurrentTxs.merge(blockHash, (long) partialMsg.getTxs().size(), (o, n) -> o + partialMsg.getTxs().size());
                blocksDownloadHistory.register(blockHash, peerInfo.getPeerAddress(), partialMsg.getTxs().size() + " Txs downloaded, (" + bigBlocksCurrentTxs.get(blockHash) + " Txs so far)");
            } else if (msg.is(PartialBlockRawTxMsg.MESSAGE_TYPE)) {
                // We update the info about the Txs of this block:
                PartialBlockRawTxMsg partialMsg = (PartialBlockRawTxMsg) msg.getBody();
                bigBlocksCurrentTxs.merge(blockHash, (long) partialMsg.getTxs().size(), (o, n) -> o + partialMsg.getTxs().size());
                blocksDownloadHistory.register(blockHash, peerInfo.getPeerAddress(), partialMsg.getTxs().size() + " Raw Txs downloaded, (" + bigBlocksCurrentTxs.get(blockHash) + " Txs so far)");
            }

            // Now we check if we've reached the total of TXs, so the Download is complete:
            // This verification is
            if (isDownloadComplete(blockHash)) {
                PartialBlockHeaderMsg blockHeader = bigBlocksHeaders.get(blockHash);
                processDownloadSuccess(peerInfo, blockHeader.getBlockHeader(), blockHeader.getTxsSizeInbytes().getValue());
            }

        } finally {
            lock.unlock();
        }
    }

    private void processWholeBlockReceived(BlockPeerInfo peerInfo, BitcoinMsg<BlockMsg> blockMesage) {

        // We just received a Block.
        // We publish an specific event for this Lite Block being downloaded:
        // NOTE: Sometimes, remote Peers send BLOCKS to us Even If we didn't ask for them. In that case, the Downloading
        // time is ZERO, since we didn't ask for it so we cannot measure the downloading time...

        try {
            lock.lock();

            String blockHash = Utils.HEX.encode(blockMesage.getBody().getBlockHeader().getHash().getBytes()).toString();

            // We perform some Sanity Checks:
            boolean checkOK = checkDownloadFromPeer(blockHash, peerInfo.getPeerAddress());
            if (!checkOK) return;

            // we register it:
            this.liteBlocksDownloaded.add(blockHash);

            Duration downloadingDuration = (peerInfo != null && peerInfo.getCurrentBlockInfo() != null)
                    ? Duration.between(peerInfo.getCurrentBlockInfo().getStartTimestamp(), Instant.now())
                    : Duration.ZERO;

            // We publish:
            super.eventBus.publish(new LiteBlockDownloadedEvent(peerInfo.getPeerAddress(), blockMesage, downloadingDuration));

            // Now, in order to follow the same approach for both Big blocks and Regular Blocks, we also trigger the
            // "HeaderDownloaded" and "TxsDownloaded" Events for this Block:

            // We notify the Header has been downloaded:
            PartialBlockHeaderMsg partialHeaderMsg = PartialBlockHeaderMsg.builder()
                    .blockHeader(blockMesage.getBody().getBlockHeader())
                    .txsSizeInBytes(blockMesage.getHeader().getMsgLength() - blockMesage.getBody().getBlockHeader().getLengthInBytes())
                    .blockTxsFormat(PartialBlockHeaderMsg.BlockTxsFormat.DESERIALIZED)
                    .build();
            BitcoinMsg<PartialBlockHeaderMsg> partialHeaderBtcMsg =  new BitcoinMsgBuilder<>(config.getBasicConfig(), partialHeaderMsg).build();
            super.eventBus.publish(new BlockHeaderDownloadedEvent(peerInfo.getPeerAddress(), partialHeaderBtcMsg));

            // We notify the txs has been downloaded:
            PartialBlockTXsMsg partialBlockTxsMsg = PartialBlockTXsMsg.builder()
                    .blockHeader(blockMesage.getBody().getBlockHeader())
                    .txs(blockMesage.getBody().getTransactionMsg())
                    .txsOrdersNumber(0)
                    .txsIndexNumber(0)
                    .build();
            BitcoinMsg<PartialBlockTXsMsg> partialBlockTxsBtcMsg = new BitcoinMsgBuilder<>(config.getBasicConfig(), partialBlockTxsMsg).build();
            super.eventBus.publish(new BlockTXsDownloadedEvent(peerInfo.getPeerAddress(),partialBlockTxsBtcMsg));

            // We update the structures:
            blocksDownloadHistory.register(blockHash, peerInfo.getPeerAddress(), "Whole block downloaded");
            blocksDownloadHistory.markForDeletion(blockHash);
            processDownloadSuccess(peerInfo, blockMesage.getBody().getBlockHeader(), blockMesage.getLengthInBytes());
        } finally {
            lock.unlock();
        }
    }

    private void processWholeRawBlockReceived(BlockPeerInfo peerInfo, BitcoinMsg<RawBlockMsg> rawBlockMessage) {

        // We just received a Block.
        // We publish an specific event for this Lite Block being downloaded:
        // NOTE: Sometimes, remote Peers send BLOCKS to us Even If we did ask for them. In that case, the Downloading
        // time is ZERO, since we didn't ask for it so we cannot measure the downloading time...

        try {
            lock.lock();

            String blockHash = Utils.HEX.encode(rawBlockMessage.getBody().getBlockHeader().getHash().getBytes()).toString();

            // We perform some Sanity Checks:
            boolean checkOK = checkDownloadFromPeer(blockHash, peerInfo.getPeerAddress());
            if (!checkOK) return;

            // we register it:
            this.liteBlocksDownloaded.add(blockHash);

            Duration downloadingDuration = (peerInfo != null && peerInfo.getCurrentBlockInfo() != null)
                    ? Duration.between(peerInfo.getCurrentBlockInfo().getStartTimestamp(), Instant.now())
                    : Duration.ZERO;

            // We publish it:
            super.eventBus.publish(new LiteRawBlockDownloadedEvent(peerInfo.getPeerAddress(), rawBlockMessage, downloadingDuration));

            // Now, in order to follow the same approach for both Big blocks and Regular Blocks, we also trigger the
            // "HeaderDownloaded" and "TxsDownloaded" Events for this Block:

            // We notify the Header has been downloaded:
            PartialBlockHeaderMsg partialHeaderMsg = PartialBlockHeaderMsg.builder()
                    .blockHeader(rawBlockMessage.getBody().getBlockHeader())
                    .txsSizeInBytes(rawBlockMessage.getHeader().getMsgLength() - rawBlockMessage.getBody().getBlockHeader().getLengthInBytes())
                    .blockTxsFormat(PartialBlockHeaderMsg.BlockTxsFormat.RAW)
                    .build();
            BitcoinMsg<PartialBlockHeaderMsg> partialHeaderBtcMsg =  new BitcoinMsgBuilder<>(config.getBasicConfig(), partialHeaderMsg).build();
            super.eventBus.publish(new BlockHeaderDownloadedEvent(peerInfo.getPeerAddress(), partialHeaderBtcMsg));

            // We notify the txs has been downloaded:
            PartialBlockRawTxMsg partialBlockRawTxMsg = PartialBlockRawTxMsg.builder()
                    .blockHeader(rawBlockMessage.getBody().getBlockHeader())
                    .txs(rawBlockMessage.getBody().getTxs())
                    .txsOrdersNumber(0)
                    .txsIndexNumber(0)
                    .build();
            BitcoinMsg<PartialBlockRawTxMsg> partialBlockRawTxsBtcMsg = new BitcoinMsgBuilder<>(config.getBasicConfig(), partialBlockRawTxMsg).build();
            super.eventBus.publish(new BlockRawTXsDownloadedEvent(peerInfo.getPeerAddress(),partialBlockRawTxsBtcMsg));

            // We update the structures:
            blocksDownloadHistory.register(blockHash, peerInfo.getPeerAddress(), "Whole Raw block downloaded");
            blocksDownloadHistory.markForDeletion(blockHash);
            processDownloadSuccess(peerInfo, rawBlockMessage.getBody().getBlockHeader(), rawBlockMessage.getLengthInBytes());
        } finally {
            lock.unlock();
        }
    }


    private void processDownloadSuccess(BlockPeerInfo peerInfo, BlockHeaderMsg blockHeader, Long blockSize) {
        // We process the success of this Block being downloaded
        // This block can be downloaded in two ways:
        // - The "normal" way: We requested this Blocks to be download, so a Peer was assigned to it, and now the
        //    whole Block has been received.
        // - The "unexpected" way: Some peer has sent this block to us without being asked for it. Maybe even the block
        //   is also being downloaded by other Peer (to whom we did ask to).

        try {
            lock.lock();

            String blockHash = Utils.HEX.encode(blockHeader.getHash().getBytes()).toString();

            // The Duration of the downloading time can be calculated, but if the peer has sent the Block without asking
            // for it, the downloading time is just ZERO (since we can't keep track)

            Duration downloadingDuration = (peerInfo != null && peerInfo.getCurrentBlockInfo() != null)
                    ? Duration.between(peerInfo.getCurrentBlockInfo().getStartTimestamp(), Instant.now())
                    : Duration.ZERO;

            // Log and record history:
            logger.debug(peerInfo.getPeerAddress(), "Block successfully downloaded", blockHash);

            // We register the history and mark it for deletion, since its been processed successfully.
            blocksDownloadHistory.register(blockHash, peerInfo.getPeerAddress(), "Block successfully downloaded");
            blocksDownloadHistory.markForDeletion(blockHash);
            if (config.isRemoveBlockHistoryAfterDownload()) {
                blocksDownloadHistory.remove(blockHash);    // immediate deletion
            }

            // We activated back the ping/Pong Verifications for this Peer
            super.eventBus.publish(new EnablePingPongRequest(peerInfo.getPeerAddress()));

            // We publish an Event notifying that this Block being downloaded:
            super.eventBus.publish(
                    new BlockDownloadedEvent(
                            peerInfo.getPeerAddress(),
                            blockHeader,
                            downloadingDuration,
                            blockSize
                    )
            );

            // We reset the peer, to make it ready for a new download, and we update its workingState:
            // NOTE: Some Peers disconnect after sending a block and the Disconnected Event might have arrived BEFORE
            // this one, so we check....
            if (!peerInfo.isDisconnected()) {
                peerInfo.reset();
                peerInfo.getStream().resetBufferSize();
            }

            // If The Block has been downloading after several Attempts, we log that info as an Event, we might want to
            // see it in the log:
            int numAttempts = blocksPendingManager.getBlockDownloadAttempts().get(blockHash);
            if (numAttempts > 1) {
                this.downloadEvents.add(Instant.now() + " : Block #" + blockHash + " downloaded from [" + peerInfo.getPeerAddress() + "] in attempt #" + numAttempts);
            }

            blocksDownloaded.add(blockHash);
            blocksPendingManager.registerBlockDownloaded(blockHash);
            blocksInLimbo.remove(blockHash);
            bigBlocksHeaders.remove(blockHash);
            bigBlocksCurrentTxs.remove(blockHash);
            blocksPendingToCancel.remove(blockHash);
        } finally {
            lock.unlock();
        }
    }

    private void cancelDownload(String blockHash) {
        try {
            lock.lock();

            // If this block has not been cancelled previously, we record it:
            if (!blocksPendingToCancel.contains(blockHash) && !blocksCancelled.contains(blockHash)) {
                logger.debug("Cancelling " + blockHash + " block from download: ");
                blocksPendingToCancel.add(blockHash);
                blocksDownloadHistory.register(blockHash,   "block requested for cancellation");
            }

            // the only scenario when a Block can NOT be cancelling is when it's been actively being downloaded. In the
            // rest of cases, we cancel and remove it from our internal structures:

            List<String> blocksBeingDownloaded = getBlocksBeingDownloaded();
            if (!blocksBeingDownloaded.contains(blockHash)) {
                blocksInLimbo.remove(blockHash);
                blocksPendingManager.registerBlockCancelled(blockHash);
                blocksDiscarded.remove(blockHash);
                bigBlocksHeaders.remove(blockHash);
                bigBlocksCurrentTxs.remove(blockHash);
                blocksPendingToCancel.remove(blockHash);
                blocksCancelled.add(blockHash);

                blocksDownloadHistory.register(blockHash,   "block cancelled");
            }
        } finally {
            lock.unlock();
        }
    }

    /*
        It takes a Peer, which is downloading a block, and set them both to "LIMBO" State.
     */
    private void putDownloadIntoLimbo(BlockPeerInfo peerInfo) {
        try {
            lock.lock();
            blocksInLimbo.add(peerInfo.getCurrentBlockInfo().getHash());
            blocksLastActivity.put(peerInfo.getCurrentBlockInfo().getHash(), Instant.now());
            peerInfo.setToLimbo();
        } finally {
            lock.unlock();
        }
    }


    /*
        It processes a Block that is in LIMBO State, and decides what to do with it: leave it there some more time,
        cancle it, etc...
     */
    private void processBlockInLimbo(String blockHash) {
        try {
            lock.lock();

            // if the number of attempts tried for this block is no longer stored, that means that the block has been
            // succesfully downloaded after all...
            if (!blocksPendingManager.isBlockBeingAttempted(blockHash)) {
                // The block is removed from the Limbo, but NOT the Peer, which might still send the block some time
                // in the future:
                blocksInLimbo.remove(blockHash);
                return;
            }

            // If this block is in the "pendingToCancel" list, that means that this Block has been cancelled, so we do
            // not try to re-attempts it anymore...
            if (this.blocksPendingToCancel.contains(blockHash)) {
                cancelDownload(blockHash);
                return;
            }

            // Peer downloading this Block:
            BlockPeerInfo peerDownloading = getPeerDownloadingThisBlock(blockHash).get();

            // We check the time passed from the moment we got the first bit of data of the body of this block
            // We get initial timestmap:
            Instant blockActivityInit = peerDownloading.getCurrentBlockInfo().getLastBytesReceivedTimestamp();

            // If this is an Unrequested block, we set up timestamp to the time it was supposed to be sent to LIMBO:
            if (blockActivityInit == null) {
                blockActivityInit = Instant.now().minus(config.getInactivityTimeoutToFail().toSeconds(), ChronoUnit.SECONDS);
            }
            Duration timePassedSinceLastActivity = Duration.between(blockActivityInit, Instant.now());

            // If the Timeout has expired, the download of this block has definitely failed:
            if (timePassedSinceLastActivity.compareTo(config.getInactivityTimeoutToFail()) > 0) {
                processDownloadFailure(blockHash); // This block has definitely failed:
            }

        } finally {
            lock.unlock();
        }
    }

    /*
        It processes the Download of this Block as a FAILURE. Depending on the case:
        The block is sent either back to the Pending Pool or discarded
        The Peer assigned to this block is either disconnected or sent back to Work
     */
    private void processDownloadFailure(String blockHash) {
        try {
            lock.lock();

            // WHAT TO DO WITH THE BLOCK:
            // The Block might be sent back to the pending Pool or discarded based on the number of attempts tried:

            int numAttempts = blocksPendingManager.getNumDownloadAttempts(blockHash);
            if (numAttempts < config.getMaxDownloadAttempts()) {
                logger.debug("Download failure for " + blockHash + " :: back to the pending Pool...");
                blocksDownloadHistory.register(blockHash, "Block moved back to the pending Pool");
                blocksPendingManager.addWithPriority(blockHash); // we add it to the FRONT of the Queue
                this.totalReattempts.incrementAndGet();          // keep track of total re-attempts
            } else {
                logger.debug("Download failure for " + blockHash, numAttempts + " attempts (max " + config.getMaxDownloadAttempts() + ")", "discarding Block...");
                blocksDownloadHistory.register(blockHash,   "block discarded (max attempts broken, reset to zero)");
                blocksDiscarded.put(blockHash, Instant.now());
                blocksPendingManager.registerBlockDiscarded(blockHash);
                // We publish the event:
                super.eventBus.publish(new BlockDiscardedEvent(blockHash, BlockDiscardedEvent.DiscardedReason.TIMEOUT));
            }

            bigBlocksHeaders.remove(blockHash);
            bigBlocksCurrentTxs.remove(blockHash);
            blocksInLimbo.remove(blockHash);

            // WHAT TO DO WITH THE PEER:
            //  - If the Peer has already sent something for this Block, we DISCARD the Peer and
            //    put the block back to work.
            //  - If the Peer has NOT sent anything for this Block, we put both (Peer and block) back to work.

            // TODO: Discarding a Peer might NOt be the best option

            Optional<BlockPeerInfo> peerInfoOpt = getPeerDownloadingThisBlock(blockHash);
            if (peerInfoOpt.isPresent()) {
                PeerAddress peerAddress  = peerInfoOpt.get().getPeerAddress();
                boolean hasSentSomething = peerInfoOpt.get().getCurrentBlockInfo().getBytesDownloaded() > 0;

                if (!hasSentSomething) {
                    logger.warm(peerAddress, "Block " + blockHash + " failed Download. No data sent. Setting this Per back to Work...");
                    peerInfoOpt.get().reset();
                } else {
                    logger.warm(peerAddress, "Block " + blockHash + " failed Download after sending some Data. Dropping connection...");
                    peerInfoOpt.get().discard();
                    //super.eventBus.publish(new DisconnectPeerRequest(peerAddress, PeerDisconnectedEvent.DisconnectedReason.DISCONNECTED_BY_LOCAL_LAZY_DOWNLOAD));
                }
                // We register the failure from this Peer
                blocksPendingManager.registerDownloadFailure(blockHash, peerAddress);
            } else {
                logger.warm("Block " + blockHash + " failed Download but there is NO Peer downloading it!");
            }

        } finally {
            lock.unlock();
        }
    }

    private void startDownloading(BlockPeerInfo peerInfo, String blockHash) {
        try {
            lock.lock();

            // log and record history:
            logger.debug(peerInfo.getPeerAddress(), "Starting downloading Block " + blockHash);
            blocksDownloadHistory.register(blockHash, peerInfo.getPeerAddress(), "Starting downloading");

            // The block is not rejected anymore if it ever was)
            downloadRejections.remove(peerInfo.getPeerAddress());

            // We update the Peer Info
            int numAttempts = blocksPendingManager.getNumDownloadAttempts(blockHash) + 1;
            peerInfo.startDownloading(blockHash, numAttempts);
            peerInfo.getStream().upgradeBufferSize();

            // We disable the Ping/Pong monitor process on it, since it might be busy during the block downloading
            super.eventBus.publish(new DisablePingPongRequest(peerInfo.getPeerAddress()));

            // We update number of attempts:
            blocksPendingManager.registerNewDownloadAttempt(blockHash);

            // We update the accumulative "busyPercentage" field:
            this.busyPercentage.set(getUpdatedBusyPercentage());

            // We use the Bitcoin Protocol to ask for that Block, sending a GETDATA message...
            HashMsg hashMsg =  HashMsg.builder().hash(Utils.reverseBytes(Utils.HEX.decode(blockHash)))
                    .build();
            InventoryVectorMsg invMsg = InventoryVectorMsg.builder()
                    .type(InventoryVectorMsg.VectorType.MSG_BLOCK)
                    .hashMsg(hashMsg)
                    .build();

            GetdataMsg msg = GetdataMsg.builder().invVectorList(Arrays.asList(invMsg)).build();
            BitcoinMsg<GetdataMsg> btcMsg = new BitcoinMsgBuilder<>(config.getBasicConfig(), msg).build();
            // We send the message
            super.eventBus.publish(new SendMsgRequest(peerInfo.getPeerAddress(), btcMsg));
            logger.trace(peerInfo.getPeerAddress(), "GET_DATA for Block " + blockHash + " sent to " + peerInfo.getPeerAddress());
        } finally {
            lock.unlock();
        }
    }

    // On each execution of this Job, we perform 2 actions:
    // - Update download progress and re-assign block downloads:
    // - check interrumpted downloads
    // - check Discarded Blocks

    private void jobProcessCheckDownloadingProcess() {
        try {
            while (true) {

                try {
                    lock.lock();

                    // UPDATE DOWNLOAD PROGRESS AND RE-ASSIGN BLOCK DOWNLOADS:
                    // We look over all the Peers, and we assign new Blocks to download to them, we update the
                    // download progress, or we detect if some timeouts have been triggered...

                    // We order the peers by download Speed, the fastest go first:
                    List<BlockPeerInfo> peersOrdered =  handlerInfo.values().stream()
                            .filter(p -> p.isHandshaked())
                            .collect(Collectors.toList());
                    Collections.sort(peersOrdered, BlockPeerInfo.SPEED_COMPARATOR);
                    Iterator<BlockPeerInfo> it = peersOrdered.iterator();

                    // We process each Peer...
                    while (it.hasNext()) {
                        BlockPeerInfo peerInfo = it.next();
                        PeerAddress peerAddress = peerInfo.getPeerAddress();
                        BlockPeerInfo.PeerWorkingState peerWorkingState = peerInfo.getWorkingState();

                        // If the Peer is NOT HANDSHAKED, we skip it...
                        if (!peerInfo.isHandshaked()) continue;

                        // We manage it based on its state:
                        switch (peerWorkingState) {
                            case IDLE: {
                                // SANITY CHECK: WE check if more downloads are allowed:
                                int numPeersWorking = getCurrentPeersDownloading();
                                long totalMBbeingDownloaded = getCurrentDownloadingBlocksSize() / 1_000_000; // convert to MB
                                this.bandwidthRestricted = totalMBbeingDownloaded >= config.getMaxMBinParallel();
                                this.moreDownloadsAllowed = (numPeersWorking == 0)
                                        || ((numPeersWorking < config.getMaxBlocksInParallel()) && !bandwidthRestricted);


                                // If we are in PAUSED Mode, we might still need to keep trying to download those blocks
                                // which we already started and they are now in LIMBO...
                                boolean isPausedAndBlocksInProcess = isPaused() && !blocksPendingManager.getBlockDownloadAttempts().isEmpty();

                                // If we can download more Blocks, we ask the BlocksPendingManager for a Suitable block for
                                // this Peer to download:

                                if (isPausedAndBlocksInProcess || (isRunning() && moreDownloadsAllowed)) {

                                    // In order to be efficient, the BlocksPendingManager also needs to know
                                    // about all the peers available for Download (EXCLUDING THIS ONE):

                                    List availablePeers = peersOrdered.stream()
                                            .filter(i -> !i.getPeerAddress().equals(peerAddress))
                                            .filter(i -> i.isHandshaked())
                                            .filter(i -> i.getWorkingState().equals(BlockPeerInfo.PeerWorkingState.IDLE))
                                            .map( i -> i.getPeerAddress())
                                            .collect(Collectors.toList());

                                    List notAvailablePeers = peersOrdered.stream()
                                            .filter(i -> !i.getPeerAddress().equals(peerAddress))
                                            .filter(i -> i.isHandshaked())
                                            .filter(i -> i.getWorkingState().equals(BlockPeerInfo.PeerWorkingState.PROCESSING))
                                            .map( i -> i.getPeerAddress())
                                            .collect(Collectors.toList());

                                    // We finally request a Peer to assign and download from this Peer, if any has been found:
                                    Optional<DownloadResponse> downloadResponse = blocksPendingManager
                                            .extractMostSuitableBlockForDownload(peerAddress, availablePeers, notAvailablePeers);

                                    // possible outcomes:
                                    // - No blocks to assign: nothing to do:
                                    // - Assignment possible: we trigger the download
                                    // - Assignment rejected: we save this info for logging

                                    if (downloadResponse.isPresent()) {
                                        if (downloadResponse.get().isAssigned()) {
                                            startDownloading(peerInfo, downloadResponse.get().getRequest().getBlockHash());
                                        } else {
                                            // A Peer not being able to download any block at all is a rare situation, so
                                            // we save the references to the rejection so they can be saved in the State:
                                            downloadRejections.put(peerInfo.getPeerAddress(), downloadResponse.get());
                                        }
                                    } else {
                                        downloadRejections.remove(peerInfo.getPeerAddress());
                                    }
                                }
                                break;
                            }

                            case PROCESSING: {
                                // we update the Progress of this Peer:
                                peerInfo.updateBytesProgress();

                                // We check for Errors in the data coming from this Peer:
                                String msgFailure = null;

                                // We check the timeouts. If the peer has broken some of these timeouts, we discard it:

                                if (peerInfo.getStream().getState().getProcessState().isCorrupted())                      { msgFailure = "CORRUPTED: Error during Deserialization";}
                                if (peerInfo.isIdleTimeoutBroken(config.getMaxIdleTimeout()))                             { msgFailure = "Idle Time expired"; peerInfo.setDownloadSpeed(0);}
                                if (peerInfo.isDownloadTimeoutBroken(config.getMaxDownloadTimeout()))                     { msgFailure = "Downloading Time expired"; }
                                if (peerInfo.getConnectionState().equals(BlockPeerInfo.PeerConnectionState.DISCONNECTED)) { msgFailure = "Peer Closed while downloading"; }

                                // We dont process a SLOW peer as a Failure anymore: Since we are NOT dropping the connections to
                                // Peers now, if a Peer is slow we cannot cancel the download since the Peer will still be sending
                                // as the block in the background, so we might be downloading the same blocks from multiple peers,
                                // which is not good.
                                //if (peerInfo.isTooSlow(config.getMinSpeed()))                                             { msgFailure = "Peer too slow"; }

                                if (msgFailure != null) {
                                    logger.debug(peerAddress, "Download Failure", peerInfo.getCurrentBlockInfo().getHash(), msgFailure);
                                    this.downloadEvents.add(Instant.now() + " : " + "Download Failure from [" + peerAddress + "] : " + msgFailure);
                                    blocksDownloadHistory.register(peerInfo.getCurrentBlockInfo().getHash(), peerInfo.getPeerAddress(), "Download Issue detected : " + msgFailure);
                                    putDownloadIntoLimbo(peerInfo);
                                    blocksPendingManager.registerDownloadFailure(peerInfo.getCurrentBlockInfo().getHash(), peerInfo.getPeerAddress());
                                    // We discard this Peer and also send a request to Disconnect from it:
                                    // peerInfo.discard();
                                    // super.eventBus.publish(new DisconnectPeerRequest(peerInfo.getPeerAddress(), PeerDisconnectedEvent.DisconnectedReason.DISCONNECTED_BY_LOCAL_LAZY_DOWNLOAD, null));

                                }
                                break;
                            }
                        } // Switch...
                    } // white it. next...

                    // CHECK INTERRUMPTED DOWNLOADS (BLOCKS IN "LIMBO")
                    // We look over the "blocksFailedDuringDownload" set, and check if those blocks are still "alive"
                    // (we are still receiving data from them, although we've been already notified about the peers
                    //  disconnecting), or those blocks are actually "broken", in this case we re-assign or discard them

                    List<String> blocksFailed = this.blocksInLimbo.stream().collect(Collectors.toList());
                    for (String blockHash: blocksFailed) {
                        processBlockInLimbo(blockHash);
                    }

                    // CHECK DISCARDED BLOCKS
                    // blocks stored in "blocksDiscarded" are blocks that have failed more times than specified in a
                    // limit in the config. But they can be retried again, after some time has passed. Here we check if
                    // its time to re-try them:

                    List<String> blocksToReTry = new ArrayList<>();
                    for (String hashDiscarded: blocksDiscarded.keySet()) {
                        if (Duration.between(blocksDiscarded.get(hashDiscarded), Instant.now())
                                .compareTo(config.getRetryDiscardedBlocksTimeout()) > 0) blocksToReTry.add(hashDiscarded);
                    }

                    if (blocksToReTry.size() > 0) {
                        for (String hashToRetry : blocksToReTry) {
                            blocksDiscarded.remove(hashToRetry);
                            blocksDownloadHistory.register(hashToRetry, "Block picked up again to re-attempt download...");
                            blocksPendingManager.addWithPriority(hashToRetry); // blocks to retry have preference...
                        }
                    }

                } finally {
                    lock.unlock();
                }

                Thread.sleep(10); // to avoid tight loops...
            } // while true...
        } catch (InterruptedException ie) {
            // We do nothing, most probably, it fails due to the main service being stopped...
        } catch (Exception e) {
            e.printStackTrace();
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    @Override
    public BlockDownloaderHandlerConfig getConfig() {
        return this.config;
    }

    @Override
    public void updateConfig(HandlerConfig config) {
        if (!(config instanceof BlockDownloaderHandlerConfig)) {
            throw new RuntimeException("config class is NOT correct for this Handler");
        }
        try {
            lock.lock();
            this.config = (BlockDownloaderHandlerConfig) config;
            // We refresh the Download Strategy:
            setDownloadStrategy(this.config, this.blocksPendingManager, this.eventBus);
        } finally {
            lock.unlock();
        }
    }
}