package io.bitcoinsv.bsvcl.net.protocol.handlers.block

import io.bitcoinsv.bitcoinjsv.core.Utils
import io.bitcoinsv.bitcoinjsv.core.VarInt
import io.bitcoinsv.bsvcl.common.bigObjects.BigCollectionChunk
import io.bitcoinsv.bsvcl.common.bigObjects.BigCollectionChunkImpl
import io.bitcoinsv.bsvcl.common.bigObjects.stores.BigCollectionChunksStore
import io.bitcoinsv.bsvcl.common.bigObjects.stores.BigCollectionChunksStoreCMap3
import io.bitcoinsv.bsvcl.common.bigObjects.stores.ObjectSerializer
import io.bitcoinsv.bsvcl.common.bytes.ByteArrayReader
import io.bitcoinsv.bsvcl.common.bytes.ByteArrayWriter
import io.bitcoinsv.bsvcl.common.config.RuntimeConfig
import io.bitcoinsv.bsvcl.common.config.provided.RuntimeConfigDefault
import io.bitcoinsv.bsvcl.common.serialization.BitcoinSerializerUtils
import io.bitcoinsv.bsvcl.net.P2P
import io.bitcoinsv.bsvcl.net.P2PBuilder
import org.apache.commons.io.FileUtils
import spock.lang.Ignore
import spock.lang.Specification

import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import java.util.stream.Collectors

/**
 * A test to Download an specific Block, save it in a BigCollectionChunksStore, and once its completely downloaded
 * we extract its chunks again and print them out on the console, to check that all the content has been downloaded
 * and saved properly.
 */

class BlockDownloadAndReceiverTest extends Specification {

    // Hash to Download and Save:
    //String HASH = "00000000000000000dc61d2fcd6495e835562a90db649d093e9eea660ccb5911";
    //String HASH = "000000000000000004f464f1cbd55f818e77ad61d444b9a40d5d59400ec27b9b";
    String HASH = "00000000000000000d3c90b80fa76954bca88a8599b856d5eeb9a224e5c3da61";

    // Id for the Store (A folder will be created with this name)
    private static String STORE_ID = "testingDownloadAndSaveBlock";

    // Item: It represents each TX downloaded.
    // Txs from JCL are wrapped into this class before saving
    // Each instance of this class contains one Tx in raw format.
    static class BlockTxReceived {
        // We work with Tx in raw format, without priori Deserialization.
        private final byte[] txContent;

        BlockTxReceived(byte[] txContent) {
            this.txContent = txContent;
        }
        byte[] getTxContent() {
            return this.txContent;
        }
    }

    // Item Serializer:
    // Converts each Tx to byte[] and viceversa
    static class BlockTxSerializer implements ObjectSerializer<BlockTxReceived> {
        @Override
        void serialize(BlockTxReceived object, ByteArrayWriter writer) {
            writer.write(new VarInt(object.getTxContent().length).encode());
            writer.write(object.getTxContent());
        }
        @Override
        BlockTxReceived deserialize(ByteArrayReader reader) {
            int numBytes = (int) BitcoinSerializerUtils.deserializeVarInt(reader);
            byte[] txContent = reader.read(numBytes);
            return new BlockTxReceived(txContent);
        }
    }

