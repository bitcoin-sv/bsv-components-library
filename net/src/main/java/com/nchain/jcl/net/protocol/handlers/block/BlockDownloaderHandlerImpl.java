package com.nchain.jcl.net.protocol.handlers.block;


import com.nchain.jcl.net.network.PeerAddress;
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

    /**
     * This class stores an item in the history of a Block download process. Each item is an oepration performed on
     * a block that we want to keep track of...
     */
    public class BlockDownloadHistoryItem {
        private Instant timestamp;
        private String item;

        public BlockDownloadHistoryItem(String item) {
            this.timestamp = Instant.now();
            this.item = item;
        }
        @Override
        public String toString() { return timestamp + " :: " + item;}
    }
    // Blocks Download History:
    private Map<String, List<BlockDownloadHistoryItem>> blocksHistory = new ConcurrentHashMap<>();
    private boolean removeBlockHistoryAfterDownload = false;

    private LoggerUtil logger;
    private BlockDownloaderHandlerConfig config;

    // En Executor and a Listener to trigger jobs in parallels.
    private ExecutorService executor;

    // Info about each connected Peer and their downloading progress:
    private Map<PeerAddress, BlockPeerInfo> peersInfo = new ConcurrentHashMap<>();

    // Structures to keep track of the download process:
    private Map<String, Integer>    blocksNumDownloadAttempts = new ConcurrentHashMap<>();
    private Deque<String>           blocksPending = new ConcurrentLinkedDeque<>();
    private List<String>            blocksDownloaded = new CopyOnWriteArrayList<>();
    private Map<String, Instant>    blocksDiscarded = new ConcurrentHashMap<>();

    Lock lock = new ReentrantLock();

    // The Big Blocks will be Downloaded and Deserialized in Real Time, and we'll get notified every time a
    // block Header or a set of Txs are deserialized. So in order to know WHEN a Bock has been fully serialzied,
    // we need to keep track of the number of TXs contained in each block. When we detect that all the TXs within
    // a block have been deserialized, we mark it as finished.

    private Map<String, BlockHeaderMsg>   bigBlocksHeaders   = new ConcurrentHashMap<>();
    private Map<String, Long>             bigBlocksCurrentTxs = new ConcurrentHashMap();

    // We keep track of some indicators:
    private AtomicLong totalReattempts = new AtomicLong();
    private AtomicInteger busyPercentage = new AtomicInteger();

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
        super.eventBus.subscribe(BlockMsgReceivedEvent.class, e -> this.onBlockMsgReceived((BlockMsgReceivedEvent) e));
        super.eventBus.subscribe(PartialBlockHeaderMsgReceivedEvent.class, e -> this.onPartialBlockHeaderMsgReceived((PartialBlockHeaderMsgReceivedEvent) e));
        super.eventBus.subscribe(PartialBlockTxsMsgReceivedEvent.class, e -> this.onPartialBlockTxsMsgReceived((PartialBlockTxsMsgReceivedEvent) e));
        super.eventBus.subscribe(BlocksDownloadRequest.class, e -> this.download(((BlocksDownloadRequest) e).getBlockHashes()));
    }

    // This method calculates the percentage of Thread occupation.
    // This value is an accumulative one, and it resets every time the "getState()" method is called.
    // It compares the number of blocks being downloaded to the maximum allowed.
    private int getUpdatedBusyPercentage() {
        int numBlocksInProgress = (int) this.peersInfo.values().stream()
                .filter(p -> p.getWorkingState().equals(BlockPeerInfo.PeerWorkingState.PROCESSING))
                .count();
        int percentage = (int) (numBlocksInProgress * 100) / config.getMaxBlocksInParallel();
        int result = Math.max(this.busyPercentage.get(), percentage);
        return result;
    }

    // It registers a new line into this block download History:
    private void registerBlockHistory(String blockHash, String ...historyItems) {
        try {
            lock.lock();
            List<BlockDownloadHistoryItem> blockHistoryItems = blocksHistory.containsKey(blockHash)
                    ? blocksHistory.get(blockHash)
                    : new ArrayList<>();
            for (String historyItem : historyItems) {
                blockHistoryItems.add(new BlockDownloadHistoryItem(historyItem));
            }

            blocksHistory.put(blockHash, blockHistoryItems);
        } finally {
            lock.unlock();
        }
    }

    // Removes the block Download History for the block given:
    private void removeBlockHistory(String blockHash) {
        try {
            lock.lock();
            blocksHistory.remove(blockHash);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public BlockDownloaderHandlerState getState() {
        // We get the percentage and we reset it right after that:
        int percentage = getUpdatedBusyPercentage();
        this.busyPercentage.set(0);

        return BlockDownloaderHandlerState.builder()
                .pendingBlocks(this.blocksPending.stream().collect(Collectors.toList()))
                .downloadedBlocks(this.blocksDownloaded)
                .discardedBlocks(this.blocksDiscarded.keySet().stream().collect(Collectors.toList()))
                .blocksHistory(this.blocksHistory)
                .peersInfo(this.peersInfo.values().stream()
                        //.filter( p -> p.getCurrentBlockInfo() != null)
                        //.filter( p -> p.getWorkingState().equals(BlockPeerInfo.PeerWorkingState.PROCESSING))
                        .filter(p -> p.getConnectionState().equals(BlockPeerInfo.PeerConnectionState.HANDSHAKED))
                        .collect(Collectors.toList()))
                .totalReattempts(this.totalReattempts.get())
                .blocksNumDownloadAttempts(this.blocksNumDownloadAttempts)
                .busyPercentage(percentage)
                .build();
    }

    @Override
    public void init() {
        registerForEvents();
    }

    @Override
    public void download(List<String> blockHashes) {
        try {
            lock.lock();
            logger.debug("Adding " + blockHashes.size() + " blocks to download: ");
            blockHashes.forEach(b -> {
                logger.debug(" Block " + b);
                blocksPending.offer(b);
            });
        } finally {
            lock.unlock();
        }
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
        try {
            lock.lock();
            // If the Peer is already in our Pool, that means it's been used before, so we just reset it, otherwise
            // we create a new one...
            PeerAddress peerAddress = event.getStream().getPeerAddress();
            BlockPeerInfo peerInfo = peersInfo.get(peerAddress);

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

            peersInfo.put(peerAddress, peerInfo);
        } finally {
            lock.unlock();
        }
    }
    // Event Handler:
    public void onPeerHandshaked(PeerHandshakedEvent event) {
        try {
            lock.lock();
            BlockPeerInfo peerInfo = peersInfo.get(event.getPeerAddress());
            peerInfo.handshake();
        } finally {
            lock.unlock();
        }
    }
    // Event Handler:
    public void onPeerDisconnected(PeerDisconnectedEvent event) {
        try {
            lock.lock();
            BlockPeerInfo peerInfo = peersInfo.get(event.getPeerAddress());
            if (peerInfo != null) {
                logger.trace(peerInfo.getPeerAddress(),  "Peer Disconnected", peerInfo.toString());
                // If this Peer was in the middle of downloading a block, we process the failure...
                if (peerInfo.getWorkingState().equals(BlockPeerInfo.PeerWorkingState.PROCESSING)) {
                    registerBlockHistory(peerInfo.getCurrentBlockInfo().hash, "Peer " + peerInfo.getPeerAddress() + " has disconnected");
                    processDownloadFailiure(peerInfo, false);
                }
                peerInfo.disconnect();
            }
        } finally {
            lock.unlock();
        }
    }

    // Event Handler:
    public void onBlockMsgReceived(BlockMsgReceivedEvent event) {
        try {
            lock.lock();
            if (!peersInfo.containsKey(event.getPeerAddress())) return;
            processWholeBlockReceived(peersInfo.get(event.getPeerAddress()), (BitcoinMsg<BlockMsg>) event.getBtcMsg());
        } finally {
            lock.unlock();
        }
    }

    // Event Handler:
    public void onPartialBlockHeaderMsgReceived(PartialBlockHeaderMsgReceivedEvent event) {
        try {
            lock.lock();
            if (!peersInfo.containsKey(event.getPeerAddress())) return;
            processPartialBlockReceived(peersInfo.get(event.getPeerAddress()), event.getBtcMsg());
        } finally {
            lock.unlock();
        }
    }

    // Event Handler:
    public void onPartialBlockTxsMsgReceived(PartialBlockTxsMsgReceivedEvent event) {
        try {
            lock.lock();
            if (!peersInfo.containsKey(event.getPeerAddress())) return;
            processPartialBlockReceived(peersInfo.get(event.getPeerAddress()), event.getBtcMsg());
        } finally {
            lock.unlock();
        }
    }

    private void processPartialBlockReceived(BlockPeerInfo peerInfo, BitcoinMsg<?> msg) {

        try {
            lock.lock();
            if (msg.is(PartialBlockHeaderMsg.MESSAGE_TYPE)) {

                PartialBlockHeaderMsg partialMsg = (PartialBlockHeaderMsg) msg.getBody();
                String hash = partialMsg.getBlockHeader().getHash().toString();
                bigBlocksHeaders.put(hash, partialMsg.getBlockHeader());
                bigBlocksCurrentTxs.put(hash, 0L);

                registerBlockHistory(hash, "Header downloaded from " + peerInfo.getPeerAddress());

                // We notify it:
                super.eventBus.publish(new BlockHeaderDownloadedEvent(peerInfo.getPeerAddress(), partialMsg.getBlockHeader()));

            } else if (msg.is(PartialBlockTXsMsg.MESSAGE_TYPE)) {

                PartialBlockTXsMsg partialMsg = (PartialBlockTXsMsg) msg.getBody();
                String hash = partialMsg.getBlockHeader().getHash().toString();
                BlockHeaderMsg blockHeader = bigBlocksHeaders.get(hash);
                Long numCurrentTxs = bigBlocksCurrentTxs.get(hash) + partialMsg.getTxs().size();
                bigBlocksCurrentTxs.put(hash, numCurrentTxs);

                registerBlockHistory(hash, partialMsg.getTxs().size() +  "Txs downloaded from " + peerInfo.getPeerAddress() + ", (" + numCurrentTxs + " Txs so far)");

                // We notify it:
                super.eventBus.publish(new BlockTXsDownloadedEvent(peerInfo.getPeerAddress(), partialMsg.getBlockHeader(), partialMsg.getTxs(), partialMsg.getTxsOrderNumber().getValue()));

                // Now we check if we've reached the total of TXs yet:


                if (numCurrentTxs.equals(blockHeader.getTransactionCount().getValue())) {
                    // Due to the Multi-Thread nature of the EventBus, it might happen that when a Peer disconnects right
                    // after sending the last batch of Tx, the events do NOt come in the right order: the "Disconnect" event
                    // might come BEFORE the last "PartialTxsReceived". In that case, when receiving this Batch of Txs, the
                    //  peer might already be disconnected. In that case, we loos the info about the Amount of Bytes received
                    // for this Block.
                    // TODO: Can we store the Block Size elsewhere so we can return it???
                    Long blockSize = (peerInfo.getCurrentBlockInfo() != null) ? peerInfo.getCurrentBlockInfo().bytesTotal : null;
                    processDownloadSuccess(peerInfo, blockHeader, blockSize);
                }
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

            super.eventBus.publish(
                    new LiteBlockDownloadedEvent(
                            peerInfo.getPeerAddress(),
                            blockMesage,
                            downloadingDuration)
            );

            // We notify the Header has been downloaded:
            super.eventBus.publish(new BlockHeaderDownloadedEvent(peerInfo.getPeerAddress(), blockMesage.getBody().getBlockHeader()));

            // We notify the tXs has been downloaded:
            super.eventBus.publish(new BlockTXsDownloadedEvent(peerInfo.getPeerAddress(), blockMesage.getBody().getBlockHeader(), blockMesage.getBody().getTransactionMsg(), 0));

            registerBlockHistory(blockHash, "Whole Block downloaded from " + peerInfo.getPeerAddress());
            processDownloadSuccess(peerInfo, blockMesage.getBody().getBlockHeader(), blockMesage.getLengthInbytes());
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
            registerBlockHistory(blockHash, "Block successfully downloaded");
            if (removeBlockHistoryAfterDownload) { removeBlockHistory(blockHash);}

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
        } finally {
            lock.unlock();
        }
    }


    // This method is called when the downloading process carried out by the Peer specified in the parameter has
    // failed for any reason (timeout, peer disconnected...). So now, depending on configuration, we give up on this
    // block or we can try again to download it with another Peer...

    private void processDownloadFailiure(BlockPeerInfo peerInfo, boolean toDiscardPeer) {
        if (peerInfo == null) return;
        try {
            lock.lock();

            String blockHash = peerInfo.getCurrentBlockInfo().getHash();
            if (peerInfo.getWorkingState().equals(BlockPeerInfo.PeerWorkingState.PROCESSING)) {
                // This peer is not downloading anymore...
                peerInfo.setIdle();

                // HORRIBLE HOOK THAT SHOULD NEVER HAPPEN!!!
                if (!blocksNumDownloadAttempts.containsKey(blockHash)) {
                    logger.warm("Weird Scenario for " + peerInfo.getPeerAddress() + " :: num attempts for block " + blockHash + " not found!");
                    return;
                }

                int numAttempts = blocksNumDownloadAttempts.get(blockHash);
                if (numAttempts < config.getMaxDownloadAttempts()) {
                    logger.debug("Download failure for " + blockHash + " :: back to the pending Pool...");
                    registerBlockHistory(blockHash, "download failure", "back to the pending Pool");

                    blocksPending.offerFirst(blockHash);        // we add it to the FRONT of the Queue
                    this.totalReattempts.incrementAndGet();     // keep track of total re-attempts
                } else {
                    logger.debug("Download failure for " + blockHash, numAttempts + " attempts (max " + config.getMaxDownloadAttempts() + ")", "discarding Block...");
                    registerBlockHistory(blockHash, "download failure", "block discarded (max attempts broken)");
                    blocksDiscarded.put(blockHash, Instant.now());
                    // We publish the event:
                    super.eventBus.publish(new BlockDiscardedEvent(blockHash, BlockDiscardedEvent.DiscardedReason.TIMEOUT));
                }
            }
            if (toDiscardPeer) {
                // We discard this Peer and also sen a request to Disconnect from it:
                peerInfo.discard();
                super.eventBus.publish(new PeerDisconnectedEvent(peerInfo.getPeerAddress(), PeerDisconnectedEvent.DisconnectedReason.DISCONNECTED_BY_LOCAL_LAZY_DOWNLOAD));
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
            registerBlockHistory(blockHash, "Starting download from " + peerInfo.getPeerAddress().toString());

            // We update the Peer Info
            peerInfo.startDownloading(blockHash);
            peerInfo.getStream().upgradeBufferSize();

            // We disable the Ping/Pong monitor process on it, since it might be busy during the block downloading
            super.eventBus.publish(new DisablePingPongRequest(peerInfo.getPeerAddress()));

            // We update other structures (num Attempts on this block, and blocks pendings, etc):
            blocksNumDownloadAttempts.merge(blockHash, 1, (v1, v2) -> v1 + v2);
            blocksPending.remove(blockHash);

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

    private void jobProcessCheckDownloadingProcess() {
        try {
            while (true) {
                // On each execution of this Job, we loop over the list of Peers and process them one by one, checking
                // their workingState. We process the Peers ordered by Speed (high Speed first)

                List<BlockPeerInfo> peersOrdered =  peersInfo.values().stream().collect(Collectors.toList());
                Collections.sort(peersOrdered, BlockPeerInfo.SPEED_COMPARATOR);
                Iterator<BlockPeerInfo> it = peersOrdered.iterator();

                try {
                    lock.lock();
                    // We process each Peer...
                    while (it.hasNext()) {
                       BlockPeerInfo peerInfo = it.next();
                       PeerAddress peerAddress = peerInfo.getPeerAddress();
                       BlockPeerInfo.PeerWorkingState peerWorkingState = peerInfo.getWorkingState();
                       BlockPeerInfo.PeerConnectionState peerConnState = peerInfo.getConnectionState();

                       // If the Peer is NOT HANDSHAKED, we skip it...
                       if (!peerConnState.equals(BlockPeerInfo.PeerConnectionState.HANDSHAKED)) continue;

                       // we update the Progress of this Peer:
                       peerInfo.updateBytesProgress();

                       // We manage it based on its state:
                       switch (peerWorkingState) {
                          case IDLE: {
                              // This peer is idle. If according to config we can download more Blocks, we assign one
                              // to it, if there is any...

                              if (blocksPending.size() > 0) {
                                  long numPeersWorking = peersInfo.values().stream()
                                          .filter(p -> p.getWorkingState().equals(BlockPeerInfo.PeerWorkingState.PROCESSING))
                                          .count();
                                  if (numPeersWorking < config.getMaxBlocksInParallel()) {
                                      String hash = blocksPending.poll();
                                      logger.trace("Putting peer " + peerInfo.getPeerAddress() + " to download " + hash + "...");
                                      startDownloading(peerInfo, hash);
                                  }
                              }
                              break;
                          }

                          case PROCESSING: {
                              // We check the timeouts. If the peer has broken some of these timeouts, we discard it:

                              boolean toDiscard = false;
                              String msgFailure = null;
                              if (peerInfo.isIdleTimeoutBroken(config.getMaxIdleTimeout())) {
                                  msgFailure = "Idle Time expired";
                                  toDiscard = true;
                              }
                              if (peerInfo.isDownloadTimeoutBroken(config.getMaxDownloadTimeout())) {
                                  msgFailure = "Downloading Time expired";
                              }
                              if (peerInfo.getConnectionState().equals(BlockPeerInfo.PeerConnectionState.DISCONNECTED)) {
                                  msgFailure = "Peer Closed while downloading";
                              }
                              if (msgFailure != null) {
                                  logger.debug(peerAddress.toString(), "Download Failure", peerInfo.getCurrentBlockInfo().hash, msgFailure);
                                  registerBlockHistory(peerInfo.getCurrentBlockInfo().hash, "Download issue detected : " + msgFailure);
                                  processDownloadFailiure(peerInfo, toDiscard);
                              }
                              break;
                          }
                       } // Switch...
                    } // white it. next...

                    // Now we loop over the discarded Blocks, to check if any of them can be tried again:
                    List<String> blocksToReTry = new ArrayList<>();
                    for (String hashDiscarded: blocksDiscarded.keySet())
                        if (Duration.between(blocksDiscarded.get(hashDiscarded), Instant.now())
                                .compareTo(config.getRetryDiscardedBlocksTimeout()) > 0) blocksToReTry.add(hashDiscarded);

                    if (blocksToReTry.size() > 0) {
                        for (String hashToRetry : blocksToReTry) {
                              blocksDiscarded.remove(hashToRetry);
                              blocksPending.offerFirst(hashToRetry); // blocks to retry have preference...
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
