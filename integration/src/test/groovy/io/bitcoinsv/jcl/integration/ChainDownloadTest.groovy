/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.integration

import io.bitcoinsv.jcl.net.network.handlers.NetworkHandler
import io.bitcoinsv.jcl.net.protocol.config.ProtocolBasicConfig
import io.bitcoinsv.jcl.net.protocol.config.ProtocolConfig

import io.bitcoinsv.jcl.net.protocol.config.provided.ProtocolBSVMainConfig
import io.bitcoinsv.jcl.net.protocol.handlers.block.BlockDownloaderHandler
import io.bitcoinsv.jcl.net.protocol.handlers.block.BlockDownloaderHandlerConfig
import io.bitcoinsv.jcl.net.protocol.handlers.block.BlockDownloaderHandlerState
import io.bitcoinsv.jcl.net.protocol.handlers.block.BlockPeerInfo
import io.bitcoinsv.jcl.net.protocol.handlers.handshake.HandshakeHandler
import io.bitcoinsv.jcl.net.protocol.messages.BaseGetDataAndHeaderMsg
import io.bitcoinsv.jcl.net.protocol.messages.GetHeadersMsg
import io.bitcoinsv.jcl.net.protocol.messages.HashMsg
import io.bitcoinsv.jcl.net.protocol.messages.HeadersMsg
import io.bitcoinsv.jcl.net.protocol.messages.VarIntMsg
import io.bitcoinsv.jcl.net.protocol.wrapper.P2P
import io.bitcoinsv.jcl.net.protocol.wrapper.P2PBuilder
import io.bitcoinsv.jcl.store.blockChainStore.BlockChainStore
import io.bitcoinsv.jcl.store.blockStore.metadata.provided.BlockValidationMD
import io.bitcoinsv.jcl.store.keyValue.blockChainStore.BlockChainInfo
import io.bitcoinsv.jcl.store.levelDB.blockChainStore.BlockChainStoreLevelDB
import io.bitcoinsv.jcl.store.levelDB.blockChainStore.BlockChainStoreLevelDBConfig
import io.bitcoinsv.jcl.tools.config.RuntimeConfig
import io.bitcoinsv.jcl.tools.config.provided.RuntimeConfigDefault
import io.bitcoinsv.bitcoinjsv.bitcoin.Genesis
import io.bitcoinsv.bitcoinjsv.bitcoin.api.extended.ChainInfo
import io.bitcoinsv.bitcoinjsv.core.Sha256Hash
import io.bitcoinsv.bitcoinjsv.core.Utils
import io.bitcoinsv.bitcoinjsv.params.Net
import io.bitcoinsv.bitcoinjsv.params.NetworkParameters
import org.junit.Ignore
import spock.lang.Specification

import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.stream.Collectors

/**
 * This test will download the whole Chain. It uses JCl-Store to store the chain (mainly headers), and JCL-Net for
 * the P2P Communication. It performs a headers sync, and right after gettting the headers it requests the blocks for
 * download (they are requested ordered by height).
 * In case the tests is interrupted, next time it will read the DB looking for blocks which headers are stored but they
 * have not been downloaded, and it will start with that list.
 */
// @TODO: Ignored. It's time-consuming and the basic functionality is already tested in other sub-modules
@Ignore
class ChainDownloadTest extends Specification {

    // Maximum Duration of the Test:
    private static final Duration MAX_TEST_DURATION = Duration.ofMinutes(150) // 2.5 hours

    // Network used
    private static final NetworkParameters NETWORK_PARAMS = Net.of(Net.MAINNET)

    // JCL-NEt protocol Configuration. This should be obtained from the Network Parameters above, but the current
    // parameters in bitcoinJ are quite out-of-date and DNs perform very poorly, so we use the ProtocolConfig class
    // from JCL instead:
    private static final ProtocolConfig PROTOCOL_CONFIG = new ProtocolBSVMainConfig()

    // Basic Connection and Download Configuration
    private static final int MIN_PEERS = 30
    private static final int MAX_PEERS = 32
    private static final int MAX_BLOCK_PARALLEL = 10

    // Hardcoded Block Hashes that we need to IGNORE so we only Sync with BSV (and not BCH or BTC)
    private static final List<String> HEADERS_TO_IGNORE = Arrays.asList(
            "000000000000000000afe19d2ba3afbbc2627b1a6d7ee2425f998ddabd6134ed",
            "00000000000000000019f1679932c8a69051fca08d0934ac0c6cad56077d0c66",
            "0000000000000000004626ff6e3b936941d341c5932ece4357eeccac44e6d56c",
            "0000000000000000055de705ef722c11d90c1a52de52c21aa646c6bb46de3770")

