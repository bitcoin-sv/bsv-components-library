package io.bitcoinsv.jcl.tools.unit.bigObjects.stores

import io.bitcoinsv.bitcoinjsv.core.VarInt
import io.bitcoinsv.jcl.tools.bigObjects.BigCollectionChunk
import io.bitcoinsv.jcl.tools.bigObjects.BigCollectionChunkImpl
import io.bitcoinsv.jcl.tools.bigObjects.stores.BigCollectionChunksStore
import io.bitcoinsv.jcl.tools.bigObjects.stores.BigCollectionChunksStoreCMap3
import io.bitcoinsv.jcl.tools.bigObjects.stores.ObjectSerializer
import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader
import io.bitcoinsv.jcl.tools.bytes.ByteArrayWriter
import io.bitcoinsv.jcl.tools.config.RuntimeConfig
import io.bitcoinsv.jcl.tools.config.provided.RuntimeConfigDefault
import io.bitcoinsv.jcl.tools.serialization.BitcoinSerializerUtils
import shaded.org.apache.commons.io.FileUtils
import spock.lang.Ignore
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * A test to cehck if the BigCollectionChunksStore works well when we try to save a BIG Item
 * This can be used to simulate the scenario when we save a Big TX.
 */
class BigTxStoreTest extends Specification {

    // Block of the Hash we are saving this TX into (not important, it can be fake)
    private static String HASH = "0000000000000000090faac1f42f10b6cb9963096215259aa8e2d5019ecf7cea"

    // Size of the Dummy Tx (in bytes)
    private static int TX_SIZE = 33095950;

    // Id for the Store (A folder will be created with this name)
    private static String STORE_ID = "testingBigTxStore";

    // Properties used by the ChronicleMap Implementation
    private static final long   MAX_NUM_BLOCKS          = 300_000;          // total number of Block Headers
    private static final int    AVG_HEADER_KEY_SIZE     = 64;               // (bytes) hash in String format
    private static final long   AVG_TX_SIZE             = 5_00;             // av Tx size in average (wild guess)
    private static final long   TXS_FILE_SIZE           = 100_000_000;      // 100 MB

    // Item: It represents a TX
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

    @Ignore
    def "testing saving a Big Item"() {
        given:

        // We set up the Receiver:
        // We use ChronicleMap implementation
        // =======================================================================================================

        RuntimeConfig runtimeConfig = new RuntimeConfigDefault();

        // We instantiate the Receiver:
        String sourceExample = "[NETWORK] BSN : xxx.xxx.xxx.xxx:8444/xxx.xxx.xxx.xxx:8444";
        int avgCollectionIdSize = AVG_HEADER_KEY_SIZE + 5 + sourceExample.getBytes().length;

        BigCollectionChunksStore<BlockTxReceived> receiver = new BigCollectionChunksStoreCMap3(
                runtimeConfig,
                STORE_ID,
                new BlockTxSerializer(),
                avgCollectionIdSize,
                MAX_NUM_BLOCKS,
                AVG_TX_SIZE,
                (long) (TXS_FILE_SIZE / AVG_TX_SIZE)
        );

        when:
        // We remove the folder, to start clean:
        Path testFolder = Paths.get(runtimeConfig.getFileUtils().getRootPath().toString(), "store", STORE_ID);
        println("Testing folder: " + testFolder)
        FileUtils.deleteDirectory(testFolder.toFile());

        // We start the receiver:
        receiver.start();

        // We instantiate the dummy Tx:
        BlockTxReceived tx = new BlockTxReceived(new byte[TX_SIZE]);
        BigCollectionChunk<BlockTxReceived> chunkTx = new BigCollectionChunkImpl<>(List.of(tx), 0);
        receiver.save(HASH, chunkTx);

        // Now we iterate and return all the Txs....
        int txIndex = 0;
        Iterator<BigCollectionChunk<BlockTxReceived>> chunksReceived = receiver.getChunks(HASH);
        while (chunksReceived.hasNext()) {
            BigCollectionChunk<BlockTxReceived> chunk = chunksReceived.next();
            for (BlockTxReceived txReceived : chunk.getItems()) {
                System.out.println(">> Recovered Tx #" + txIndex + ", size: " + txReceived.txContent.length);
                txIndex++;
            }
        }

        // And we are done:
        receiver.stop();

        then:
        true
    }
}
