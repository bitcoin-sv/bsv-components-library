package io.bitcoinsv.jcl.net.protocol.handlers.block;


import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.network.events.NetStartEvent;
import io.bitcoinsv.jcl.net.network.events.NetStopEvent;
import io.bitcoinsv.jcl.net.network.events.PeerDisconnectedEvent;
import io.bitcoinsv.jcl.net.protocol.events.control.*;
import io.bitcoinsv.jcl.net.protocol.events.data.*;
import io.bitcoinsv.jcl.net.protocol.messages.*;
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsg;
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsgBuilder;
import io.bitcoinsv.jcl.net.protocol.handlers.message.streams.deserializer.DeserializerStream;
import io.bitcoinsv.jcl.tools.config.RuntimeConfig;
import io.bitcoinsv.jcl.tools.handlers.HandlerImpl;
import io.bitcoinsv.jcl.net.tools.LoggerUtil;
import io.bitcoinsv.jcl.tools.thread.ThreadUtils;
import io.bitcoinsv.bitcoinjsv.core.Sha256Hash;
import io.bitcoinsv.bitcoinjsv.core.Utils;

import java.time.Duration;
import java.time.Instant;
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

    // If this is FALSE, it means that at this very moment no MORE downloads are allowed, until some blocks are finish
    private boolean moreDownloadsAllowed = true;
    // If TRUE, we reached the max MB to download at a given time
    private boolean bandwidthRestricted = false;

    // We keep track of the Block Download History:
    private BlocksDownloadHistory blocksDownloadHistory;

    // Basic Config:
    private LoggerUtil logger;
    private BlockDownloaderHandlerConfig config;

    // En Executor and a Listener to trigger jobs in parallels.
    private ExecutorService executor;

    // This Managers stores the list of Pending Blocks and has some logic inside to decide what is the "best" block
    // to download from an specific Peer, based on configuration:
    private BlocksPendingManager blocksPendingManager;

    // Structures to keep track of the download process:
    private List<String>            blocksDownloaded = new CopyOnWriteArrayList<>();
    private Map<String, Instant>    blocksDiscarded = new ConcurrentHashMap<>();

    // A block might fall into LIMBO if the download process gets interrupted: either by the Peer dropping the
    // connection, or the Peer becoming Idle. In these cases, we haven't received the whole content of the block
    // yet, but it might still be coming. So the Blocks goes to LIMBO, and it will stay there until it's
    // download finishes, or we detect that there has not been any activity for that block for some time, in which
    // case we can discard it:
    private Set<String> blocksInLimbo = ConcurrentHashMap.newKeySet();

    // Blocks can also be cancelled after they've benn requested for download. A block can only be cancelled if:
    // - the download of the block has not started yet
    // - the download of the block has started BUt there has been an error and now the block is in the
    //   "blocksFailedDuringDownload" set, where It will remain until it's re-attempted. But if the block is cancelled, it
    //   will bre removed from there and not re-attempted any more.

    private Set<String> blocksPendingToCancel = ConcurrentHashMap.newKeySet();
    private Set<String> blocksCancelled = ConcurrentHashMap.newKeySet();

    // If the block download fails for any reason while downloading a block,we process that as a FAILED.
    // But since the order of events is not guaranteed, it might be possible that the
    // block download is still going on, but the "disconnection" event has just arrived before it should have.
    // So the final Check to determine if a Block has really FAILED, is if we've processed the FAIL BUT ALSO there
    // has been no activity in this block for some time.
    // So we keep track of the last activity timestamp for any block, and the threshold mentioned before:

    private Map<String, Instant> blocksLastActivity = new ConcurrentHashMap<>();

    // We store in this list some "Notes" or "Events" that we might want to TRACE. These will be store din the State
    private List<String> downloadEvents = new ArrayList<>();

    // LOCK fort Multithread Sanity:
    Lock lock = new ReentrantLock();

    // We keep a reference to the List of blocks downloaded:
    private Set<String> liteBlocksDownloaded = ConcurrentHashMap.newKeySet();

    // The Big Blocks will be Downloaded and Deserialized in Real Time, and we'll get notified every time a
    // block Header or a set of Txs are deserialized. So in order to know WHEN a Bock has been fully serialized,
    // we need to keep track of the number of TXs contained in each block. When we detect that all the TXs within
    // a block have been deserialized, we mark it as finished.

    private Map<String, PartialBlockHeaderMsg>   bigBlocksHeaders       = new ConcurrentHashMap<>();
    private Map<String, Long>                    bigBlocksCurrentTxs    = new ConcurrentHashMap();

    // And A block might be downloaded in RAW Format, that means that each Batch of Txs contains a byte array of Txs. In
    // this case, we detect that we got the whole block when we received the total number of Bytes.
    private Map<String, Long>                    bigBlocksCurrentBytes  = new ConcurrentHashMap<>();

    // We keep track of some indicators:
    private AtomicLong totalReattempts = new AtomicLong();
    private AtomicInteger busyPercentage = new AtomicInteger();

    // This handler can pause/resume itself based on some state(number of Pers connected, MB being downloaded, etc)
    // But it can also pause/resume by specific events triggered by the client. So the order of preference is this:
    //  - If an specific Event requests to PAUSE, then we PAUSE no matter what.
    //  - If an specific event asks to Resume,then we resume as long as our internal state allows us to

    private boolean allowedToRunByClient = true;
    private boolean allowedToRunByInternalState = false;

    // We keep track also of the list of Rejections obtained for each Peer that can NOT download any block at all
    private Map<PeerAddress, BlocksPendingManager.DownloadFromPeerResponse> downloadRejections = new ConcurrentHashMap<>();

    /** Constructor */
    public BlockDownloaderHandlerImpl(String id, RuntimeConfig runtimeConfig, BlockDownloaderHandlerConfig config) {
        super(id, runtimeConfig);
        this.config = config;
        this.logger = new LoggerUtil(id, HANDLER_ID, this.getClass());
        this.executor = ThreadUtils.getSingleThreadExecutorService("JclBlockDownloaderHandler");
        this.downloadingState = DonwloadingState.RUNNING;
        this.blocksDownloadHistory = new BlocksDownloadHistory();
        this.blocksDownloadHistory.setCleaningTimeout(config.getBlockHistoryTimeout());

        // We configure the Blocks-Pending Manager:
        this.blocksPendingManager = new BlocksPendingManager();
        this.blocksPendingManager.setBestMatchCriteria(config.getBestMatchCriteria());
        this.blocksPendingManager.setBestMatchNotAvailableAction(config.getBestMatchNotAvailableAction());
        this.blocksPendingManager.setNoBestMatchAction(config.getNoBestMatchAction());

    }

    private void registerForEvents() {

        // Network Events:
        super.eventBus.subscribe(NetStartEvent.class, e -> this.onNetStart((NetStartEvent) e));
        super.eventBus.subscribe(NetStopEvent.class, e -> this.onNetStop((NetStopEvent) e));
        super.eventBus.subscribe(PeerMsgReadyEvent.class, e -> this.onPeerMsgReady((PeerMsgReadyEvent) e));
        super.eventBus.subscribe(PeerHandshakedEvent.class, e -> this.onPeerHandshaked((PeerHandshakedEvent) e));
        super.eventBus.subscribe(PeerDisconnectedEvent.class, e -> this.onPeerDisconnected((PeerDisconnectedEvent) e));
        super.eventBus.subscribe(MinHandshakedPeersReachedEvent.class, e -> this.resumeByInternalState());
        super.eventBus.subscribe(MinHandshakedPeersLostEvent.class, e -> this.pauseByInternalState());

        // Lite Blocks downloaded in a single go:
        super.eventBus.subscribe(BlockMsgReceivedEvent.class, e -> this.onBlockMsgReceived((BlockMsgReceivedEvent) e));
        super.eventBus.subscribe(RawBlockMsgReceivedEvent.class, e -> this.onBlockMsgReceived((RawBlockMsgReceivedEvent) e));

        // Big Blocks received in Batches:
        super.eventBus.subscribe(BlockHeaderDownloadedEvent.class, e -> this.onPartialBlockHeaderMsgReceived((BlockHeaderDownloadedEvent) e));
        super.eventBus.subscribe(BlockTXsDownloadedEvent.class, e -> this.onPartialBlockTxsMsgReceived((BlockTXsDownloadedEvent) e));
        super.eventBus.subscribe(BlockRawTXsDownloadedEvent.class, e -> this.onPartialBlockTxsMsgReceived((BlockRawTXsDownloadedEvent) e));

        // Download/Cancel requests:
        super.eventBus.subscribe(BlocksDownloadRequest.class, e -> this.download(
                ((BlocksDownloadRequest) e).getBlockHashes(),
                ((BlocksDownloadRequest) e).isWithPriority(),
                ((BlocksDownloadRequest) e).getFromThisPeerOnly(),
                ((BlocksDownloadRequest) e).getFromThisPeerPreferably()));
        super.eventBus.subscribe(BlocksCancelDownloadRequest.class, e -> this.cancelDownload(((BlocksCancelDownloadRequest) e).getBlockHashes()));
        super.eventBus.subscribe(BlocksDownloadStartRequest.class, e -> this.resumeByClient());
        super.eventBus.subscribe(BlocksDownloadPauseRequest.class, e -> this.pauseByClient());
        super.eventBus.subscribe(NotFoundMsgReceivedEvent.class, e -> this.onNotFoundMsg((NotFoundMsgReceivedEvent) e));

        // We get notified about a block being announced:
        super.eventBus.subscribe(InvMsgReceivedEvent.class, e -> this.onInvMsgReceived((InvMsgReceivedEvent) e));
    }

    // Returns the total size (in bytes) of all the blocks being downloaded at this moment
    public long getCurrentDownloadingBlocksSize() {
        return this.bigBlocksHeaders.values().stream().mapToLong(h -> h.getTxsSizeInbytes().getValue()).sum();
    }

    // Returns the number of Peers currently downloading blocks:
    public int getCurrentPeersDownloading() {
        return (int) handlerInfo.values().stream()
                .filter(p -> p.getWorkingState().equals(BlockPeerInfo.PeerWorkingState.PROCESSING))
                .count();
    }

    // Returns the list of Blocks being downloaded at this moment
    public List<String> getBlocksBeingDownloaded() {
        return handlerInfo.values().stream()
                .filter(p -> p.isProcessing())
                .map(p -> p.getCurrentBlockInfo().getHash())
                .collect(Collectors.toList());
    }

    // This method calculates the percentage of Thread occupation.
    // This value is an accumulative one, and it resets every time the "getState()" method is called.
    // It compares the number of blocks being downloaded to the maximum allowed.
    private int getUpdatedBusyPercentage() {
        int numBlocksInProgress = getCurrentPeersDownloading();
        int percentage = (int) (numBlocksInProgress * 100) / config.getMaxBlocksInParallel();
        int result = Math.max(this.busyPercentage.get(), percentage);
        return result;
    }

    // State-related methods:

    // These methods pause/resume the Handler:

    private void pause() {
        logger.debug("Pausing download...");
        this.downloadingState = DonwloadingState.PAUSED;
        this.blocksPendingManager.switchToRestrictedMode();
    }

    private void resume() {
        logger.debug("Resuming download...");
        this.downloadingState = DonwloadingState.RUNNING;
        this.blocksPendingManager.switchToNormalMode();
    }

    // If the client requests it or our state demands it, we pause right away:

    private void pauseByClient() {
        logger.debug("Pausing download by Client...");
        this.allowedToRunByClient = false;
        pause();
    }

    private void pauseByInternalState() {
        logger.debug("Pausing download by Internal State...");
        this.allowedToRunByInternalState = false;
        pause();
    }

    // In oder to Resume, it should be possible by both the client and internal State

    private void resumeByClient() {
        logger.debug("Resuming download by Client...");
        this.allowedToRunByClient = true;
        if (this.allowedToRunByInternalState) {
            resume();
        }
    }

    private void resumeByInternalState() {
        logger.debug("Resuming download by Internal State...");
        this.allowedToRunByInternalState = true;
        if (this.allowedToRunByClient) {
            resume();
        }
    }

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

        // We process this download as a failure immediately so it can be re-tried right away:
        logger.info("Block #{} NOT FOUND by Peer [{}]...", blockHash, peerInfoOpt.get().getPeerAddress());
        blocksDownloadHistory.register(blockHash,   "NOT FOUND Msg received from Peer [" + event.getPeerAddress() + "]");
        downloadEvents.add(Instant.now().toString() + " : Block #" + blockHash + " Not Found by [" + event.getPeerAddress() + "]");
        processDownloadFailure(blockHash);

    }

    private boolean isRunning() { return this.downloadingState.equals(DonwloadingState.RUNNING); }
    private boolean isPaused()  { return this.downloadingState.equals(DonwloadingState.PAUSED); }

    @Override
    public BlockDownloaderHandlerState getState() {
        // We get the percentage and we reset it right after that:
        int percentage = getUpdatedBusyPercentage();
        this.busyPercentage.set(0);

        long blocksDownloadingSize = this.bigBlocksHeaders.values().stream().mapToLong(h -> h.getTxsSizeInbytes().getValue()).sum();

        return BlockDownloaderHandlerState.builder()

                // Direct references:
                .pendingBlocks(this.blocksPendingManager.getPendingBlocks().stream().collect(Collectors.toList()))
                .downloadedBlocks(this.blocksDownloaded)
                .discardedBlocks(this.blocksDiscarded.keySet().stream().collect(Collectors.toList()))
                .pendingToCancelBlocks(this.blocksPendingToCancel.stream().collect(Collectors.toList()))
                .cancelledBlocks(this.blocksCancelled.stream().collect(Collectors.toList()))
                .blocksHistory(this.blocksDownloadHistory.getBlocksHistory())
                .blocksLastActivity(this.blocksLastActivity)
                .downloadEvents(this.downloadEvents)

                // Defensive Copies:
                .downloadRejections(new HashMap<>(this.downloadRejections))
                .peersInfo(new ArrayList(this.handlerInfo.values().stream().filter(p -> p.isHandshaked()).collect(Collectors.toList())))
                .totalReattempts(this.totalReattempts.get())
                .blocksNumDownloadAttempts(new HashMap<>(blocksPendingManager.getBlockDownloadAttempts()))
                .blocksInLimbo(new HashSet<>(this.blocksInLimbo))
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
        download(blockHashes, false, null, null);
    }

    @Override
    public void download(List<String> blockHashes, boolean withPriority) {
        download(blockHashes, false, null, null);
    }

    @Override
    public void download(List<String> blockHashes, boolean withPriority, PeerAddress fromThisPeerOnly, PeerAddress fromThisPeerPreferably) {
        try {
            lock.lock();
            // First we add the list of Block Hashes to the Pending Pool:
            logger.debug("Adding " + blockHashes.size() + " blocks to download (priority: " + withPriority +": ");

            // We filter out those blocks cancelled, etc...
            List<String> blocksBeingDownloadedNow = this.getBlocksBeingDownloaded();
            List<String> blockHashesToAdd =  blockHashes.stream()
                    .filter(h -> !blocksCancelled.contains(h))
                    .filter(h -> !blocksPendingToCancel.contains(h))
                    // NOTE: A block downloaded can be re-downloaded if the clients decides to...
                    // .filter(h -> !blocksDownloaded.contains(h))
                    .filter(h -> !blocksBeingDownloadedNow.contains(h))
                    .collect(Collectors.toList());

            // Now we add them all to the BlocksPendingManager:
            if (withPriority) {
                blocksPendingManager.addWithPriority(blockHashesToAdd);
            } else {
                blocksPendingManager.add(blockHashesToAdd);
            }

            // Now we add the Priorities Peers, if specified:
            if (fromThisPeerOnly != null) {
                blocksPendingManager.registerBlockExclusivity(blockHashesToAdd, fromThisPeerOnly);
            }
            if (fromThisPeerPreferably != null) {
                blocksPendingManager.registerBlockPriority(blockHashesToAdd, fromThisPeerPreferably);
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

    // Event Handler:
    public void onNetStart(NetStartEvent event) {
        logger.trace("Starting...");
        this.blocksDownloadHistory.start();
        executor.submit(this::jobProcessCheckDownloadingProcess);

    }

    // Event Handler:
    public void onNetStop(NetStopEvent event) {
        this.blocksDownloadHistory.stop();
        if (this.executor != null) executor.shutdownNow();
        logger.trace("Stop.");
    }

    // Event Handler:
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
    // Event Handler:
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
    // Event Handler:
    public void onPeerDisconnected(PeerDisconnectedEvent event) {
        try {
            lock.lock();
            BlockPeerInfo peerInfo = handlerInfo.get(event.getPeerAddress());
            if ((peerInfo != null) && peerInfo.isProcessing()) {
                // If this Peer was in the middle of downloading a block, we put this block in the LIMBO:
                blocksDownloadHistory.register(peerInfo.getCurrentBlockInfo().hash, peerInfo.getPeerAddress(), "Peer has disconnected");
                // Order is important here:
                // - 1: we put the block into Limbo so we wait a while until we retry...
                // - 2: we disconnect and we release the Stream so it can be gc collected
                putDownloadIntoLimbo(peerInfo);
                peerInfo.disconnect();
                logger.trace(peerInfo.getPeerAddress(),  "Peer Disconnected", peerInfo.toString());
            }
            downloadRejections.remove(event.getPeerAddress());
        } finally {
            lock.unlock();
        }
    }

    // Event Handler:
    // We register the Peers that are announcing Blocks:
    public void onInvMsgReceived(InvMsgReceivedEvent event) {
        event.getBtcMsg().getBody().getInvVectorList().stream()
                .filter(item -> item.getType().equals(InventoryVectorMsg.VectorType.MSG_BLOCK))
                .forEach(item -> {
                    String blockHash = Sha256Hash.wrapReversed(item.getHashMsg().getHashBytes()).toString();
                    blocksPendingManager.registerBlockAnnouncement(blockHash, event.getPeerAddress());
                });
    }

    // Event Handler:
    public void onBlockMsgReceived(BlockMsgReceivedEvent event) {
        try {
            lock.lock();
            if (!handlerInfo.containsKey(event.getPeerAddress())) return;
            processWholeBlockReceived(handlerInfo.get(event.getPeerAddress()), event.getBtcMsg());
        } finally {
            lock.unlock();
        }
    }

    // Event Handler:
    public void onBlockMsgReceived(RawBlockMsgReceivedEvent event) {
        try {
            lock.lock();
            if (!handlerInfo.containsKey(event.getPeerAddress())) return;
            processWholeRawBlockReceived(handlerInfo.get(event.getPeerAddress()), event.getBtcMsg());
        } finally {
            lock.unlock();
        }
    }

    // Event Handler:
    public void onPartialBlockHeaderMsgReceived(BlockHeaderDownloadedEvent event) {
        try {
            lock.lock();
            if (!handlerInfo.containsKey(event.getPeerAddress())) return;
            processPartialBlockReceived(handlerInfo.get(event.getPeerAddress()), event.getBtcMsg());
        } finally {
            lock.unlock();
        }
    }

    // Event Handler:
    public void onPartialBlockTxsMsgReceived(MsgReceivedEvent event) {
        try {
            lock.lock();
            if (!handlerInfo.containsKey(event.getPeerAddress())) return;
            processPartialBlockReceived(handlerInfo.get(event.getPeerAddress()), event.getBtcMsg());
        } finally {
            lock.unlock();
        }
    }

    private boolean isDownloadComplete(String blockHash) {
        boolean result = false;

        // If the Header has not been downloaded, then the result is FALSE:
        PartialBlockHeaderMsg partialHeaderMsg = this.bigBlocksHeaders.get(blockHash);

        if (partialHeaderMsg != null) {
            // This block might have been downloaded in Deserialized format or RAw format, so we check both:
            // If its been downloaded in Deserialized format, we compare the number of Txs downloaded so far
            // If its been downloaded in RAW format, we compare the number of bytes downloaded. This amount
            // compared is the size of the Txs part (blockSize - BlockHeader)

            if (this.bigBlocksCurrentTxs.containsKey(blockHash)) {
                result = this.bigBlocksCurrentTxs.get(blockHash).equals(partialHeaderMsg.getBlockHeader().getTransactionCount().getValue());
            } else if (this.bigBlocksCurrentBytes.containsKey(blockHash)) {
                result = this.bigBlocksCurrentBytes.get(blockHash).equals(partialHeaderMsg.getTxsSizeInbytes().getValue());
            }
        }
        return result;
    }

    private void processPartialBlockReceived(BlockPeerInfo peerInfo, BitcoinMsg<?> msg) {

        try {
            lock.lock();

            // The client of this library can expect that for every block downloaded, 3 types of Events are propagated:
            // - A: 1 "PartialBlockHeaderMsg" event when the Header is received
            // - B: several "BlockTXsDownloadedEvent" events, depending on the Block Size.
            // - C: 1 "BlockDownloadedEvent" event when the Block has been downloaded completely

            // This Handler receives the blocks from the EventBus in 2 ways:
            // - for BIG Blocks, we already receive the events "PartialBlockHeaderMsg" and "BlockTXsDownloadedEvent", we
            //   do some checks on them in this method and they are then propagated to the client.
            // - For LITE Blocks, we only receive a "BlockMsgReceivedEvent" event with the whole block.

            // So in order to propagate always the same type of events regardless of the Block size, for LITE blocks
            // we manually trigger the "PartialBlockHeaderMsg" and "BlockTXsDownloadedEvent" events, so the client can
            // subscribe to the same events no matter the block size.
            // But for this "artificially" triggered events its NOT necessary to process them here again, so we just
            // ignore them if they belong to a LITE Block:

            String blockHash = (msg.is(PartialBlockHeaderMsg.MESSAGE_TYPE))
                    ? Utils.HEX.encode(((PartialBlockHeaderMsg) msg.getBody()).getBlockHeader().getHash().getBytes())
                    : (msg.is(PartialBlockTXsMsg.MESSAGE_TYPE))
                    ? Utils.HEX.encode(((PartialBlockTXsMsg) msg.getBody()).getBlockHeader().getHash().getBytes())
                    : Utils.HEX.encode(((PartialBlockRawTxMsg) msg.getBody()).getBlockHeader().getHash().getBytes());

            // If this Event is triggered by this own class, we discard it (infinite loop)
            if (liteBlocksDownloaded.contains(blockHash)) {
                return;
            }

            if (msg.is(PartialBlockHeaderMsg.MESSAGE_TYPE)) {
                // We update the info about the Header this block:
                PartialBlockHeaderMsg partialMsg = (PartialBlockHeaderMsg) msg.getBody();
                bigBlocksHeaders.put(blockHash, partialMsg);
                blocksLastActivity.put(blockHash, Instant.now());
                blocksDownloadHistory.register(blockHash, peerInfo.getPeerAddress(), "Header downloaded");
            } else if (msg.is(PartialBlockTXsMsg.MESSAGE_TYPE)) {
                // We update the info about the Txs of this block:
                PartialBlockTXsMsg partialMsg = (PartialBlockTXsMsg) msg.getBody();
                bigBlocksCurrentTxs.merge(blockHash, (long) partialMsg.getTxs().size(), (o, n) -> o + partialMsg.getTxs().size());
                blocksLastActivity.put(blockHash, Instant.now());
                blocksDownloadHistory.register(blockHash, peerInfo.getPeerAddress(), partialMsg.getTxs().size() + " Txs downloaded, (" + bigBlocksCurrentTxs.get(blockHash) + " Txs so far)");
            } else if (msg.is(PartialBlockRawTxMsg.MESSAGE_TYPE)) {
                // We update the info about the Txs of this block:
                PartialBlockRawTxMsg partialMsg = (PartialBlockRawTxMsg) msg.getBody();
                bigBlocksCurrentTxs.merge(blockHash, (long) partialMsg.getTxs().size(), (o, n) -> o + partialMsg.getTxs().size());
                blocksLastActivity.put(blockHash, Instant.now());
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
            Duration downloadingDuration = (peerInfo != null && peerInfo.getCurrentBlockInfo() != null)
                    ? Duration.between(peerInfo.getCurrentBlockInfo().getStartTimestamp(), Instant.now())
                    : Duration.ZERO;

            // We publish and register it:
            super.eventBus.publish(new LiteBlockDownloadedEvent(peerInfo.getPeerAddress(), blockMesage, downloadingDuration));
            liteBlocksDownloaded.add(blockHash);

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
                    .build();
            BitcoinMsg<PartialBlockTXsMsg> partialBlockTxsBtcMsg = new BitcoinMsgBuilder<>(config.getBasicConfig(), partialBlockTxsMsg).build();
            super.eventBus.publish(new BlockTXsDownloadedEvent(peerInfo.getPeerAddress(),partialBlockTxsBtcMsg));

            // We update the structures:
            blocksLastActivity.put(blockHash, Instant.now());
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
            Duration downloadingDuration = (peerInfo != null && peerInfo.getCurrentBlockInfo() != null)
                    ? Duration.between(peerInfo.getCurrentBlockInfo().getStartTimestamp(), Instant.now())
                    : Duration.ZERO;

            // We publish and register it:
            super.eventBus.publish(new LiteRawBlockDownloadedEvent(peerInfo.getPeerAddress(), rawBlockMessage, downloadingDuration));
            liteBlocksDownloaded.add(blockHash);

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
                    .build();
            BitcoinMsg<PartialBlockRawTxMsg> partialBlockRawTxsBtcMsg = new BitcoinMsgBuilder<>(config.getBasicConfig(), partialBlockRawTxMsg).build();
            super.eventBus.publish(new BlockRawTXsDownloadedEvent(peerInfo.getPeerAddress(),partialBlockRawTxsBtcMsg));

            // We update the structures:
            blocksLastActivity.put(blockHash, Instant.now());
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

            // In both cases, we process this success...

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

            // We reset the peer, to make it ready for a new download, and we updateNextMessage the workingState:
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
                blocksInLimbo.remove(blockHash);

                blocksPendingToCancel.remove(blockHash);
                blocksCancelled.add(blockHash);

                blocksDownloadHistory.register(blockHash,   "block cancelled");
            }
        } finally {
            lock.unlock();
        }
    }

    private void putDownloadIntoLimbo(BlockPeerInfo peerInfo) {
        try {
            lock.lock();
            blocksInLimbo.add(peerInfo.getCurrentBlockInfo().getHash());
            peerInfo.setToLimbo();
        } finally {
            lock.unlock();
        }
    }

    private void processDownloadFailure(String blockHash) {
        try {
            lock.lock();

            // if the number of attempts tried for this block is no longer stored, that means that the block has been
            // succcesfully downloaded after all...
            if (!blocksPendingManager.isBlockBeingAttempted(blockHash)) {
                blocksInLimbo.remove(blockHash);
                return;
            }

            // If this block is in the "pendingToCancel" list, that means that this Block has been cancelled, so we do
            // not try to re-attempts it anymore...

            if (this.blocksPendingToCancel.contains(blockHash)) {
                cancelDownload(blockHash);
                return;
            }

            // We reset the Peer that is currently downloading this Block, if any....
            Optional<BlockPeerInfo> peerOpt = handlerInfo.values().stream().filter(p -> p.isDownloading(blockHash)).findFirst();
            if (peerOpt.isPresent()) {
                peerOpt.get().reset();
            }

            // We move the block back to the pool or discard it:
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

            // We update other structures (num Attempts on this block, and blocks pendings, etc):
            blocksLastActivity.put(blockHash, Instant.now());
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
                                    Optional<BlocksPendingManager.DownloadFromPeerResponse> downloadResponse = blocksPendingManager
                                            .extractMostSuitableBlockForDownload(peerAddress, availablePeers, notAvailablePeers);

                                    // possible outcomes:
                                    // - No blocks to assign: nothing to do:
                                    // - Assignment possible: we trigger the download
                                    // - Assignment rejected: we save this info for logging

                                    if (downloadResponse.isPresent()) {
                                        if (downloadResponse.get().isAssigned()) {
                                            startDownloading(peerInfo, downloadResponse.get().getAssignedResponse().getRequest().getBlockHash());
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

                                // We check the timeouts. If the peer has broken some of these timeouts, we discard it:
                                String msgFailure = null;
                                if (peerInfo.isIdleTimeoutBroken(config.getMaxIdleTimeout()))                             { msgFailure = "Idle Time expired"; peerInfo.setDownloadSpeed(0);}
                                if (peerInfo.isDownloadTimeoutBroken(config.getMaxDownloadTimeout()))                     { msgFailure = "Downloading Time expired"; }
                                if (peerInfo.getConnectionState().equals(BlockPeerInfo.PeerConnectionState.DISCONNECTED)) { msgFailure = "Peer Closed while downloading"; }

                                // We dont process a SLOW peer as a Failure anymore: Since we are NOT dropping the connections to
                                // Peers now, if a Peer is slow we cannot cancel the download since the Peer will still be sending
                                // as the block in the background, so we might be downloading the same blocks from multiple peers,
                                // which is not good.
                                //if (peerInfo.isTooSlow(config.getMinSpeed()))                                             { msgFailure = "Peer too slow"; }

                                if (msgFailure != null) {
                                    logger.debug(peerAddress, "Download Failure", peerInfo.getCurrentBlockInfo().hash, msgFailure);
                                    this.downloadEvents.add(Instant.now() + " : " + "Download Failure from [" + peerAddress + "] : " + msgFailure);
                                    blocksDownloadHistory.register(peerInfo.getCurrentBlockInfo().hash, peerInfo.getPeerAddress(), "Download Issue detected : " + msgFailure);
                                    putDownloadIntoLimbo(peerInfo);
                                    blocksPendingManager.registerDownloadFailure(peerInfo.getCurrentBlockInfo().hash, peerInfo.getPeerAddress());
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
                        Duration timePassedSinceLastActivity = Duration.between(blocksLastActivity.get(blockHash), Instant.now());
                        if (timePassedSinceLastActivity.compareTo(config.getInactivityTimeoutToFail()) > 0) {
                            processDownloadFailure(blockHash); // This block has definitely failed:
                        }
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

    public BlockDownloaderHandlerConfig getConfig() {
        return this.config;
    }
}