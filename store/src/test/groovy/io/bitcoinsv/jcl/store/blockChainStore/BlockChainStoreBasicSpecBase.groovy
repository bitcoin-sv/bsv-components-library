/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.store.blockChainStore



import io.bitcoinsv.jcl.store.blockChainStore.events.ChainStateEvent
import io.bitcoinsv.jcl.store.common.TestingUtils
import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.HeaderReadOnly
import io.bitcoinsv.bitcoinjsv.bitcoin.api.extended.ChainInfo
import io.bitcoinsv.bitcoinjsv.core.Sha256Hash

import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.stream.Collectors

/**
 * Basic Tests to verify tha the Chain info is saved and is consistent after saving or removing Blocks.
 */
abstract class BlockChainStoreBasicSpecBase extends BlockChainStoreSpecBase {
    /**
     * We test that we can traverse a series of Blocks with a parent/child relationship back and forth
     */
    def "Testing Traversing Blocks"() {
        final int NUM_CHAIN_BLOCKS = 5
        given:
            println(" - Connecting to the DB...")
            HeaderReadOnly genesisBlock = TestingUtils.buildBlock()
            println(" - Using block genesis: " + genesisBlock.getHash())
            BlockChainStore db = getInstance("BSV-Main", false, false, genesisBlock, null, null, null, null, null)

        when:
            db.start()

            // We clean the DB:
            db.clear()
            // We check the DB Content in the console...
            println("Content of DB Right BEFORE the Test:")
            db.printKeys()

            // We insert a chain of Blocks, connected to each other
            List<HeaderReadOnly> blocks = new ArrayList<>();
            blocks.add(genesisBlock)
            String parentHash = genesisBlock.getHash().toString()
            String tipHash = null
            println(" - Generating " + NUM_CHAIN_BLOCKS + " Blocks with relation parent-child between them:")
            for (int i = 1; i < NUM_CHAIN_BLOCKS; i++) {
                HeaderReadOnly block = TestingUtils.buildBlock(parentHash)
                tipHash = block.getHash().toString()
                println("  - Block " + block.getHash() + " , parent: " + parentHash)
                blocks.add(block)
                parentHash = tipHash
            }
            println(" - Saving " + NUM_CHAIN_BLOCKS + " Blocks in Batch...")
            db.saveBlocks(blocks)

            // We take the Tip of the chain and we traverse the Chain back to the genesis block:
            // As we traverse the Chain we make sure that we loop over all the Blocks created previously:
            Set<String> hashesTraversedGenesis = new HashSet<>()
            println(" - Traversing the Chain from the Tip (" + tipHash + "), Back to the genesis block:")
            Optional<HeaderReadOnly> block = db.getBlock(Sha256Hash.wrap(tipHash))
            while (block.isPresent()) {
                hashesTraversedGenesis.add(block.get().getHash().toString())
                println(" - Block " + block.get().getHash())
                block = db.getBlock(block.get().getPrevBlockHash())
            }

            boolean traverseToGenesisOK = hashesTraversedGenesis.size() == NUM_CHAIN_BLOCKS

            // Now we take the genesis block and we traverse the chain up to the Tip of the Chain:
            Set<String> hashesTraversedTip = new HashSet<>()
            Sha256Hash startBlockHash = genesisBlock.getHash()
            println(" - Traversing the Chain from  Genesis (" + startBlockHash + "), to the Tip:")
            while (startBlockHash != null) {
                hashesTraversedTip.add(startBlockHash.toString())
                println(" - Block " + startBlockHash + " obtained.")
                List<Sha256Hash> children = db.getNextBlocks(startBlockHash)
                startBlockHash = (children != null && children.size() > 0) ? children.get(0) : null
            }

            boolean traverseToTipOK = hashesTraversedTip.size() == NUM_CHAIN_BLOCKS

            // We check the DB Content in the console...
            db.printKeys()

        then:
            traverseToGenesisOK
            traverseToTipOK
        cleanup:
            println(" - Cleanup...")
            db.removeBlocks(blocks.stream().map({b -> b.getHash()}).collect(Collectors.toList()))
            db.removeBlock(genesisBlock.getHash())
            db.removeTipsChains()
            db.printKeys()
            db.clear()
            db.stop()
            println(" - Test Done.")
    }

