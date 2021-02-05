package com.nchain.jcl.net.protocol.handlers.block;


import com.nchain.jcl.net.protocol.events.*;
import com.nchain.jcl.net.protocol.messages.*;
import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.net.network.events.NetStartEvent;
import com.nchain.jcl.net.network.events.NetStopEvent;
import com.nchain.jcl.net.network.events.PeerDisconnectedEvent;
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg;
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsgBuilder;
import com.nchain.jcl.net.protocol.streams.deserializer.DeserializerStream;
import com.nchain.jcl.tools.config.RuntimeConfig;
import com.nchain.jcl.tools.handlers.HandlerImpl;
import com.nchain.jcl.tools.log.LoggerUtil;
import com.nchain.jcl.tools.thread.ThreadUtils;
import io.bitcoinj.core.Utils;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
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
 * - A "real time" way: When the Block is BIG. In thi case, the Whole block is deserialized at the same time as the
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
public class BlockDownloaderHandlerImpl extends HandlerImpl implements BlockDownloaderHandler {

    private LoggerUtil logger;
    @Getter private BlockDownloaderHandlerConfig config;

    // En Executor and a Listener to trigger jobs in parallels.
    private ExecutorService executor;

    // Info about each connected Peer and their downloading progress:
    private Map<PeerAddress, BlockPeerInfo> peersInfo = new ConcurrentHashMap<>();

    // Structures to keep track of the download process:
    private Map<String, Integer>    blocksNumDownloadAttempts = new ConcurrentHashMap<>();
    private Set<String>             blocksPending = ConcurrentHashMap.newKeySet();
    private List<String>            blocksDownloaded = new CopyOnWriteArrayList<>();
    private Map<String, Instant>    blocksDiscarded = new ConcurrentHashMap<>();

    // The Big Blocks will be Downloaded and Deserialized in Real Time, and we'll get notified every time a
    // block Header or a set of Txs are deserialized. So in order to know WHEN a Bock has been fully serialzied,
    // we need to keep track of the number of TXs contained in each block. When we detect that all the TXs within
    // a block have been deserialzied, we mark it as finished.

    private Map<String, BlockHeaderMsg>   bigBlocksHeaders   = new ConcurrentHashMap<>();
    private Map<String, Long>             bigBlocksCurrentTxs = new ConcurrentHashMap();

    /** Constructor */
    public BlockDownloaderHandlerImpl(String id, RuntimeConfig runtimeConfig, BlockDownloaderHandlerConfig config) {
        super(id, runtimeConfig);
        this.config = config;
        this.logger = new LoggerUtil(id, HANDLER_ID, this.getClass());
        this.executor = ThreadUtils.getSingleThreadExecutorService(id + "-Job");
    }

    private void registerForEvents() {
        super.eventBus.subscribe(NetStartEvent.class, e -> this.onNetStart((NetStartEvent) e));
        super.eventBus.subscribe(NetStopEvent.class, e -> this.onNetStop((NetStopEvent) e));
        super.eventBus.subscribe(PeerMsgReadyEvent.class, e -> this.onPeerMsgReady((PeerMsgReadyEvent) e));
        super.eventBus.subscribe(PeerHandshakedEvent.class, e -> this.onPeerHandshaked((PeerHandshakedEvent) e));
        super.eventBus.subscribe(PeerDisconnectedEvent.class, e -> this.onPeerDisconnected((PeerDisconnectedEvent) e));
        super.eventBus.subscribe(MsgReceivedEvent.class, e -> this.onMsgReceived((MsgReceivedEvent) e));

        super.eventBus.subscribe(BlocksDownloadRequest.class, e -> this.download(((BlocksDownloadRequest) e).getBlockHashes()));
    }

    @Override
    public BlockDownloaderHandlerState getState() {
        return BlockDownloaderHandlerState.builder()
                .pendingBlocks(this.blocksPending.stream().collect(Collectors.toList()))
                .downloadedBlocks(this.blocksDownloaded)
                .discardedBlocks(this.blocksDiscarded.keySet().stream().collect(Collectors.toList()))
                .blocksProgress(this.peersInfo.values().stream()
                        .filter( p -> p.getCurrentBlockInfo() != null)
                        .filter( p -> p.getWorkingState().equals(BlockPeerInfo.PeerWorkingState.PROCESSING))
                        .filter(p -> p.getConnectionState().equals(BlockPeerInfo.PeerConnectionState.HANDSHAKED))
                        .map(p -> p.getCurrentBlockInfo())
                        .collect(Collectors.toList()))
                .build();
    }

    @Override
    public void init() {
        registerForEvents();
    }

