package io.bitcoinsv.jcl.tools.unit.blobStore

import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.HeaderReadOnly
import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.Tx
import io.bitcoinsv.bitcoinjsv.bitcoin.bean.base.FullBlockBean
import io.bitcoinsv.bitcoinjsv.core.Sha256Hash
import io.bitcoinsv.jcl.tools.blobStore.BlockStorePosix
import io.bitcoinsv.jcl.tools.blobStore.BlockStorePosixConfig
import io.bitcoinsv.jcl.tools.common.TestingUtils
import shaded.org.apache.maven.wagon.ResourceDoesNotExistException
import spock.lang.Specification

import java.nio.file.Path
import java.util.concurrent.atomic.AtomicLong

class BlockStorePosixTest extends Specification {

    // Folder to store the LevelDB files in:
    private static final String DB_TEST_FOLDER = "testingDB";

    // Convenience method to generate a Random Folder to use as a working Folder:
    static Path buildWorkingFolder() {
        return Path.of(DB_TEST_FOLDER + "/test-" + new Random().nextInt(100) + new Random().nextInt(100))
    }

    def "test append saving and reading of blocks"() {
        given:
        int batchSize = 10_000;
        Path path = buildWorkingFolder()
        BlockStorePosixConfig blockStorePosixConfig = BlockStorePosixConfig.builder()
                .batchSize(batchSize)
                .workingFolder(path)
                .build()
        BlockStorePosix blockStorePosix = new BlockStorePosix(blockStorePosixConfig)

        Tx tx = TestingUtils.buildTx()
        HeaderReadOnly block = TestingUtils.buildBlock()

        Tx tx2 = TestingUtils.buildTx();

        when:
        blockStorePosix.saveBlock(block, 2, tx.serialize())
        blockStorePosix.saveBlock(block, 2, tx2.serialize());

        blockStorePosix.commitBlock(block.getHash())

        byte[] expectedOutput = new byte[tx.serialize().size() + tx2.serialize().size()]
        System.arraycopy(tx.serialize(), 0, expectedOutput, 0, tx.serialize().length)
        System.arraycopy(tx2.serialize(), 0, expectedOutput, tx.serialize().length, tx2.serialize().length);

        then:
        blockStorePosix.readBlockHeader(block.getHash()) == block
        blockStorePosix.readBlockTxs(block.getHash()).findFirst().get() == expectedOutput

        blockStorePosix.removeBlock(block.getHash())
        blockStorePosix.containsBlock(block.getHash()) == false

        cleanup:
        blockStorePosix.clear()
    }

    def "test write after failure"() {
        given:
        int batchSize = 10_000;
        Path path = buildWorkingFolder()
        BlockStorePosixConfig blockStorePosixConfig = BlockStorePosixConfig.builder()
                .batchSize(batchSize)
                .workingFolder(path)
                .build()
        BlockStorePosix blockStorePosix = new BlockStorePosix(blockStorePosixConfig)

        Tx tx = TestingUtils.buildTx()
        HeaderReadOnly block = TestingUtils.buildBlock()


        when:
        blockStorePosix.saveBlock(block, 1, tx.serialize())
        blockStorePosix.removeBlock(block.getHash())
        boolean containsRemovedBlock = blockStorePosix.containsBlock(block.getHash())

        blockStorePosix.saveBlock(block, 1, tx.serialize());
        blockStorePosix.commitBlock(block.getHash());

        then:
        !containsRemovedBlock
        blockStorePosix.containsBlock(block.getHash())
        blockStorePosix.readBlockTxs(block.getHash()).findFirst().get() == tx.serialize()

        cleanup:
        blockStorePosix.clear()
    }

    def "test saving and reading of blocks"() {
        given:
        int batchSize = 100_000;
        Path path = buildWorkingFolder()
        BlockStorePosixConfig blockStorePosixConfig = BlockStorePosixConfig.builder()
                .batchSize(batchSize)
                .workingFolder(path)
                .build()
        BlockStorePosix blockStorePosix = new BlockStorePosix(blockStorePosixConfig)

        Tx tx = TestingUtils.buildTx()
        HeaderReadOnly block = TestingUtils.buildBlock()

        FullBlockBean blockBean = new FullBlockBean()
        blockBean.setHeader(block)
        blockBean.setTransactions(List.of(tx))

        when:
        blockStorePosix.saveBlock(block, 1, tx.serialize())
        blockStorePosix.commitBlock(block.getHash())

        then:
        blockStorePosix.readBlockHeader(block.getHash()) == block
        blockStorePosix.readBlockTxs(block.getHash()).findFirst().get() == tx.serialize()
        Arrays.equals(blockStorePosix.readBlock(block.getHash()).findFirst().get(), blockBean.serialize())

        blockStorePosix.removeBlock(block.getHash())
        blockStorePosix.containsBlock(block.getHash()) == false

        cleanup:
        blockStorePosix.clear()
    }