    // We keep track of headers sync, blocks downloaded... so we do not request the same stuff twice:
    private static Set<Sha256Hash>  headersRequested = ConcurrentHashMap.newKeySet()
    private static Set<String>      blocksRequested = ConcurrentHashMap.newKeySet()
    private static AtomicLong numBlocksDownloaded = new AtomicLong()

    // It determines if the HEADERS message must be ignored for belonging to another Chain
    private boolean ignoreHeadersMsg(HeadersMsg msg) {
        boolean result = msg.getBlockHeaderMsgList().stream()
                            .map({ h -> Sha256Hash.wrapReversed(h.getHash().getHashBytes()).toString()})
                            .anyMatch({ h -> HEADERS_TO_IGNORE.contains(h)})
         return result;
    }

    // Given the list of Hashes, returns the minimum Height of all the blocks referrenced
    private int minHeight(BlockChainStore db, List<Sha256Hash> hashesReversed) {
        int result = Integer.MAX_VALUE
        for (Sha256Hash hash : hashesReversed) {
            Optional<BlockChainInfo> chainInfo = db.getBlockChainInfo(hash)
            if (chainInfo.isPresent()) result = Math.min(result, chainInfo.get().getHeight());
        }
        return result;
    }

    // Given the list of Hashes, returns the Maximum Height of all the blocks referrenced
    private int maxHeight(BlockChainStore db, List<Sha256Hash> hashesReversed) {
        int result = 1;
        for (Sha256Hash hash : hashesReversed) {
            Optional<BlockChainInfo> chainInfo = db.getBlockChainInfo(hash)
            if (chainInfo.isPresent()) result = Math.max(result, chainInfo.get().getHeight());
        }
        return result;
    }

    // If sends out a GET_HEADERS message to all the peers we are connected to.
    private synchronized void requestHeaders(P2P p2p, BlockChainStore db,  List<Sha256Hash> hashesReversed) {

        if (hashesReversed == null || hashesReversed.isEmpty()) { return }

        // We build the GET_HEADERS Message:
        List<HashMsg> hashMsgs = new ArrayList<>();
        for (Sha256Hash locatorHash : hashesReversed) {
            byte[] bytesReversed = Utils.reverseBytes(locatorHash.getBytes())
            HashMsg hashMsg = HashMsg.builder().hash(bytesReversed).build();
            hashMsgs.add(hashMsg);
        }

        BaseGetDataAndHeaderMsg baseGetDataAndHeaderMsg = BaseGetDataAndHeaderMsg.builder()
                .version(p2p.getProtocolConfig().getBasicConfig().getProtocolVersion())
                .blockLocatorHash(hashMsgs)
                .hashCount(VarIntMsg.builder().value(1).build())
                .hashStop(HashMsg.builder().hash(Sha256Hash.ZERO_HASH.getBytes()).build())
                .build();

        GetHeadersMsg getHeadersMsg = GetHeadersMsg.builder()
                .baseGetDataAndHeaderMsg(baseGetDataAndHeaderMsg)
                .build();

        // We send out the message:
        p2p.REQUESTS.MSGS.broadcast(getHeadersMsg).submit()

        // we mark it, so we do not request it twice:
        headersRequested.addAll(hashesReversed)
    }