    @Override
    public void download(List<String> blockHashes) {
        logger.debug("Adding " + blockHashes.size() + " blocks to download: ");
        blockHashes.forEach(b -> logger.debug(" Block " + b));
        blocksPending.addAll(blockHashes);
    }

    // Event Handler:
    public void onNetStart(NetStartEvent event) {
        logger.debug("Starting...");
        executor.submit(this::jobProcessCheckDownloadingProcess);

    }
    // Event Handler:
    public void onNetStop(NetStopEvent event) {
        if (this.executor != null) executor.shutdownNow();
        logger.debug("Stop.");
    }
    // Event Handler:
    public void onPeerMsgReady(PeerMsgReadyEvent event) {
        // If the Peer is already in our Pool, that means it's been used before, so we just reset it, otherwise
        // we create a new one...
        PeerAddress peerAddress = event.getStream().getPeerAddress();
        BlockPeerInfo peerInfo = peersInfo.get(peerAddress);

        if (peerInfo == null) peerInfo = new BlockPeerInfo(peerAddress, (DeserializerStream) event.getStream().input());
        else                  peerInfo.connect((DeserializerStream)event.getStream().input());

        peersInfo.put(peerAddress, peerInfo);
    }
    // Event Handler:
    public void onPeerHandshaked(PeerHandshakedEvent event) {
        BlockPeerInfo peerInfo = peersInfo.get(event.getPeerAddress());
        peerInfo.handshake();
    }
    // Event Handler:
    public void onPeerDisconnected(PeerDisconnectedEvent event) {
        BlockPeerInfo peerInfo = peersInfo.get(event.getPeerAddress());
        if (peerInfo != null) {
            logger.trace(peerInfo.getPeerAddress(),  "Peer Disconnected", peerInfo.toString());
            // If this Peer was in the middle of downloading a block, we process the failiure...
            if (peerInfo.getWorkingState().equals(BlockPeerInfo.PeerWorkingState.PROCESSING))
                processDownloadFailiure(peerInfo);
            peerInfo.disconnect();
        }
    }

    // Event Handler:
    public void onMsgReceived(MsgReceivedEvent event) {
        if (!peersInfo.containsKey(event.getPeerAddress())) return;

        if (event.getBtcMsg().is(BlockMsg.MESSAGE_TYPE))
            processWholeBlockReceived(peersInfo.get(event.getPeerAddress()), (BitcoinMsg<BlockMsg>) event.getBtcMsg());
        if (event.getBtcMsg().is(PartialBlockHeaderMsg.MESSAGE_TYPE) || event.getBtcMsg().is(PartialBlockTXsMsg.MESSAGE_TYPE))
            processPartialBlockReceived(peersInfo.get(event.getPeerAddress()), event.getBtcMsg());
    }

    private synchronized void processPartialBlockReceived(BlockPeerInfo peerInfo, BitcoinMsg<?> msg) {

        if (msg.is(PartialBlockHeaderMsg.MESSAGE_TYPE)) {

            PartialBlockHeaderMsg partialMsg = (PartialBlockHeaderMsg) msg.getBody();
            String hash = partialMsg.getBlockHeader().getHash().toString();
            bigBlocksHeaders.put(hash, partialMsg.getBlockHeader());
            bigBlocksCurrentTxs.put(hash, 0L);

            // We notify it:
            super.eventBus.publish(new BlockHeaderDownloadedEvent(peerInfo.getPeerAddress(), partialMsg.getBlockHeader()));

        } else if (msg.is(PartialBlockTXsMsg.MESSAGE_TYPE)) {

            PartialBlockTXsMsg partialMsg = (PartialBlockTXsMsg) msg.getBody();
            String hash = partialMsg.getBlockHeader().getHash().toString();
            BlockHeaderMsg blockHeader = bigBlocksHeaders.get(hash);
            Long numCurrentTxs = bigBlocksCurrentTxs.get(hash) + partialMsg.getTxs().size();
            bigBlocksCurrentTxs.put(hash, numCurrentTxs);

            // We notify it:
            super.eventBus.publish(new BlockTXsDownloadedEvent(peerInfo.getPeerAddress(), partialMsg.getBlockHeader(), partialMsg.getTxs()));

            // Now we check if we've reached the total of TXs yet...
            if (numCurrentTxs.equals(blockHeader.getTransactionCount().getValue()))
                processDownloadSuccess(peerInfo, blockHeader, peerInfo.getCurrentBlockInfo().bytesTotal);
        }
    }