    /**
     * We tests that we save/remove blocks with a parent/child relationship, and the "ChainsTips" information is
     * updated properly, reflecting always the tip of the Chain.
     */
    def "testing saving/removing Blocks and Chain Tips updates"() {
        given:
            println(" - Connecting to the DB...")
            HeaderReadOnly genesisBlock = TestingUtils.buildBlock(Sha256Hash.ZERO_HASH.toString())
            println(" - Using block genesis: " + genesisBlock.getHash())
            BlockChainStore db = getInstance("BSV-Main", true, false, genesisBlock, null, null, null, null, null)

            // We keep track of the Events triggered:
            AtomicInteger numBlocksSavedEvents = new AtomicInteger()
            AtomicInteger numBlocksRemovedEvents = new AtomicInteger()

            db.EVENTS().BLOCKS_SAVED.forEach({e -> numBlocksSavedEvents.incrementAndGet()})
            db.EVENTS().BLOCKS_REMOVED.forEach({e -> numBlocksRemovedEvents.incrementAndGet()})

        when:
            db.start()

            // We clean the DB:
            db.clear()
            // We check the DB Content in the console...
            println("Content of DB Right BEFORE the Test:")
            db.printKeys()

            // Right after starting the DB from an empty state, the only Block in the DB should be the genesis block,
            // which is also part of the Tip...

            List<Sha256Hash> tipsChain = db.getTipsChains()
            ChainInfo tipChainInfo = db.getBlockChainInfo(tipsChain.get(0)).get()
            boolean beforeInserting = (tipsChain.size() == 1) && tipsChain.contains(genesisBlock.getHash()) &&
                    tipChainInfo.getHeight() == 0

            // We insert Blocks 1, 2 and 3 ( [genesis] -> 1 -> 2 -> 3)
            HeaderReadOnly block_1 = TestingUtils.buildBlock(genesisBlock.getHash().toString())
            println(" - Saving Block 1: " + block_1.getHash())
            db.saveBlock(block_1)
            tipsChain = db.getTipsChains()

            // We print the content of the Tip:
            println(" - Content of the Chain Tips:")
            tipsChain.forEach({t -> println(" - Block " + t.toString())})

            tipChainInfo = db.getBlockChainInfo(tipsChain.get(0)).get()
            Thread.sleep(50) // a bit of a delay, to trigger the events

            boolean afterInsert1_tipsLengthOK = tipsChain.size() == 1
            boolean afterInsert1_tipsContentOK = tipsChain.contains(block_1.getHash())
            boolean afterInsert1_tipChaininfoOK = tipChainInfo.getHeight() == 1
            int afterInsert1_numBlocksSaved = numBlocksSavedEvents.get()

            HeaderReadOnly block_2 = TestingUtils.buildBlock(block_1.getHash().toString())
            println(" - Saving Block 2: " + block_2.getHash())
            db.saveBlock(block_2)
            tipsChain = db.getTipsChains()

            // We print the content of the Tip:
            println(" - Content of the Chain Tips:")
            tipsChain.forEach({t -> println(" - Block " + t.toString())})

            tipChainInfo = db.getBlockChainInfo(tipsChain.get(0)).get()
            Thread.sleep(50) // a bit of a delay, to trigger the events

            boolean afterInsert2_tipsLengthOK = tipsChain.size() == 1
            boolean afterInsert2_tipsContentOK = tipsChain.contains(block_2.getHash())
            boolean afterInsert2_tipChaininfoOK = tipChainInfo.getHeight() == 2
            int afterInsert2_numBlocksSaved = numBlocksSavedEvents.get()


            HeaderReadOnly block_3 = TestingUtils.buildBlock(block_2.getHash().toString())
            println(" - Saving Block 3: " + block_3.getHash())
            db.saveBlock(block_3)
            tipsChain = db.getTipsChains()

            // We print the content of the Tip:
            println(" - Content of the Chain Tips:")
            tipsChain.forEach({t -> println(" - Block " + t.toString())})

            tipChainInfo = db.getBlockChainInfo(tipsChain.get(0)).get()
            Thread.sleep(50) // a bit of a delay, to trigger the events

            boolean afterInsert3_tipsLengthOK = tipsChain.size() == 1
            boolean afterInsert3_tipsContentOK = tipsChain.contains(block_3.getHash())
            boolean afterInsert3_tipChaininfoOK = tipChainInfo.getHeight() == 3
            int afterInsert3_numBlocksSaved = numBlocksSavedEvents.get()

            // Now we remove a Block in the middle (block 1):
            println("Removing Block 1: " + block_1.getHash() + " ...")
            db.removeBlock(block_1.getHash())
            tipsChain = db.getTipsChains()

            // We print the content of the Tip:
            println(" - Content of the Chain Tips:")
            tipsChain.forEach({t -> println(" - Block " + t.toString())})

            tipChainInfo = db.getBlockChainInfo(tipsChain.get(0)).get()
            Thread.sleep(50) // a bit of a delay, to trigger the events

            boolean afterRemoving1_tipsLengthOK = tipsChain.size() == 1
            boolean afterRemoving1_tipsContentOK = tipsChain.contains(genesisBlock.getHash())
            boolean afterRemoving1_tipChaininfoOK = tipChainInfo.getHeight() == 0
            int afterRemoving1_numBlocksSaved = numBlocksRemovedEvents.get()

        then:
            beforeInserting

            afterInsert1_tipsLengthOK
            afterInsert1_tipsContentOK
            afterInsert1_tipChaininfoOK
            afterInsert1_numBlocksSaved == 1

            afterInsert2_tipsLengthOK
            afterInsert2_tipsContentOK
            afterInsert2_tipChaininfoOK
            afterInsert2_numBlocksSaved == 2

            afterInsert3_tipsLengthOK
            afterInsert3_tipsContentOK
            afterInsert3_tipChaininfoOK
            afterInsert3_numBlocksSaved == 3

            afterRemoving1_tipsLengthOK
            afterRemoving1_tipsContentOK
            afterRemoving1_tipChaininfoOK
            afterRemoving1_numBlocksSaved == 1

        cleanup:
            println(" - Cleanup...")
            // We check the DB Content in the console...
            db.printKeys()
            println("Cleaning DB afer Test...")
            db.removeBlocks(Arrays.asList(genesisBlock.getHash(), block_1.getHash(), block_2.getHash(), block_3.getHash()))
            db.removeTipsChains()
            // We check the DB Content in the console...
            db.printKeys()
            db.clear()
            db.stop()
            println(" - Test Done.")
    }