    def "test read uncommited block read txs"() {
        given:
        int batchSize = 10_000;
        Path path = buildWorkingFolder()
        BlockStorePosixConfig blockStorePosixConfig = BlockStorePosixConfig.builder()
                .batchSize(batchSize)
                .workingFolder(path)
                .build()
        BlockStorePosix blockStorePosix = new BlockStorePosix(blockStorePosixConfig)

        Tx tx = TestingUtils.buildTx()
        HeaderReadOnly block = TestingUtils.buildBlock()

        when:
        blockStorePosix.saveBlock(block, 1, tx.serialize())
        blockStorePosix.readBlockTxs(block.getHash())

        then:
        thrown ResourceDoesNotExistException

        cleanup:
        blockStorePosix.clear()
    }

    def "test block uncommited read header"() {
        given:
        int batchSize = 10_000;
        Path path = buildWorkingFolder()
        BlockStorePosixConfig blockStorePosixConfig = BlockStorePosixConfig.builder()
                .batchSize(batchSize)
                .workingFolder(path)
                .build()
        BlockStorePosix blockStorePosix = new BlockStorePosix(blockStorePosixConfig)

        Tx tx = TestingUtils.buildTx()
        HeaderReadOnly block = TestingUtils.buildBlock()

        when:
        blockStorePosix.saveBlock(block, 1, tx.serialize())
        blockStorePosix.readBlockHeader(block.getHash())

        then:
        thrown ResourceDoesNotExistException

        cleanup:
        blockStorePosix.clear()
    }

    def "test block uncommitted size"() {
        given:
        int batchSize = 10_000;
        Path path = buildWorkingFolder()
        BlockStorePosixConfig blockStorePosixConfig = BlockStorePosixConfig.builder()
                .batchSize(batchSize)
                .workingFolder(path)
                .build()
        BlockStorePosix blockStorePosix = new BlockStorePosix(blockStorePosixConfig)

        Tx tx = TestingUtils.buildTx()
        HeaderReadOnly block = TestingUtils.buildBlock()

        when:
        blockStorePosix.saveBlock(block, 1, tx.serialize())
        blockStorePosix.getBlockSize(block.getHash())

        then:
        thrown ResourceDoesNotExistException

        cleanup:
        blockStorePosix.clear()
    }


    def "test saving and reading of large data"() {
        given:
        int batchSize = 100_000
        int totalBatches = 10

        Path path = buildWorkingFolder()
        BlockStorePosixConfig blockStorePosixConfig = BlockStorePosixConfig.builder()
                .batchSize(batchSize)
                .workingFolder(path)
                .build()
        BlockStorePosix blockStorePosix = new BlockStorePosix(blockStorePosixConfig)

        HeaderReadOnly block = TestingUtils.buildBlock()

        byte[] batchData = new byte[batchSize]
        Arrays.fill(batchData, (byte)1)

        when:
        for (int i = 0; i < totalBatches; i++) {
            blockStorePosix.saveBlock(block, totalBatches, batchData)
        }
        blockStorePosix.commitBlock(block.getHash())

        AtomicLong totalBytes = new AtomicLong();

        blockStorePosix.readBlockTxs(block.getHash()).forEach({ b ->
            totalBytes.addAndGet(b.length);
        });

        then:
        totalBytes.get() == (long)batchSize * totalBatches;
        blockStorePosix.readBlockHeader(block.getHash()) == block
        blockStorePosix.getBlockSize(block.getHash()) == HeaderReadOnly.FIXED_MESSAGE_SIZE + totalBytes.get() + 1 //1 byte for VarInt numberOfTxs

        blockStorePosix.removeBlock(block.getHash())
        blockStorePosix.containsBlock(block.getHash()) == false

        cleanup:
        blockStorePosix.clear()
    }