    // It processes an incoming HEADERS message:
    // We store athe headers, so our chain gets updated, and we also request for download all the blocks between the
    // our previous tip (before saving the headers) and the new one (After saving them).
    private synchronized void receiveHeaders(P2P p2p, BlockChainStore db, HeadersMsg headersMsg) {

        if (ignoreHeadersMsg(headersMsg)) { return; }

        // We get a reference of the current Tip:
        List<Sha256Hash> currentTips = db.getTipsChains()

        // We save the Block Headers into the DB. We save them one by one, as we also need to save metadata for each...
        db.saveBlocks(headersMsg.getBlockHeaderMsgList().stream().map({m -> m.toBean()}).collect(Collectors.toList()))

        // We use the new Tips of the Chain as the next Headers to request:
        List<Sha256Hash> newChainTips = db.getTipsChains().stream()
            .filter({h -> !headersRequested.contains(h)})
            .collect(Collectors.toList())

        // We only continue if the tips of the chain have changed after the last addition:
        if (newChainTips != null && !newChainTips.isEmpty() && !currentTips.equals(newChainTips)) {
            if (newChainTips.size() == 1) {
                Optional<BlockChainInfo> tipChainInfo = db.getBlockChainInfo(newChainTips.get(0))
                println("Headers received, new Tip: " + newChainTips.get(0) + " Height: " + tipChainInfo.get().getHeight())
            } else {
                println("Headers received, " + newChainTips.size() + " tips updated...");
            }
            requestHeaders(p2p, db, newChainTips)


            // Now we request to download the blocks, all of them from the previous tip until the new one:
            int startHeight = Math.max(1, minHeight(db, currentTips))
            int stopHeight = maxHeight(db, newChainTips)
            List<String> blocksToDownload = new ArrayList<>()
            for (int i = startHeight; i <= stopHeight; i++) {
                Optional<ChainInfo> blockChainInfo = db.getBlock(i)
                String blockHash = blockChainInfo.get().getHeader().getHash().toString()
                if (!blocksRequested.contains(blockHash)) {
                    blocksToDownload.add(Utils.HEX.encode(blockChainInfo.get().getHeader().getHash().getBytes()))
                }
            }
            // we send out the GET_DATA Message to all the noes connected:
            println("Downloading Blocks, from #" + startHeight + " to #" + stopHeight + "...")
            p2p.REQUESTS.BLOCKS.download(blocksToDownload).submit()

            // we mark them, so we do not request them twice:
            blocksRequested.addAll(blocksRequested)
        }
    }

    // It processes a new block completey downloaded
    private void receiveBlock(P2P p2p, BlockChainStore db, Sha256Hash blockHash) {
        Sha256Hash blockHashReversed = Sha256Hash.wrapReversed(blockHash.getBytes())
        BlockValidationMD metadata = new BlockValidationMD()
        metadata.setDownloaded(true)
        db.saveBlockMetadata(blockHashReversed, metadata);
    }

    // Returns an initial list of blocks to download, from the DB: (blocks which header have been saved but they are
    // lacking the metadata linked to them when they are downloaded)
    private List<String> getBlocksToDownloadFromDB(BlockChainStore db) {
        List<String> result= new ArrayList<>()

        // we start from the beginning, and we loop over all the DB:
        int height = 1
        Optional<ChainInfo> blockChainInfo = db.getBlock(height);
        while (blockChainInfo.isPresent()) {
            Sha256Hash blockHash = blockChainInfo.get().getHeader().getHash()
            Optional<BlockValidationMD> metadata = (Optional<BlockValidationMD>) db.getBlockMetadata(blockHash)
            if (metadata.isEmpty() || (metadata.isPresent() &&  !metadata.get().isDownloaded())) {
                result.add(blockHash.toString())
            }
            height++
            blockChainInfo = db.getBlock(height)
        }
        return result;
    }

