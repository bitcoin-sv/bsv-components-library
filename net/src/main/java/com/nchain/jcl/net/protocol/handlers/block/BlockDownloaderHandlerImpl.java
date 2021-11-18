package com.nchain.jcl.net.protocol.handlers.block;


import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.net.network.events.DisconnectPeerRequest;
import com.nchain.jcl.net.network.events.NetStartEvent;
import com.nchain.jcl.net.network.events.NetStopEvent;
import com.nchain.jcl.net.network.events.PeerDisconnectedEvent;
import com.nchain.jcl.net.protocol.events.control.*;
import com.nchain.jcl.net.protocol.events.data.*;
import com.nchain.jcl.net.protocol.messages.*;
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg;
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsgBuilder;
import com.nchain.jcl.net.protocol.streams.deserializer.DeserializerStream;
import com.nchain.jcl.tools.config.RuntimeConfig;
import com.nchain.jcl.tools.handlers.HandlerImpl;
import com.nchain.jcl.tools.log.LoggerUtil;
import com.nchain.jcl.tools.thread.ThreadUtils;
import io.bitcoinj.core.Sha256Hash;
import io.bitcoinj.core.Utils;

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
    private Map<String, Integer>    blocksNumDownloadAttempts = new ConcurrentHashMap<>();
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

    private Set<String>             blocksPendingToCancel = ConcurrentHashMap.newKeySet();
    private Set<String>             blocksCancelled = ConcurrentHashMap.newKeySet();

    // If the block download fails for any reason while downloading a block,we process that as a FAILED.
    // But since the order of events is not guaranteed, it might be possible that the
    // block download is still going on, but the "disconnection" event has just arrived before it should have.
    // So the final Check to determine if a Block has really FAILED, is if we've processed the FAIL BUT ALSO there
    // has been no activity in this block for some time.
    // So we keep track of the last activity timestamp for any block, and the threshold mentioned before:

    private Map<String, Instant>    blocksLastActivity = new ConcurrentHashMap<>();

    Lock lock = new ReentrantLock();

    // We keep a reference to the List of blocks downloaded:
    private Set<String>  liteBlocksDownloaded = ConcurrentHashMap.newKeySet();

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
        if (config.isOnlyDownloadAfterAnnouncement()) {
            blocksPendingManager.onlyDownloadAfterAnnouncement(true);
        }
        if (config.isDownloadFromAnnouncersFirst()) {
            blocksPendingManager.downloadFromAnnouncersFirst(true);
        }
    }

    private void registerForEvents() {

        // Network Events:
        super.eventBus.subscribe(NetStartEvent.class, e -> this.onNetStart((NetStartEvent) e));
        super.eventBus.subscribe(NetStopEvent.class, e -> this.onNetStop((NetStopEvent) e));
        super.eventBus.subscribe(PeerMsgReadyEvent.class, e -> this.onPeerMsgReady((PeerMsgReadyEvent) e));
        super.eventBus.subscribe(PeerHandshakedEvent.class, e -> this.onPeerHandshaked((PeerHandshakedEvent) e));
        super.eventBus.subscribe(PeerDisconnectedEvent.class, e -> this.onPeerDisconnected((PeerDisconnectedEvent) e));
        super.eventBus.subscribe(MaxHandshakedPeersReachedEvent.class, e -> this.resume());
        super.eventBus.subscribe(MinHandshakedPeersLostEvent.class, e -> this.pause());

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
        super.eventBus.subscribe(BlocksDownloadStartRequest.class, e -> this.resume());
        super.eventBus.subscribe(BlocksDownloadPauseRequest.class, e -> this.pause());

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
                .filter(p -> p.getWorkingState().equals(BlockPeerInfo.PeerWorkingState.PROCESSING))
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
    private void pause()        { this.downloadingState = DonwloadingState.PAUSED; }
    private void resume()       { this.downloadingState = DonwloadingState.RUNNING; }
    private boolean isRunning() { return this.downloadingState.equals(DonwloadingState.RUNNING); }

    @Override
    public BlockDownloaderHandlerState getState() {
        // We get the percentage and we reset it right after that:
        int percentage = getUpdatedBusyPercentage();
        this.busyPercentage.set(0);

        long blocksDownloadingSize = this.bigBlocksHeaders.values().stream().mapToLong(h -> h.getTxsSizeInbytes().getValue()).sum();

        return BlockDownloaderHandlerState.builder()
                .downloadingState(this.downloadingState)
                .pendingBlocks(this.blocksPendingManager.getPendingBlocks().stream().collect(Collectors.toList()))
                .downloadedBlocks(this.blocksDownloaded)
                .discardedBlocks(this.blocksDiscarded.keySet().stream().collect(Collectors.toList()))
                .pendingToCancelBlocks(this.blocksPendingToCancel.stream().collect(Collectors.toList()))
                .cancelledBlocks(this.blocksCancelled.stream().collect(Collectors.toList()))
                .blocksInLimbo(this.blocksInLimbo)
                .blocksHistory(this.blocksDownloadHistory.getBlocksHistory())
                .peersInfo(this.handlerInfo.values().stream()
                        //.filter( p -> p.getCurrentBlockInfo() != null)
                        //.filter( p -> p.getWorkingState().equals(BlockPeerInfo.PeerWorkingState.PROCESSING))
                        .filter(p -> p.getConnectionState().equals(BlockPeerInfo.PeerConnectionState.HANDSHAKED))
                        .collect(Collectors.toList()))
                .totalReattempts(this.totalReattempts.get())
                .blocksNumDownloadAttempts(this.blocksNumDownloadAttempts)
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
            blockHashes.stream()
                    .filter(h -> !blocksCancelled.contains(h))
                    .filter(h -> !blocksPendingToCancel.contains(h))
                    .forEach(b -> {
                        logger.debug(" Block " + b);
                        if (withPriority) {
                            blocksPendingManager.addWithPriority(b);
                        } else {
                            blocksPendingManager.add(b);
                        }
                    });
            // Now we add the Priorities Peers, if specified:
            if (fromThisPeerOnly != null) {
                blocksPendingManager.registerBlockExclusivity(blockHashes, fromThisPeerOnly);
            }
            if (fromThisPeerPreferably != null) {
                blocksPendingManager.registerBlockPriority(blockHashes, fromThisPeerPreferably);
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
        logger.debug("Starting...");
        this.blocksDownloadHistory.start();
        executor.submit(this::jobProcessCheckDownloadingProcess);

    }

    // Event Handler:
    public void onNetStop(NetStopEvent event) {
        this.blocksDownloadHistory.stop();
        if (this.executor != null) executor.shutdownNow();
        logger.debug("Stop.");
    }

    // Event Handler:
    public void onPeerMsgReady(PeerMsgReadyEvent event) {
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
        try {
            lock.lock();
            BlockPeerInfo peerInfo = handlerInfo.get(event.getPeerAddress());
            peerInfo.handshake();
        } finally {
            lock.unlock();
        }
    }
    // Event Handler:
    public void onPeerDisconnected(PeerDisconnectedEvent event) {
        try {
            lock.lock();
            BlockPeerInfo peerInfo = handlerInfo.get(event.getPeerAddress());
            if (peerInfo != null) {
                logger.trace(peerInfo.getPeerAddress(),  "Peer Disconnected", peerInfo.toString());
                // If this Peer was in the middle of downloading a block, we process the failure...
                if (peerInfo.getWorkingState().equals(BlockPeerInfo.PeerWorkingState.PROCESSING)) {
                    blocksDownloadHistory.register(peerInfo.getCurrentBlockInfo().hash, peerInfo.getPeerAddress(), "Peer has disconnected");
                    blocksInLimbo.add(peerInfo.getCurrentBlockInfo().hash);

                    // We process this failiure right away, no need to wait for the monitorJob to pick it up 1 minutes later
                    processDownloadFailiure(peerInfo.getCurrentBlockInfo().hash);
                }
                peerInfo.disconnect();
            }
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
            if (!handlerInfo.get(event.getPeerAddress()).isProcessing()) return;
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
            if (!handlerInfo.get(event.getPeerAddress()).isProcessing()) return;
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
            if (!handlerInfo.get(event.getPeerAddress()).isProcessing()) return;
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
            if (!handlerInfo.get(event.getPeerAddress()).isProcessing()) return;
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
                    ? Utils.HEX.encode(Utils.reverseBytes(((PartialBlockHeaderMsg) msg.getBody()).getBlockHeader().getHash().getHashBytes()))
                    : (msg.is(PartialBlockTXsMsg.MESSAGE_TYPE))
                    ? Utils.HEX.encode(Utils.reverseBytes(((PartialBlockTXsMsg) msg.getBody()).getBlockHeader().getHash().getHashBytes()))
                    : Utils.HEX.encode(Utils.reverseBytes(((PartialBlockRawTxMsg) msg.getBody()).getBlockHeader().getHash().getHashBytes()));

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
        // NOTE: Sometimes, remote Peers send BLOCKS to us Even If we did ask for them. In that case, the Downloading
        // time is ZERO, since we didn't ask for it so we cannot measure the downloading time...

        try {
            lock.lock();

            String blockHash = Utils.HEX.encode(Utils.reverseBytes(blockMesage.getBody().getBlockHeader().getHash().getHashBytes())).toString();
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
            processDownloadSuccess(peerInfo, blockMesage.getBody().getBlockHeader(), blockMesage.getLengthInbytes());
        } finally {
            lock.unlock();
        }
    }

    private void processWholeRawBlockReceived(BlockPeerInfo peerInfo, BitcoinMsg<RawTxBlockMsg> rawBlockMessage) {

        // We just received a Block.
        // We publish an specific event for this Lite Block being downloaded:
        // NOTE: Sometimes, remote Peers send BLOCKS to us Even If we did ask for them. In that case, the Downloading
        // time is ZERO, since we didn't ask for it so we cannot measure the downloading time...

        try {
            lock.lock();

            String blockHash = Utils.HEX.encode(Utils.reverseBytes(rawBlockMessage.getBody().getBlockHeader().getHash().getHashBytes())).toString();
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
            processDownloadSuccess(peerInfo, rawBlockMessage.getBody().getBlockHeader(), rawBlockMessage.getLengthInbytes());
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

            String blockHash = Utils.HEX.encode(Utils.reverseBytes(blockHeader.getHash().getHashBytes())).toString();

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
            peerInfo.reset();
            peerInfo.getStream().resetBufferSize();

            blocksDownloaded.add(blockHash);
            blocksNumDownloadAttempts.remove(blockHash);
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
            lock.tryLock();

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
                blocksPendingManager.remove(blockHash);
                blocksDiscarded.remove(blockHash);
                blocksNumDownloadAttempts.remove(blockHash);
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

    private void processDownloadFailiure(String blockHash) {
        try {
            lock.lock();

            // if the number of attempts tried for this block is no longer stored, that means that the block has been
            // succcesfully downloaded after all...
            if (!blocksNumDownloadAttempts.containsKey(blockHash)) {
                blocksInLimbo.remove(blockHash);
                return;
            }

            // If this block is in the "pendingToCancel" list, that means that this Block ha sbeen cencelled, so we do
            // not try to re-attempts it anymore...

            if (this.blocksPendingToCancel.contains(blockHash)) {
                cancelDownload(blockHash);
                return;
            }

            int numAttempts = blocksNumDownloadAttempts.get(blockHash);
            if (numAttempts < config.getMaxDownloadAttempts()) {
                logger.debug("Download failure for " + blockHash + " :: back to the pending Pool...");
                blocksDownloadHistory.register(blockHash, "Block moved back to the pending Pool");
                blocksPendingManager.addWithPriority(blockHash); // we add it to the FRONT of the Queue
                this.totalReattempts.incrementAndGet();                       // keep track of total re-attempts
            } else {
                logger.debug("Download failure for " + blockHash, numAttempts + " attempts (max " + config.getMaxDownloadAttempts() + ")", "discarding Block...");
                blocksDownloadHistory.register(blockHash,   "block discarded (max attempts broken, reset to zero)");
                blocksDiscarded.put(blockHash, Instant.now());
                // We reset the number of attempts, next time we try this block will start from scratch:
                blocksNumDownloadAttempts.remove(blockHash);
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

            // We update the Peer Info
            int numAttempts = blocksNumDownloadAttempts.containsKey(blockHash) ? blocksNumDownloadAttempts.get(blockHash) + 1 : 1;
            peerInfo.startDownloading(blockHash, numAttempts);
            peerInfo.getStream().upgradeBufferSize();

            // We disable the Ping/Pong monitor process on it, since it might be busy during the block downloading
            super.eventBus.publish(new DisablePingPongRequest(peerInfo.getPeerAddress()));

            // We update other structures (num Attempts on this block, and blocks pendings, etc):
            blocksLastActivity.put(blockHash, Instant.now());
            blocksNumDownloadAttempts.put(blockHash,numAttempts);
            blocksPendingManager.remove(blockHash);

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
                    List<BlockPeerInfo> peersOrdered =  handlerInfo.values().stream().collect(Collectors.toList());
                    Collections.sort(peersOrdered, BlockPeerInfo.SPEED_COMPARATOR);
                    Iterator<BlockPeerInfo> it = peersOrdered.iterator();

                    // We process each Peer...
                    while (it.hasNext()) {
                       BlockPeerInfo peerInfo = it.next();
                       PeerAddress peerAddress = peerInfo.getPeerAddress();
                       BlockPeerInfo.PeerWorkingState peerWorkingState = peerInfo.getWorkingState();

                       // If the Peer is NOT HANDSHAKED, we skip it...
                       if (!peerInfo.isHandshaked()) continue;

                       // we update the Progress of this Peer:
                       peerInfo.updateBytesProgress();

                       // We manage it based on its state:
                       switch (peerWorkingState) {
                          case IDLE: {
                              // SANITY CHECK: WE check if more downloads are allowed:
                              int numPeersWorking = getCurrentPeersDownloading();
                              long totalMBbeingDownloaded = getCurrentDownloadingBlocksSize() / 1_000_000; // convert to MB
                              this.bandwidthRestricted = totalMBbeingDownloaded >= config.getMaxMBinParallel();
                              this.moreDownloadsAllowed = (numPeersWorking == 0)
                                      || ((numPeersWorking < config.getMaxBlocksInParallel()) && !bandwidthRestricted);

                              // If we can download more Blocks, we ask the BlocksPendingManager for a Suitable block for
                              // this Peer to download:

                              if (isRunning() && moreDownloadsAllowed) {

                                  // In order to be efficient, the BlocksPendingManager also needs to know
                                  // about all the peers available for Download (EXCLUDING THIS ONE):
                                  List availablePeers = peersOrdered.stream()
                                          .filter(i -> !i.getPeerAddress().equals(peerAddress))
                                          .filter(i -> i.isHandshaked())
                                          .filter(i -> i.getWorkingState().equals(BlockPeerInfo.PeerWorkingState.IDLE))
                                          .map( i -> i.getPeerAddress())
                                          .collect(Collectors.toList());

                                  // We finally request a Peer to assign adn download from this Peer, if any has been found:
                                  Optional<String> blockHashToDownload = blocksPendingManager.extractMostSuitableBlockForDownload(peerAddress, availablePeers);
                                  if (blockHashToDownload.isPresent()) {
                                      startDownloading(peerInfo, blockHashToDownload.get());
                                  }
                              }
                              break;
                          }

                          case PROCESSING: {
                              // We check the timeouts. If the peer has broken some of these timeouts, we discard it:
                              String msgFailure = null;
                              if (peerInfo.isIdleTimeoutBroken(config.getMaxIdleTimeout()))                             { msgFailure = "Idle Time expired"; }
                              if (peerInfo.isDownloadTimeoutBroken(config.getMaxDownloadTimeout()))                     { msgFailure = "Downloading Time expired"; }
                              if (peerInfo.getConnectionState().equals(BlockPeerInfo.PeerConnectionState.DISCONNECTED)) { msgFailure = "Peer Closed while downloading"; }
                              if (msgFailure != null) {
                                  logger.debug(peerAddress.toString(), "Download Failure", peerInfo.getCurrentBlockInfo().hash, msgFailure);
                                  blocksDownloadHistory.register(peerInfo.getCurrentBlockInfo().hash, peerInfo.getPeerAddress(), "Download Issue detected : " + msgFailure);
                                  blocksInLimbo.add(peerInfo.getCurrentBlockInfo().hash);
                                  // We discard this Peer and also send a request to Disconnect from it:
                                  peerInfo.discard();
                                  super.eventBus.publish(new DisconnectPeerRequest(peerInfo.getPeerAddress(), PeerDisconnectedEvent.DisconnectedReason.DISCONNECTED_BY_LOCAL_LAZY_DOWNLOAD, null));
                                  //super.eventBus.publish(new PeerDisconnectedEvent(peerInfo.getPeerAddress(), PeerDisconnectedEvent.DisconnectedReason.DISCONNECTED_BY_LOCAL_LAZY_DOWNLOAD));
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
                            processDownloadFailiure(blockHash); // This block has definitely failed:
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

                Thread.sleep(100); // to avoid tight loops...
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