    def "test saving and readying block hashes"() {
        given:
        int batchSize = 10_000;
        Path path = buildWorkingFolder()
        BlockStorePosixConfig blockStorePosixConfig = BlockStorePosixConfig.builder()
                .batchSize(batchSize)
                .workingFolder(path)
                .build()
        BlockStorePosix blockStorePosix = new BlockStorePosix(blockStorePosixConfig)

        Tx tx = TestingUtils.buildTx()
        HeaderReadOnly block = TestingUtils.buildBlock()

        Tx tx2 = TestingUtils.buildTx();
        Tx tx3 = TestingUtils.buildTx()
        Tx tx4 = TestingUtils.buildTx();
        Tx tx5 = TestingUtils.buildTx();

        when:
        blockStorePosix.saveBlock(block, 2, tx.serialize())
        blockStorePosix.saveBlock(block, 2, tx2.serialize());
        blockStorePosix.saveBlock(block, 2, tx3.serialize())
        blockStorePosix.saveBlock(block, 2, tx4.serialize());

        List<Sha256Hash> blockHashes = new ArrayList<>();

        blockHashes.add(tx.getHash())
        blockHashes.add(tx2.getHash())

        blockStorePosix.saveBlockHashes(block, blockHashes)
        blockStorePosix.saveBlockHashes(block, Arrays.asList(tx3.getHash()))
        blockStorePosix.saveBlockHashes(block, Arrays.asList(tx4.getHash()))

        blockStorePosix.commitBlock(block.getHash())

        blockStorePosix.saveBlockHashes(block, Arrays.asList(tx5.getHash()))

        then:

        Sha256Hash[] hashes = blockStorePosix.readBlockTxHashes(block.getHash()).toArray();

        hashes[0] == tx.getHash()
        hashes[1] == tx2.getHash()
        hashes[2] == tx3.getHash()
        hashes[3] == tx4.getHash()

        thrown IllegalAccessException

        blockStorePosix.removeBlock(block.getHash())
        blockStorePosix.containsBlock(block.getHash()) == false

        cleanup:
        blockStorePosix.clear()
    }

    def "test contains fails for uncommited blocks"() {
        given:
        int batchSize = 10_000;
        Path path = buildWorkingFolder()
        BlockStorePosixConfig blockStorePosixConfig = BlockStorePosixConfig.builder()
                .batchSize(batchSize)
                .workingFolder(path)
                .build()
        BlockStorePosix blockStorePosix = new BlockStorePosix(blockStorePosixConfig)

        Tx tx = TestingUtils.buildTx()
        HeaderReadOnly block = TestingUtils.buildBlock()

        when:
        blockStorePosix.saveBlock(block, 2, tx.serialize())

        then:
        !blockStorePosix.containsBlock(block.getHash())

        blockStorePosix.removeBlock(block.getHash())
        blockStorePosix.containsBlock(block.getHash()) == false

        cleanup:
        blockStorePosix.clear()
    }


    def "test removal of uncommitted blocks"() {
        given:
        int batchSize = 100_000
        int totalBlocks = 100
        Path path = buildWorkingFolder()
        BlockStorePosixConfig blockStorePosixConfig = BlockStorePosixConfig.builder()
                .batchSize(batchSize)
                .workingFolder(path)
                .build()
        BlockStorePosix blockStorePosix = new BlockStorePosix(blockStorePosixConfig)

        Tx tx = TestingUtils.buildTx()
        HeaderReadOnly block = TestingUtils.buildBlock()

        when:
        blockStorePosix.saveBlock(block, 1, tx.serialize())

        List<Sha256Hash> blocksSaved = new ArrayList<>();
        for (int i = 0; i < totalBlocks; i++) {

            HeaderReadOnly b = TestingUtils.buildBlock();
            blockStorePosix.saveBlock(b, 1, new byte[1])

            blocksSaved.add(b .getHash())
        }

        blockStorePosix.commitBlock(block.getHash())
        blockStorePosix.clearUncommittedBlocks()

        then:
        blockStorePosix.containsBlock(block.getHash()) == true

        for (Sha256Hash blockHash: blocksSaved) {
            !blockStorePosix.containsBlock(blockHash)
            !blockStorePosix.commitBlock(blockHash)
        }

        cleanup:
        blockStorePosix.clear()
    }


}