    private synchronized void processWholeBlockReceived(BlockPeerInfo peerInfo, BitcoinMsg<BlockMsg> blockMesage) {

        // We publish an specific event for this Lite Block being downloaded:
        super.eventBus.publish(LiteBlockDownloadedEvent.builder()
                    .peerAddress(peerInfo.getPeerAddress())
                    .block(blockMesage)
                    .downloadingTime(Duration.between(peerInfo.getCurrentBlockInfo().getStartTimestamp(), Instant.now()))
                    .build());

        // We notify the Header has been downloaded:
        super.eventBus.publish(new BlockHeaderDownloadedEvent(peerInfo.getPeerAddress(), blockMesage.getBody().getBlockHeader()));

        // We notify the tXs has been downloaded:
        super.eventBus.publish(new BlockTXsDownloadedEvent(peerInfo.getPeerAddress(), blockMesage.getBody().getBlockHeader(), blockMesage.getBody().getTransactionMsg()));

        processDownloadSuccess(peerInfo, blockMesage.getBody().getBlockHeader(), blockMesage.getLengthInbytes());
    }

    private void processDownloadSuccess(BlockPeerInfo peerInfo, BlockHeaderMsg blockHeader, long blockSize) {
        String blockHash = peerInfo.getCurrentBlockInfo().getHash();

        synchronized (this) {
            logger.debug(peerInfo.getPeerAddress(), "Block successfully downloaded", blockHash);

            // We activated back the ping/Pong Verifications for this Peer
            super.eventBus.publish(new EnablePingPongRequest(peerInfo.getPeerAddress()));

            // We publish an Event notifying that this Block being downloaded:
            super.eventBus.publish(BlockDownloadedEvent.builder()
                    .peerAddress(peerInfo.getPeerAddress())
                    .blockHeader(blockHeader)
                    .blockSize(blockSize)
                    .downloadingTime(Duration.between(peerInfo.getCurrentBlockInfo().getStartTimestamp(), Instant.now()))
                    .build());

            // We reset the peer, to make it ready for a new download, and we updateNextMessage the workingState:
            peerInfo.reset();
            peerInfo.getStream().resetBufferSize();

            blocksDownloaded.add(blockHash);
            blocksNumDownloadAttempts.remove(blockHash);
        }

    }


    // This method is called when the downloading process carried out by the Peer specified in the parameter has
    // failed for any reason (timeout, peer disconnected...). So now, depending on configuration, we give up on this
    // block or we can try again to download it with another Peer...

    private void processDownloadFailiure(BlockPeerInfo peerInfo) {
        if (peerInfo == null) return;
        synchronized (this) {
            if (peerInfo.getWorkingState().equals(BlockPeerInfo.PeerWorkingState.PROCESSING)) {
                String blockHash = peerInfo.getCurrentBlockInfo().getHash();
                int numAttempts = blocksNumDownloadAttempts.get(blockHash);
                if (numAttempts < config.getMaxDownloadAttempts()) {
                    logger.debug("Download failure for " + blockHash + " :: back to the pending Pool...");
                    blocksPending.add(blockHash);
                } else {
                    logger.debug("Download failure for " + blockHash, numAttempts + " attempts (max " + config.getMaxDownloadAttempts() + ")", "discarding Block...");
                    blocksDiscarded.put(blockHash, Instant.now());
                    // We publish the event:
                    super.eventBus.publish(new BlockDiscardedEvent(blockHash, BlockDiscardedEvent.DiscardedReason.TIMEOUT));
                }
            }

            // We activated back the ping/Pong Verifications for this Peer
            super.eventBus.publish(new EnablePingPongRequest(peerInfo.getPeerAddress()));
        }
    }

    private synchronized void startDownloading(BlockPeerInfo peerInfo, String blockHash) {
        logger.debug(peerInfo.getPeerAddress(), "Starting downloading Block " + blockHash);
        // We start the Dowbloading process with this Peer:
        // - WE update the Peer Info
        // - We disable the Ping/Pong monitor process on it, since it might be busy during the block downloading
        // - We update other structures (num Attempts on this block, and blocks pendings, etc):

        synchronized (this) {
            peerInfo.startDownloading(blockHash);
            peerInfo.getStream().upgradeBufferSize();

            super.eventBus.publish(new DisablePingPongRequest(peerInfo.getPeerAddress()));
            blocksNumDownloadAttempts.merge(blockHash, 1, (v1, v2) -> v1 + v2);
            blocksPending.remove(blockHash);

            // We use the Bitcoin Protocol to ask for that Block, sending a GETDATA message...
            HashMsg hashMsg =  HashMsg.builder().hash(Utils.HEX.decode(blockHash))
                    .build();
            InventoryVectorMsg invMsg = InventoryVectorMsg.builder()
                    .type(InventoryVectorMsg.VectorType.MSG_BLOCK)
                    .hashMsg(hashMsg)
                    .build();

            GetdataMsg msg = GetdataMsg.builder().invVectorList(Arrays.asList(invMsg)).build();
            BitcoinMsg<GetdataMsg> btcMsg = new BitcoinMsgBuilder<>(config.getBasicConfig(), msg).build();
            // We send the message
            super.eventBus.publish(new SendMsgRequest(peerInfo.getPeerAddress(), btcMsg));
        }
    }