    /**
     * It downloads the Blockchain
     */
    def downloadChainSpec() {
        given:

            // Runtime Configuration
            RuntimeConfig runtimeConfig = new RuntimeConfigDefault()

            // JCL Protocol Configuration:

            // Protocol Basic Configuration
            ProtocolBasicConfig protocolBasicConfig = PROTOCOL_CONFIG.getBasicConfig().toBuilder()
                .minPeers(OptionalInt.of(MIN_PEERS))
                .maxPeers(OptionalInt.of(MAX_PEERS))
                .build()

            // Download Configuration:
            BlockDownloaderHandlerConfig downloadConfig = PROTOCOL_CONFIG.getBlockDownloaderConfig().toBuilder()
                .maxBlocksInParallel(MAX_BLOCK_PARALLEL)
                .maxDownloadAttempts(10)
                .maxDownloadTimeout(Duration.ofMinutes(20))
                .build()

            // We build the P2P Service:
            P2P p2p = new P2PBuilder("chainTest")
                    .config(runtimeConfig)
                    .config(PROTOCOL_CONFIG)
                    .config(protocolBasicConfig)
                    .config(downloadConfig)
                    .publishState(NetworkHandler.HANDLER_ID, Duration.ofSeconds(1))
                    .publishState(HandshakeHandler.HANDLER_ID, Duration.ofSeconds(1))
                    .publishState(BlockDownloaderHandler.HANDLER_ID, Duration.ofSeconds(1))
                    .build()

            // We obtain the BlockChainStore DB Service (LevelDB Implementation):
            BlockChainStoreLevelDBConfig dbConfig = BlockChainStoreLevelDBConfig.chainBuild()
                .runtimeConfig(runtimeConfig)
                .genesisBlock(Genesis.getHeaderFor(NETWORK_PARAMS.getNet()))
                .build()

            BlockChainStore db = BlockChainStoreLevelDB.chainStoreBuilder()
                .config(dbConfig)
                .enableAutomaticForkPrunning(true)
                .forkPrunningFrequency(Duration.ofSeconds(5))
                .blockMetadataClass(BlockValidationMD.class)
                .build()

            // We build the logic around the events triggered:
            AtomicBoolean minPeersReached = new AtomicBoolean(false)

            p2p.EVENTS.PEERS.HANDSHAKED_MIN_REACHED.forEach({ e ->
                minPeersReached.set(true)
            })

            p2p.EVENTS.MSGS.HEADERS.forEach({ m ->
                receiveHeaders(p2p, db, m.getBtcMsg().getBody())
            })

            p2p.EVENTS.BLOCKS.BLOCK_DOWNLOADED.forEach({ e ->
                Sha256Hash blockHash = Sha256Hash.wrap(e.getBlockHeader().getHash().getHashBytes())
                this.receiveBlock(p2p, db, blockHash)
            })

            p2p.EVENTS.STATE.HANDSHAKE.forEach({ e ->
                if (!minPeersReached.get()) println(e.getState())
            })

            p2p.EVENTS.STATE.BLOCKS.forEach({e ->
                if (minPeersReached.get()) {
                    BlockDownloaderHandlerState state = (BlockDownloaderHandlerState) e.getState()
                    int numPeersIdle = (int) state.getPeersInfo().stream()
                            .filter({p -> (p.getWorkingState().equals(BlockPeerInfo.PeerWorkingState.IDLE))})
                            .count();
                    int numPeersDiscarded = (int) state.getPeersInfo().stream()
                            .filter({p -> (p.getWorkingState().equals(BlockPeerInfo.PeerWorkingState.DISCARDED))})
                            .count();

                    println("BSN :: IDB process > Peers: [ " +
                            state.getNumPeersDownloading() + " downloading | " +
                            numPeersIdle + " idle | " +
                            numPeersDiscarded + " discarded" +
                            " ] Blocks: [ " +
                            state.getDownloadedBlocks().size() + " downloaded | " +
                            state.getPendingBlocks().size() + " pending | " +
                            state.getDiscardedBlocks().size() + " discarded | " +
                            state.getTotalReattempts() + " re-attempts | " +
                            state.getBusyPercentage() + "% busy" +
                            "]");
                }
            })

        when:

            println("Test Starting...")

            // We start the DB
            db.start()

            // We get the list of Blocks pending to downloaded from the Db. This list will be
            // the first one to download...
            println("Getting initial list of blocks to download...")
            List<String> blocksToDownload = getBlocksToDownloadFromDB(db)
            println("initial list of " + blocksToDownload.size() + " blocks loaded.")

            // we check the current tip:
            List<Sha256Hash> currentTips = db.getTipsChains()
            if (currentTips.size() > 1) {
                println("Current Tip is currently in a Fork!")
            } else {
                ChainInfo tipInfo = db.getBlockChainInfo(currentTips.get(0)).get()
                println("Current Tip :" + tipInfo.getHeader().getHash().toString() + ", Height #" + tipInfo.getHeight())
            }

            // We start the P2P Connection and we just wait until we reach enough peers:
            p2p.start()
            println("Starting Network activity...");

            while (!minPeersReached.get()) Thread.sleep(500)
            println("Minimum number of Peers reached.")

            // We use the current Tips of the Chain as a starting point for the headers sync
            println("Checking initial Hash Locator for Headers Sync...");

            println(currentTips.size() + " tips found. Starting Headers Sync and Chain Download...");
            requestHeaders(p2p, db, currentTips)
            p2p.REQUESTS.BLOCKS.download(blocksToDownload).submit()

            // We wait until the test finishes...
            Thread.sleep(MAX_TEST_DURATION.toMillis())

            // We stop all Services:
            db.stop()
            Thread.sleep(5) // we wait a bit...
            p2p.stop()
        then:
            true
    }
}
