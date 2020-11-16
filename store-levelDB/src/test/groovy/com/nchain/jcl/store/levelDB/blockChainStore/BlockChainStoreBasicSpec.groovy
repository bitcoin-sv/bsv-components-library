package com.nchain.jcl.store.levelDB.blockChainStore

import com.google.common.io.MoreFiles
import com.google.common.io.RecursiveDeleteOption
import com.nchain.jcl.base.domain.api.base.BlockHeader
import com.nchain.jcl.base.domain.api.extended.ChainInfo
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper
import com.nchain.jcl.store.blockChainStore.BlockChainStore
import com.nchain.jcl.store.blockChainStore.events.ChainStateEvent
import com.nchain.jcl.store.levelDB.blockStore.BlockStoreLevelDBConfig
import com.nchain.jcl.store.levelDB.common.TestingUtils

import spock.lang.Specification

import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.stream.Collectors

/**
 * Basic Tests to verify tha the Chain info is saved and is consistent after saving or removing Blocks.
 */
class BlockChainStoreBasicSpec extends Specification {

    /**
     * We test that we can traverse a series of Blocks with a parent/child relationship back and forth
     */
    def "Testing Traversing Blocks"() {
        final int NUM_CHAIN_BLOCKS = 5
        given:
            // Configuration and DB start up:
            Path dbPath = Path.of(TestingUtils.buildWorkingFolder())
            BlockChainStoreLevelDBConfig dbConfig = BlockChainStoreLevelDBConfig.chainBuild()
                    .workingFolder(dbPath)
                    .build()
            BlockChainStore db = BlockChainStoreLevelDB.chainBuilder()
                    .config(dbConfig)
                    .triggerBlockEvents(true)
                    .build()

            // Now we insert a dummy chain of Blocks:
            BlockHeader genesisBlock = TestingUtils.buildBlock()
            db.saveBlock(genesisBlock)
            String parentHash = genesisBlock.getHash().toString()
            String tipHash = null
            for (int i = 1; i < NUM_CHAIN_BLOCKS; i++) {
                BlockHeader block = TestingUtils.buildBlock(parentHash)
                tipHash = block.getHash().toString()
                println("Saving " + parentHash + " > " + tipHash)
                db.saveBlock(block)
                parentHash = tipHash
            }
        when:
            // We take the Tip of the chain and we traverse the Chain back to the genesis block:
            // As we traverse the Chain we make sure that the loop over al the Blocks created previously:
            Set<String> hashesTraversedGenesis = new HashSet<>()
            println("traversing the Chain from the Tip (" + tipHash + "), to the genesis block:")
            Optional<BlockHeader> block = db.getBlock(Sha256Wrapper.wrap(tipHash))
            while (block.isPresent()) {
                hashesTraversedGenesis.add(block.get().getHash().toString())
                println(" - Block " + block.get().getHash() + " obtained.")
                block = db.getBlock(block.get().getPrevBlockHash())
            }

            boolean traverseToGenesisOK = hashesTraversedGenesis.size() == NUM_CHAIN_BLOCKS

            // Now we take the genesis block and we traverse the chain up to the Tip of the Chain:
            Set<String> hashesTraversedTip = new HashSet<>()
            Sha256Wrapper startBlockHash = genesisBlock.getHash()
            println("traversing the Chain from the Genesis (" + startBlockHash + "), to the genesis block:")
            while (startBlockHash != null) {
                hashesTraversedTip.add(startBlockHash.toString())
                println(" - Block " + startBlockHash + " obtained.")
                List<Sha256Wrapper> children = db.getNextBlocks(startBlockHash)
                startBlockHash = (children != null && children.size() > 0) ? children.get(0) : null
            }

            boolean traverseToTipOK = hashesTraversedTip.size() == NUM_CHAIN_BLOCKS

        then:
            traverseToGenesisOK
            traverseToTipOK
        cleanup:
            MoreFiles.deleteRecursively(dbPath, RecursiveDeleteOption.ALLOW_INSECURE)
    }