    /**
     * It processes the Block already downloaded: It gets all its chunks, and the Txs from those Chunks, and print
     * them out on the console, fir manual verification.
     */
    private void processBlockDownloaded(BigCollectionChunksStore<BlockTxReceived> receiver, String hash) {
        println(">>> Block #" + hash + " downloaded.");
        try {
            int txIndex = 0;
            Iterator<BigCollectionChunk<BlockTxReceived>> chunksReceived = receiver.getChunks(hash);
            while (chunksReceived.hasNext()) {
                BigCollectionChunk<BlockTxReceived> chunk = chunksReceived.next();
                for (BlockTxReceived txReceived : chunk.getItems()) {
                    System.out.println(" - Recovered Tx #" + txIndex + ", size: " + txReceived.txContent.length);
                    txIndex++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace()
        }
    }

    /**
     * It uses JCL-Net and JCL_Tools to set u a P2P Connection and store the Block in a BigCollectionChunksStore.
     */
    @Ignore
    def "testing downloading block and Receiving it"() {
        given:

        // We set up the P2P Object:
        // =======================================================================================================

        // Runtime Config:
        RuntimeConfig runtimeConfig = new RuntimeConfigDefault().toBuilder()
                .msgSizeInBytesForRealTimeProcessing(5_000_000)
                .build()

        // Network Config:
        io.bitcoinsv.bsvcl.net.network.config.NetworkConfig networkConfig = new io.bitcoinsv.bsvcl.net.network.config.provided.NetworkDefaultConfig().toBuilder()
                .maxSocketConnectionsOpeningAtSameTime(50)
                .build();

        // Protocol Config:
        io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig config = new io.bitcoinsv.bsvcl.net.protocol.config.provided.ProtocolBSVMainConfig();

        // Basic Config:
        io.bitcoinsv.bsvcl.net.protocol.config.ProtocolBasicConfig basicConfig = config.getBasicConfig().toBuilder()
                .minPeers(OptionalInt.of(5))
                .maxPeers(OptionalInt.of(10))
                .protocolVersion(io.bitcoinsv.bsvcl.net.protocol.config.ProtocolVersion.ENABLE_EXT_MSGS.getVersion())
                .build()

        // Serialization Config:
        io.bitcoinsv.bsvcl.net.protocol.handlers.message.MessageHandlerConfig messageConfig = config.getMessageConfig().toBuilder()
                .rawTxsEnabled(true)
                .verifyChecksum(true)
                .build();

        // We set up the Download configuration:
        io.bitcoinsv.bsvcl.net.protocol.handlers.block.BlockDownloaderHandlerConfig blockConfig = config.getBlockDownloaderConfig().toBuilder()
                .maxBlocksInParallel(3)
                .maxDownloadAttempts(100)
                .maxDownloadTimeout(Duration.ofMinutes(20))
                .maxIdleTimeout(Duration.ofSeconds(10))
                .removeBlockHistoryAfterDownload(false)
                .removeBlockHistoryAfter(Duration.ofMinutes(10))
                .build()

        // We extends the DiscoveryHandler Config, in case DNS's are not working properly:
        io.bitcoinsv.bsvcl.net.protocol.handlers.discovery.DiscoveryHandlerConfig discoveryConfig = io.bitcoinsv.bsvcl.net.integration.utils.IntegrationUtils.getDiscoveryHandlerConfigMainnet(config.getDiscoveryConfig())

        // We configure the P2P Service:
        P2P p2p = new P2PBuilder("testing")
                .config(config)
                .config(runtimeConfig)
                .config(networkConfig)
                .config(basicConfig)
                .config(messageConfig)
                .config(blockConfig)
                .config(discoveryConfig)
                .publishStates(Duration.ofMillis(500))
                .build()

        // We set up the Receiver:
        // We use ChronicleMap implementation
        // =======================================================================================================

        final int  AVG_HEADER_KEY_SIZE  = 64;            // (bytes) hash in String format
        final long MAX_NUM_BLOCKS       = 10;            // total number of Block Headers
        final long AVG_TX_SIZE          = 5_00;          // av Tx size in average (wild guess)
        final long TXS_FILE_SIZE        = 100_000_000;   // 100 MB each File

        // We configure the size of each Entry:
        String sourceExample = "[NETWORK] BSN : xxx.xxx.xxx.xxx:8444/xxx.xxx.xxx.xxx:8444";
        int avgCollectionIdSize = AVG_HEADER_KEY_SIZE + 5 + sourceExample.getBytes().length;

        BigCollectionChunksStore<BlockTxReceived> txStore = new BigCollectionChunksStoreCMap3<>(
                runtimeConfig,
                STORE_ID,
                new BlockTxSerializer(),
                avgCollectionIdSize + 20,
                MAX_NUM_BLOCKS,
                AVG_TX_SIZE,
                (long) (TXS_FILE_SIZE / AVG_TX_SIZE));

        // We Link P2P and the Receiver together:
        // =======================================================================================================

        // Some Flags to keep State:
        AtomicBoolean minPeersReached = new AtomicBoolean()
        AtomicBoolean blockDownloaded = new AtomicBoolean()

        p2p.EVENTS.PEERS.HANDSHAKED_MIN_REACHED.forEach({ e ->
            minPeersReached.set(true)
        })

        p2p.EVENTS.BLOCKS.BLOCK_RAW_TXS_DOWNLOADED.forEach({ e ->
            // We log some info:
            String hash = Utils.HEX.encode(e.getBtcMsg().body.getBlockHeader().getHash().getBytes())
            int numTxs = e.getBtcMsg().getBody().txs.size();
            int numBytes = e.getBtcMsg().getBody().getTxs().stream().mapToInt({tx -> tx.getContent().length}).sum()
            println(numTxs + " Txs downloaded. (" + numBytes + " bytes)");

            // We save it in the Store
            List<BlockTxReceived> txsReceived = e.getBtcMsg().getBody().getTxs().stream()
                    .map({rawTx -> new BlockTxReceived(rawTx.getContent())})
                    .collect(Collectors.toList())
            int chunkOrdinal = (int) e.getBtcMsg().getBody().getTxsOrderNumber().value;
            println("Saving Chunk #" + chunkOrdinal + " with " + txsReceived.size() + " Txs...")
            BigCollectionChunk<BlockTxReceived> chunk = new BigCollectionChunkImpl<>(txsReceived, chunkOrdinal)
            txStore.save(hash, chunk)
        })

        p2p.EVENTS.BLOCKS.BLOCK_DOWNLOADED.forEach({e ->
            // We log some info:
            String hash = Utils.HEX.encode(e.getBlockHeader().getHash().getBytes())
            println("Block #" + hash + " Downloaded.")

            // We put some delay so we make sure the TxStore has saved all the chunks:
            Thread.sleep(2_000)
            txStore.registerAsCompleted(hash)
            blockDownloaded.set(true)
        })

        when:

        // We remove the folder, to start clean:
        Path testFolder = Paths.get(runtimeConfig.getFileUtils().getRootPath().toString(), "store", STORE_ID);
        println("Testing folder: " + testFolder)
        FileUtils.deleteDirectory(testFolder.toFile());

        // We start the services:
        txStore.start()
        p2p.start()

        // Wait until we get enough connections:
        while (!minPeersReached.get()) {
            Thread.sleep(1_000);
        }

        println(">> Downloading Block #" + HASH + "...")
        p2p.REQUESTS.BLOCKS.download(HASH).submit()

        // Wait until Block is downloaded:
        while (!blockDownloaded.get()) {
            Thread.sleep(1_000)
        }

        // Extract Block content and print out on console
        processBlockDownloaded(txStore, HASH)

        // We are done:
        p2p.stop()
        txStore.stop()

        then:
            true
    }

}
