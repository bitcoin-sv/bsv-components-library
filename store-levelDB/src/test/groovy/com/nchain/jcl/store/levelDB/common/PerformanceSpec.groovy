package com.nchain.jcl.store.levelDB.common

import com.google.common.io.MoreFiles
import com.google.common.io.RecursiveDeleteOption
import com.nchain.jcl.base.domain.api.base.BlockHeader
import com.nchain.jcl.base.domain.api.base.Tx
import com.nchain.jcl.store.blockStore.BlockStore
import com.nchain.jcl.store.levelDB.blockChainStore.BlockChainStoreLevelDB
import com.nchain.jcl.store.levelDB.blockChainStore.BlockChainStoreLevelDBConfig
import com.nchain.jcl.store.levelDB.blockStore.BlockStoreLevelDB
import com.nchain.jcl.store.levelDB.blockStore.BlockStoreLevelDBConfig
import spock.lang.Ignore
import spock.lang.Specification

import java.nio.file.Path
import java.time.Duration
import java.time.Instant


/**
 * A Testing class to check and measure performance.
 *
 * The tests here can be disabled in the Prod environment.
 */
class PerformanceSpec extends Specification {

    final static int NUM_BLOCK_BATCHES = 100   // Num of Batches
    final static int NUM_BLOCKS = 1000           // Num of Blocks within each Batch

    private void run(BlockStoreLevelDB db) {
        Instant init = Instant.now()
        long currentBlockIndex = 0;
        for (int i = 0; i < NUM_BLOCK_BATCHES; i++) {
            List<BlockHeader> blocks = new ArrayList<>();
            for (int b = 0; b < NUM_BLOCKS; b++) {
                BlockHeader blockHeader = TestingUtils.buildBlock()
                println("Saving Block #" + (currentBlockIndex++) + " : " + blockHeader.getHash().toString());
                blocks.add(blockHeader)
            }
            db.saveBlocks(blocks)
        }

        Duration time = Duration.between(init, Instant.now())
        println(" - " + time.toMillis() + " milliseconds.")

        // Using the testing method:
        println(" - " + db.getNumKeys("b:") + " Blocks inserted in the DB, (" + (currentBlockIndex / time.toSeconds()) + ") Blocks/sec")
        println(" - " + db.getNumKeys("tx:") + " Txs inserted in the DB")
        println(" - " + db.getNumKeys("btx:") + " relationships Block-Txs")
        println(" - " + db.getNumKeys("txb:") + " relationships Txs-Blocks")

        // We are inserting a lot of data, so we give it some time to consolidate before we clean it up...
        println("Waiting a bit to consolidate writes on the DB before cleaning up...")
        Thread.sleep(10000)
    }

    /**
     * Testing performance (only for local env, to disable on prod servers)
     */
    @Ignore
    def "testing BlockStore Performance"() {
        given:
            // Configuration and DB start up:
            Path dbPath = Path.of(TestingUtils.buildWorkingFolder())
            BlockStoreLevelDBConfig dbConfig = BlockStoreLevelDBConfig.builder()
                .workingFolder(dbPath)
                .build()
            BlockStore db = BlockStoreLevelDB.builder()
                .config(dbConfig)
                .build()
        when:
            run(db)
        then:
            true
        cleanup:
            MoreFiles.deleteRecursively(dbPath, RecursiveDeleteOption.ALLOW_INSECURE)
    }

    /**
     * Testing performance (only for local env, to disable on prod servers)
     */
    @Ignore
    def "testing BlockChainStore Performance"() {
        given:
            // Configuration and DB start up:
            Path dbPath = Path.of(TestingUtils.buildWorkingFolder())
            BlockHeader genesisBlock = TestingUtils.buildBlock()
            BlockChainStoreLevelDBConfig dbConfig =BlockChainStoreLevelDBConfig.chainBuild()
                    .workingFolder(dbPath)
                    .genesisBlock(genesisBlock)
                    .build()
            BlockStore db = BlockChainStoreLevelDB.chainBuilder()
                    .config(dbConfig)
                    .build()
        when:
            run(db)
        then:
            true
        cleanup:
            MoreFiles.deleteRecursively(dbPath, RecursiveDeleteOption.ALLOW_INSECURE)
    }
}