    /**
     * We tests that we save/remove blocks wiht a parent/child relationship, and the "ChainsTips" information is
     * updated properly, reflecting always the tip of the Chain.
     */
    def "testing saving/removing Blocks and Chain Tips updates"() {
        given:
            // Configuration and DB start up:
            Path dbPath = Path.of(TestingUtils.buildWorkingFolder())
            BlockHeader block_genesis = TestingUtils.buildBlock()
            println(" - Genesis Block: " + block_genesis.getHash())
            BlockChainStoreLevelDBConfig dbConfig = BlockChainStoreLevelDBConfig.chainBuild()
                    .workingFolder(dbPath)
                    .genesisBlock(block_genesis)
                    .build()
            BlockChainStore db = BlockChainStoreLevelDB.chainBuilder()
                    .config(dbConfig)
                    .triggerBlockEvents(true)
                    .build()

            // We keep track of the Events triggered:
            AtomicInteger numBlocksSavedEvents = new AtomicInteger()
            AtomicInteger numBlocksRemovedEvents = new AtomicInteger()

            db.EVENTS().BLOCKS_SAVED.forEach({e -> numBlocksSavedEvents.incrementAndGet()})
            db.EVENTS().BLOCKS_REMOVED.forEach({e -> numBlocksRemovedEvents.incrementAndGet()})

        when:
            db.start()
            List<Sha256Wrapper> tipsChain = db.getTipsChains()
            ChainInfo tipChainInfo = db.getBlockChainInfo(tipsChain.get(0)).get()
            boolean beforeInserting = (tipsChain.size() == 1) && tipsChain.contains(block_genesis.getHash()) &&
                    tipChainInfo.getHeight() == 0

            // We insert Blocks 1, 2 and 3 ( [genesis] -> 1 -> 2 -> 3)
            BlockHeader block_1 = TestingUtils.buildBlock(block_genesis.getHash().toString())
            db.saveBlock(block_1)
            println(" - Saving Block 1: " + block_1.getHash())
            tipsChain = db.getTipsChains()
            tipChainInfo = db.getBlockChainInfo(tipsChain.get(0)).get()
            Thread.sleep(50) // a bit of a delay, to trigger the events
            boolean afterInsert1_OK = (tipsChain.size() == 1) && tipsChain.contains(block_1.getHash()) &&
                    tipChainInfo.getHeight() == 1 && numBlocksSavedEvents.get() == 1

            BlockHeader block_2 = TestingUtils.buildBlock(block_1.getHash().toString())
            db.saveBlock(block_2)
            println(" - Saving Block 2: " + block_2.getHash())
            tipsChain = db.getTipsChains()
            tipChainInfo = db.getBlockChainInfo(tipsChain.get(0)).get()
            Thread.sleep(50) // a bit of a delay, to trigger the events
            boolean afterInsert2_OK = (tipsChain.size() == 1) && tipsChain.contains(block_2.getHash()) &&
                    tipChainInfo.getHeight() == 2 && numBlocksSavedEvents.get() == 2

            BlockHeader block_3 = TestingUtils.buildBlock(block_2.getHash().toString())
            db.saveBlock(block_3)
            println(" - Saving Block 3: " + block_3.getHash())
            tipsChain = db.getTipsChains()
            tipChainInfo = db.getBlockChainInfo(tipsChain.get(0)).get()
            Thread.sleep(50) // a bit of a delay, to trigger the events
            boolean afterInsert3_OK = (tipsChain.size() == 1) && tipsChain.contains(block_3.getHash()) &&
                    tipChainInfo.getHeight() == 3 && numBlocksSavedEvents.get() == 3

            // Now we remove a Block in the middle (block 1):
            db.removeBlock(block_1.getHash())
            tipsChain = db.getTipsChains()
            tipChainInfo = db.getBlockChainInfo(tipsChain.get(0)).get()
            Thread.sleep(50) // a bit of a delay, to trigger the events
            boolean afterRemoving1_OK = (tipsChain.size() == 1) && tipsChain.contains(block_genesis.getHash()) &&
                tipChainInfo.getHeight() == 0 && numBlocksRemovedEvents.get() == 1

            db.stop()
        then:
            beforeInserting
            afterInsert1_OK
            afterInsert2_OK
            afterInsert3_OK
            afterRemoving1_OK
        cleanup:
            MoreFiles.deleteRecursively(dbPath, RecursiveDeleteOption.ALLOW_INSECURE)
    }

    /**
     * We test that the state is published at the right times and the data it publishes is correct
     */
    def "testing State Publishing"() {
        given:
            // Configuration and DB start up:
            Path dbPath = Path.of(TestingUtils.buildWorkingFolder())
            BlockHeader block_genesis = TestingUtils.buildBlock()
            println(" - Genesis Block: " + block_genesis.getHash())
            BlockChainStoreLevelDBConfig dbConfig = BlockChainStoreLevelDBConfig.chainBuild()
                    .workingFolder(dbPath)
                    .genesisBlock(block_genesis)
                    .build()
            BlockChainStore db = BlockChainStoreLevelDB.chainBuilder()
                    .config(dbConfig)
                    .statePublishFrequency(Duration.ofMillis(100))
                    .build()

            // We store the LAST State published and the number of events triggered:
            AtomicReference<ChainStateEvent> lastState = new AtomicReference<>()
            AtomicInteger numStateEvents = new AtomicInteger()

            db.EVENTS().STATE.forEach({e ->
                lastState.set(e)
                numStateEvents.incrementAndGet()
            })

        when:
            db.start()

            // We wait a bit and we check the state, it should show just one Block (genesis)
            Thread.sleep(200)
            db.printKeys()
            int numEventsTriggered_1 = numStateEvents.get()
            boolean okAferGenesis = (numEventsTriggered_1 >= 1) &&
                    (lastState.get().state.numBlocks == 1) &&
                    (lastState.get().state.tipsChains.size() == 1) &&
                    (lastState.get().state.tipsChains.stream().map({ c-> c.header.getHash()}).anyMatch({h -> h.equals(block_genesis.getHash())}))



            // We insert a chain of 2 more blocks: [genesis] -> 1 -> 2
            BlockHeader block_1 = TestingUtils.buildBlock(block_genesis.getHash().toString())
            BlockHeader block_2 = TestingUtils.buildBlock(block_1.getHash().toString())
            db.saveBlocks(Arrays.asList(block_1, block_2))

            // We wait a bit more and we chekc the state published is updated:
            Thread.sleep(110)

            int numEventsTriggered_2 = numStateEvents.get()
            boolean okAfter2Blocks = (numEventsTriggered_2 > numEventsTriggered_1) &&
                    (lastState.get().state.numBlocks == 3) &&
                    (lastState.get().state.tipsChains.size() == 1) &&
                    (lastState.get().state.tipsChains.stream().map({ c-> c.header.getHash()}).anyMatch({h -> h.equals(block_2.getHash())}))

            db.stop()
        then:
            okAferGenesis
            okAfter2Blocks
        cleanup:
            MoreFiles.deleteRecursively(dbPath, RecursiveDeleteOption.ALLOW_INSECURE)
    }
}