    private void jobProcessCheckDownloadingProcess() {
        try {
            while (true) {
                //logger.trace("> Checking Download process and peers...");
                // On each execution of this Job, we loop over the list of Peers and process them one by one, checking
                // their workingState. We process the Peers ordered by Speed (high Speed first)

                List<BlockPeerInfo> peersOrdered =  peersInfo.values().stream().collect(Collectors.toList());
                Collections.sort(peersOrdered, BlockPeerInfo.SPEED_COMPARATOR);
                Iterator<BlockPeerInfo> it = peersOrdered.iterator();

                // After each execution, we might discover Peers that need to be removed, so we keep a reference to
                // those here, to remove them afterwards:
                Set<PeerAddress> peersToRemove = new HashSet<>();

                // We process each Peer...
                while (it.hasNext()) {

                    BlockPeerInfo peerInfo = it.next();
                    synchronized (peerInfo) {
                        PeerAddress peerAddress = peerInfo.getPeerAddress();
                        BlockPeerInfo.PeerWorkingState peerWorkingState = peerInfo.getWorkingState();
                        BlockPeerInfo.PeerConnectionState peerConnState = peerInfo.getConnectionState();

                        // If the Peer is NOT HANDSHAKED, we skip it...
                        if (!peerConnState.equals(BlockPeerInfo.PeerConnectionState.HANDSHAKED)) continue;

                        // we update the Progress of this Peer:
                        peerInfo.updateBytesProgress();

                        switch (peerWorkingState) {
                            case IDLE: {
                                // This peer is idle. If according to config we can download more Blocks, we assign one
                                // to it, if there is any...
                                if (blocksPending.size() > 0) {
                                    long numPeersWorking = peersInfo.values().stream()
                                            .filter( p -> p.getWorkingState().equals(BlockPeerInfo.PeerWorkingState.PROCESSING))
                                            .count();
                                    if (numPeersWorking < config.getMaxBlocksInParallel()) {
                                        String hash = blocksPending.stream().findFirst().get();
                                        logger.trace("Putting peer " + peerInfo.getPeerAddress() + " to download " + hash + "...");
                                        startDownloading(peerInfo, hash);
                                    }
                                }
                                break;
                            }

                            case PROCESSING:{
                                //logger.trace("> State: ", peerInfo.getPeerAddress(), peerInfo.toString());
                                String msgFailure = null;
                                if (peerInfo.isIdleTimeoutBroken(config.getMaxIdleTimeout())) {
                                    msgFailure = "Idle Time expired";
                                }
                                if (peerInfo.isDownloadTimeoutBroken(config.getMaxDownloadTimeout())) {
                                    msgFailure = "Downloading Time expired";
                                }
                                if (peerInfo.getConnectionState().equals(BlockPeerInfo.PeerConnectionState.DISCONNECTED)) {
                                    msgFailure = "Peer Closed while downloading";
                                }
                                if (msgFailure != null) {
                                    logger.debug(peerAddress.toString(), "Download Failiure", peerInfo.getCurrentBlockInfo().hash, msgFailure);
                                    processDownloadFailiure(peerInfo);
                                    peersToRemove.add(peerAddress);
                                }
                                break;
                            }
                        } // Switch...
                    }

                } // while it.next (each PeerInfo...

                // Now we remove the Peers we decided need to be removed:
                for (PeerAddress peerAddress : peersToRemove) peersInfo.remove(peerAddress);

                // Now we loop over the discarded Blocks, to check if any of them can be tried again:
                List<String> blocksToReTry = new ArrayList<>();
                for (String hashDiscarded: blocksDiscarded.keySet())
                    if (Duration.between(blocksDiscarded.get(hashDiscarded), Instant.now())
                            .compareTo(config.getRetryDiscardedBlocksTimeout()) > 0) blocksToReTry.add(hashDiscarded);

                for (String hashToRetry : blocksToReTry) {
                    blocksDiscarded.remove(hashToRetry);
                    blocksPending.add(hashToRetry);
                }

                Thread.sleep(100); // to avoid tight loops...
            } // while true...
        } catch (Exception e) {
            //e.printStackTrace(); // Most probably, it fails due to the main service being stopped...
        }
    }

}
