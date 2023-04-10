package io.bitcoinsv.bsvcl.tools.unit.blobStore

import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.HeaderReadOnly
import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.Tx
import io.bitcoinsv.bitcoinjsv.bitcoin.bean.base.FullBlockBean
import io.bitcoinsv.bitcoinjsv.core.Sha256Hash
import io.bitcoinsv.bsvcl.tools.blobStore.BlockStorePosix
import io.bitcoinsv.bsvcl.tools.blobStore.BlockStorePosixConfig
import io.bitcoinsv.bsvcl.tools.common.TestingUtils
import shaded.org.apache.maven.wagon.ResourceDoesNotExistException
import spock.lang.Ignore
import spock.lang.Specification

import java.nio.file.Path
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

@Ignore
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

    def "test async saving large data"() {
        given:
        int batchSize = 10_000_000
        int totalBatches = 30
        ExecutorService threadpool = Executors.newCachedThreadPool();

        Path path = buildWorkingFolder()
        BlockStorePosixConfig blockStorePosixConfig = BlockStorePosixConfig.builder()
                .batchSize(batchSize)
                .workingFolder(path)
                .build()
        BlockStorePosix blockStorePosix = new BlockStorePosix(blockStorePosixConfig)

        HeaderReadOnly block1 = TestingUtils.buildBlock()
        HeaderReadOnly block2 = TestingUtils.buildBlock()

        byte[] batchData = new byte[batchSize]
        Arrays.fill(batchData, (byte)1)

        when:
            //save each block on a seperate thread
            threadpool.submit({ ->
                for (int i = 0; i < totalBatches; i++) {
                    blockStorePosix.saveBlock(block1, totalBatches, batchData)
                }
                blockStorePosix.commitBlock(block1.getHash());
            })

            threadpool.submit({ ->
                for (int i = 0; i < totalBatches; i++) {
                    blockStorePosix.saveBlock(block2, totalBatches, batchData)
                }
                blockStorePosix.commitBlock(block2.getHash())
            })

        //wait for the block to save
        while(!blockStorePosix.containsBlock(block1.getHash()) || !blockStorePosix.containsBlock(block2.getHash())){
            Thread.sleep(500)
        }

        blockStorePosix.commitBlock(block1.getHash())

        AtomicLong totalBytesBlock1 = new AtomicLong();
        AtomicLong totalBytesBlock2 = new AtomicLong();

        blockStorePosix.readBlockTxs(block1.getHash()).forEach({ b ->
            totalBytesBlock1.addAndGet(b.length);
        });

        blockStorePosix.readBlockTxs(block2.getHash()).forEach({ b ->
            totalBytesBlock2.addAndGet(b.length);
        });

        then:
        totalBytesBlock1.get() == (long)batchSize * totalBatches;
        totalBytesBlock2.get() == (long)batchSize * totalBatches;

        blockStorePosix.readBlockHeader(block1.getHash()) == block1
        blockStorePosix.readBlockHeader(block2.getHash()) == block2

        blockStorePosix.getBlockSize(block1.getHash()) == HeaderReadOnly.FIXED_MESSAGE_SIZE + totalBytesBlock1.get() + 1 //1 byte for VarInt numberOfTxs
        blockStorePosix.getBlockSize(block2.getHash()) == HeaderReadOnly.FIXED_MESSAGE_SIZE + totalBytesBlock2.get() + 1 //1 byte for VarInt numberOfTxs

        blockStorePosix.removeBlock(block1.getHash())
        blockStorePosix.removeBlock(block2.getHash())

        blockStorePosix.containsBlock(block1.getHash()) == false
        blockStorePosix.containsBlock(block2.getHash()) == false

        cleanup:
        blockStorePosix.clear()
    }

    def "test async reading of large data from different blocks"() {
        given:
        int batchSize = 10_000_000
        int totalBatches = 10
        int timeoutMs = 8000;

        ExecutorService threadpool = Executors.newCachedThreadPool();

        Path path = buildWorkingFolder()
        BlockStorePosixConfig blockStorePosixConfig = BlockStorePosixConfig.builder()
                .batchSize(batchSize)
                .workingFolder(path)
                .build()
        BlockStorePosix blockStorePosix = new BlockStorePosix(blockStorePosixConfig)

        HeaderReadOnly block1 = TestingUtils.buildBlock()
        HeaderReadOnly block2 = TestingUtils.buildBlock()

        byte[] batchData = new byte[batchSize]
        Arrays.fill(batchData, (byte)1)

        when:
        for (int i = 0; i < totalBatches; i++) {
            blockStorePosix.saveBlock(block1, totalBatches, batchData)
            blockStorePosix.saveBlock(block2, totalBatches, batchData)
        }
        blockStorePosix.commitBlock(block1.getHash())
        blockStorePosix.commitBlock(block2.getHash())

        AtomicLong totalBytesBlock1 = new AtomicLong();
        AtomicLong totalBytesBlock2 = new AtomicLong();

        threadpool.submit({ ->
            blockStorePosix.readBlockTxs(block1.getHash()).forEach({ b ->
                totalBytesBlock1.addAndGet(b.length);
            });
        })

        threadpool.submit({ ->
            blockStorePosix.readBlockTxs(block2.getHash()).forEach({ b ->
                totalBytesBlock2.addAndGet(b.length);
            });
        })

        //read block, timeout after 5 seconds
        long endTime = System.currentTimeMillis() + timeoutMs;

        while(System.currentTimeMillis() < endTime && (totalBytesBlock1.get() + totalBytesBlock2.get()) < (long)batchSize * totalBatches * 2){
            Thread.sleep(500)
        }

        then:
        totalBytesBlock1.get() == (long)batchSize * totalBatches;
        totalBytesBlock2.get() == (long)batchSize * totalBatches;

        blockStorePosix.readBlockHeader(block1.getHash()) == block1
        blockStorePosix.readBlockHeader(block2.getHash()) == block2

        blockStorePosix.getBlockSize(block1.getHash()) == HeaderReadOnly.FIXED_MESSAGE_SIZE + totalBytesBlock1.get() + 1 //1 byte for VarInt numberOfTxs
        blockStorePosix.getBlockSize(block2.getHash()) == HeaderReadOnly.FIXED_MESSAGE_SIZE + totalBytesBlock2.get() + 1 //1 byte for VarInt numberOfTxs

        blockStorePosix.removeBlock(block1.getHash())
        blockStorePosix.removeBlock(block2.getHash())

        blockStorePosix.containsBlock(block1.getHash()) == false
        blockStorePosix.containsBlock(block2.getHash()) == false


        cleanup:
        blockStorePosix.clear()
    }

    def "test async reading of large data from same block async"() {
        given:
        int batchSize = 10_000_000
        int totalBatches = 30
        int timeoutMs = 8000;

        ExecutorService threadpool = Executors.newCachedThreadPool();

        Path path = buildWorkingFolder()
        BlockStorePosixConfig blockStorePosixConfig = BlockStorePosixConfig.builder()
                .batchSize(batchSize)
                .workingFolder(path)
                .build()
        BlockStorePosix blockStorePosix = new BlockStorePosix(blockStorePosixConfig)

        HeaderReadOnly block1 = TestingUtils.buildBlock()

        byte[] batchData = new byte[batchSize]
        Arrays.fill(batchData, (byte)1)

        when:
        for (int i = 0; i < totalBatches; i++) {
            blockStorePosix.saveBlock(block1, totalBatches, batchData)
        }
        blockStorePosix.commitBlock(block1.getHash())

        AtomicLong totalBytesBlockFirstRead = new AtomicLong();
        AtomicLong totalBytesBlockSecondRead = new AtomicLong();

        threadpool.submit({ ->
            blockStorePosix.readBlockTxs(block1.getHash()).forEach({ b ->
                totalBytesBlockFirstRead.addAndGet(b.length);
            });
        })

        threadpool.submit({ ->
            blockStorePosix.readBlockTxs(block1.getHash()).forEach({ b ->
                totalBytesBlockSecondRead.addAndGet(b.length);
            });
        })


        //read block, timeout after 5 seconds
        long endTime = System.currentTimeMillis() + timeoutMs;

        while(System.currentTimeMillis() < endTime && (totalBytesBlockFirstRead.get() + totalBytesBlockSecondRead.get()) < (long)batchSize * totalBatches * 2){
            Thread.sleep(500)
        }

        then:
        totalBytesBlockFirstRead.get() == (long)batchSize * totalBatches;
        totalBytesBlockSecondRead.get() == (long)batchSize * totalBatches;

        blockStorePosix.readBlockHeader(block1.getHash()) == block1
        blockStorePosix.getBlockSize(block1.getHash()) == HeaderReadOnly.FIXED_MESSAGE_SIZE + totalBytesBlockFirstRead.get() + 1 //1 byte for VarInt numberOfTxs

        blockStorePosix.removeBlock(block1.getHash())
        blockStorePosix.containsBlock(block1.getHash()) == false

        cleanup:
        blockStorePosix.clear()
    }
}