    /**
     * We test that the state is published at the right times and the data it publishes is correct
     */
    def "testing State Publishing"() {
        given:
            // Configuration and DB start up:
            println(" - Connecting to the DB...")
            HeaderReadOnly genesisBlock = TestingUtils.buildBlock(Sha256Hash.ZERO_HASH.toString())
            println(" - Using block genesis: " + genesisBlock.getHash())
            BlockChainStore db = getInstance("BSV-Main", false, false, genesisBlock, Duration.ofMillis(100), null, null, null, null)

            // We store the LAST State published and the number of events triggered:
            AtomicReference<ChainStateEvent> lastState = new AtomicReference<>()
            AtomicInteger numStateEvents = new AtomicInteger()

            db.EVENTS().STATE.forEach({e ->
                println("State Event Triggered: num blocks: " + e.getState().numBlocks)
                lastState.set(e)
                numStateEvents.incrementAndGet()
            })

        when:
            db.start()

            // We clean the DB:
            db.clear()
            // We check the DB Content in the console...
            println("Content of DB Right BEFORE the Test:")
            db.printKeys()

            // We wait a bit and we check the state, it should show just one Block (genesis)
            Thread.sleep(200)
            println(" - Last State published: num Blocks: " + lastState.get().state.numBlocks)

            // We check the DB Content in the console...
            db.printKeys()
            int numEventsTriggered_1 = numStateEvents.get()
            boolean okAferGenesis = (numEventsTriggered_1 >= 1) &&
                    (lastState.get().state.numBlocks == 1) &&
                    (lastState.get().state.tipsChains.size() == 1) &&
                    (lastState.get().state.tipsChains.stream().map({ c-> c.header.getHash()}).anyMatch({h -> h.equals(genesisBlock.getHash())}))


            // We insert a chain of 2 more blocks: [genesis] -> 1 -> 2
            HeaderReadOnly block_1 = TestingUtils.buildBlock(genesisBlock.getHash().toString())
            HeaderReadOnly block_2 = TestingUtils.buildBlock(block_1.getHash().toString())

            println(" - Saving a batch of 2 Blocks:")
            println(" - Block " + block_1.getHash().toString())
            println(" - Block " + block_2.getHash().toString())
            db.saveBlocks(Arrays.asList(block_1, block_2))

            // We wait a bit more and we check the state published is updated:
            Thread.sleep(310)
            println(" - Last State published: num Blocks: " + lastState.get().state.numBlocks)

            // We check the DB Content in the console...
            db.printKeys()

            int numEventsTriggered_2 = numStateEvents.get()

        then:
            okAferGenesis
            // Events Check after Genesis block inserted:
            (numEventsTriggered_2 > numEventsTriggered_1)
            (lastState.get().state.numBlocks == 3)
            (lastState.get().state.tipsChains.stream().map({ c-> c.header.getHash()}).anyMatch({h -> h.equals(block_2.getHash())}))
        cleanup:
            println(" - Cleanup...")
            db.removeBlocks(Arrays.asList(genesisBlock.getHash(), block_1.getHash(), block_2.getHash()))
            db.removeTipsChains()
            // We check the DB Content in the console...
            db.printKeys()
            db.clear()
            db.stop()
            println(" - Test Done.")
    }

    /**
     * We tests that when inserting dulicated Blocks, the block is only sored once and it only appears once in the
     * Tips of the Chain
     */
    def "testing Duplicates Blocks"() {
        final int NUM_BLOCKS = 3
        given:
            // Configuration and DB start up:
            println(" - Connecting to the DB...")
            HeaderReadOnly genesisBlock = TestingUtils.buildBlock(Sha256Hash.ZERO_HASH.toString())
            println(" - Using block genesis: " + genesisBlock.getHash())
            BlockChainStore db = getInstance("BSV-Main", false, false, genesisBlock, Duration.ofMillis(100), null, null, null, null)

        when:
            db.start()

            // We clean the DB:
            db.clear()
            // We check the DB Content in the console...
            println("Content of DB Right BEFORE the Test:")
            db.printKeys()

            // We create a block, and we insert it several times...
            HeaderReadOnly block = TestingUtils.buildBlock(genesisBlock.getHash().toString())
            for (int i = 0; i < NUM_BLOCKS; i++) db.saveBlock(block);

            // Now we check
            long numBlocksAfterInserts = db.getNumBlocks()
            long numTipsChain = db.getTipsChains().size()

            // We check the DB Content in the console...
            db.printKeys()

        then:
                numBlocksAfterInserts == 2 // including the genesis Block...
                numTipsChain == 1
        cleanup:
            println(" - Cleanup...")
            db.removeBlock(block.getHash())
            db.removeTipsChains()
            // We check the DB Content in the console...
            db.printKeys()
            db.clear()
            db.stop()
            println(" - Test Done.")
    }


    def "testing Getting Blocks by Height"() {
        given:
            // Configuration and DB start up:
            println(" - Connecting to the DB...")
            HeaderReadOnly genesisBlock = TestingUtils.buildBlock(Sha256Hash.ZERO_HASH.toString())
            println(" - Using block genesis: " + genesisBlock.getHash())
            BlockChainStore db = getInstance("BSV-Main", false, false, genesisBlock, Duration.ofMillis(100), null, null, null, null)

        when:
            db.start()

            // We clean the DB:
            db.clear()
            // We check the DB Content in the console...
            println("Content of DB Right BEFORE the Test:")
            db.printKeys()

            // We store a chain of 3 Blocks, all connected:
            HeaderReadOnly block1 = TestingUtils.buildBlock(genesisBlock.hash.toString())
            HeaderReadOnly block2 = TestingUtils.buildBlock(block1.hash.toString())
            HeaderReadOnly block3 = TestingUtils.buildBlock(block2.hash.toString())

            db.saveBlocks(Arrays.asList(block1, block2, block3))

            // We check the DB Content in the console...
            db.printKeys()

            // We get some Blocks by Height and by Hash...
            Optional<ChainInfo> block1ReadByHeight = db.getBlock(1)
            Optional<ChainInfo> block2ReadByHeight = db.getBlock(2)

            Optional<HeaderReadOnly> block1ReadByHash = db.getBlock(block1.hash)
            Optional<HeaderReadOnly> block2ReadByHash = db.getBlock(block2.hash)

            // Now we remove the Block1, so the Block 1 is no longer in the Db and the Block 2 is still in the chain but
            // is DISCONNECTED, so not posible to retrieve by Height...
            db.removeBlock(block1.hash)

            Optional<ChainInfo> block1ReadByHeightAferRemoval = db.getBlock(1)
            Optional<ChainInfo> block2ReadByHeightAferRemoval = db.getBlock(2)

        then:
            block1ReadByHeight.get().header.hash.toString().equals(block1ReadByHash.get().hash.toString())
            block2ReadByHeight.get().header.hash.toString().equals(block2ReadByHash.get().hash.toString())
            block1ReadByHeightAferRemoval.isEmpty()
            block2ReadByHeightAferRemoval.isEmpty()
        cleanup:
            println(" - Cleanup...")
            db.printKeys()
            db.clear()
            db.stop()
            println(" - Test Done.")
    }
}