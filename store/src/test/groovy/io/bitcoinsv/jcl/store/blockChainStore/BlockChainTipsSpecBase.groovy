/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.store.blockChainStore


import io.bitcoinsv.jcl.store.common.TestingUtils
import io.bitcoinj.bitcoin.api.base.HeaderReadOnly
import io.bitcoinj.bitcoin.api.extended.ChainInfo
import io.bitcoinj.core.Sha256Hash

import java.time.Duration
import java.util.stream.Collectors

/**
 * Testing class for scenarios that focus on the manipulation of the TIPS of the Chain
 */
abstract class BlockChainTipsSpecBase extends BlockChainStoreSpecBase {

    /**
     * We test that the getLongestChain method works as expected, when a block Hash is given as a
     * parameter.
     * In this scenario, we create a TREE hierarchy of Blocks, and this structure is created in a linear fashion: every
     * time a new Block is inserted, is immediately connected to a Parent which has been already connected previously
     *
     * So in this scenario all the Blocks are connected right after being inserted...
     */
    def "testing getLongestChain by Block when creating a Tree structure in a linear-fashion"() {
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

            // We create a tree like this:
            // - [genesis] - [A] - [B] - [D]
            //                  \- [C] - [E]
            //                        \- [F] - [G]

            // This should create 3 Tips: D, E and G.

            HeaderReadOnly blockA = TestingUtils.buildBlock(genesisBlock.hash.toString())
            HeaderReadOnly blockB = TestingUtils.buildBlock(blockA.hash.toString())
            HeaderReadOnly blockC = TestingUtils.buildBlock(blockA.hash.toString())
            HeaderReadOnly blockD = TestingUtils.buildBlock(blockB.hash.toString())
            HeaderReadOnly blockE = TestingUtils.buildBlock(blockC.hash.toString())
            HeaderReadOnly blockF = TestingUtils.buildBlock(blockC.hash.toString())
            HeaderReadOnly blockG = TestingUtils.buildBlock(blockF.hash.toString())

            List<HeaderReadOnly> blocksToSave = Arrays.asList(blockA, blockB, blockC, blockD, blockE, blockF, blockG)
            db.saveBlocks(blocksToSave)

            println(" - Genesis: " + genesisBlock.hash.toString())
            println("     \\- Block A: " + blockA.hash.toString())
            println("           \\- Block B: " + blockB.hash.toString())
            println("                \\- Block D: " + blockD.hash.toString())
            println("           \\- Block C: " + blockC.hash.toString())
            println("                \\- Block E: " + blockE.hash.toString())
            println("                \\- Block F: " + blockF.hash.toString())
            println("                      \\- Block G: " + blockG.hash.toString())

            // We check the DB Content in the console...
            db.printKeys()

            // First, we check the Tips:

            List<Sha256Hash> tipsChain = db.getTipsChains()

            // Now we Sha256Hash "getTipsChainChain()" with different Blocks:
            List<Sha256Hash> tipsForBlockA = db.getTipsChains(blockA.hash) // D, E, G
            List<Sha256Hash> tipsForBlockB = db.getTipsChains(blockB.hash) // D
            List<Sha256Hash> tipsForBlockC = db.getTipsChains(blockC.hash) // E, G

            // We check the FIRST Block of the PATH of each Tip:
            Optional<ChainInfo> firstBlockOfBlockD = db.getFirstBlockInHistory(blockD.hash) // B
            Optional<ChainInfo> firstBlockOfBlockE = db.getFirstBlockInHistory(blockE.hash) // E
            Optional<ChainInfo> firstBlockOfBlockG = db.getFirstBlockInHistory(blockG.hash) // F

            // Now, we prune the branch with tip (G) and we check again the Tips:
            db.prune(blockG.hash, false)

            Optional<ChainInfo> firstBlockOfBlockDAfterPrunning = db.getFirstBlockInHistory(blockD.hash) // B
            Optional<ChainInfo> firstBlockOfBlockEAfterPrunning = db.getFirstBlockInHistory(blockE.hash) // E
            Optional<ChainInfo> firstBlockOfBlockGAfterPrunning = db.getFirstBlockInHistory(blockG.hash) // F

        then:
                tipsChain.size() == 3
                tipsChain.contains(blockD.hash)
                tipsChain.contains(blockE.hash)
                tipsChain.contains(blockG.hash)

                tipsForBlockA.size() == 3
                tipsForBlockA.contains(blockD.hash)
                tipsForBlockA.contains(blockE.hash)
                tipsForBlockA.contains(blockG.hash)

                tipsForBlockB.size() == 1
                tipsForBlockB.contains(blockD.hash)

                tipsForBlockC.size() == 2
                tipsForBlockC.contains(blockE.hash)
                tipsForBlockC.contains(blockG.hash)

                firstBlockOfBlockD.get().header.hash.equals(blockB.hash)
                firstBlockOfBlockE.get().header.hash.equals(blockE.hash)
                firstBlockOfBlockG.get().header.hash.equals(blockF.hash)

                firstBlockOfBlockDAfterPrunning.get().header.hash.equals(blockB.hash)
                firstBlockOfBlockEAfterPrunning.get().header.hash.equals(blockC.hash)
                firstBlockOfBlockGAfterPrunning.isEmpty()

        cleanup:
            println(" - Cleanup...")
            db.removeBlocks(blocksToSave.stream().map({b -> b.hash}).collect(Collectors.toList()))
            db.removeBlock(genesisBlock.hash)
            db.removeTipsChains()
            // We check the DB Content in the console...
            db.printKeys()
            db.clear()
            db.stop()
            println(" - Test Done.")
    }


    /**
     * We test that the getLongestChain method works as expected, when a block Hash is given as a
     * parameter.
     * In this scenario, we cra a TREE hierarchy of Blocks, and this structure is created in a NON-linear fashion:
     *
     * We create 2 branches of Blocks: One is connected to the chain, the other is not. And ten we connect the root
     * of the Disconnected branch, so that will trigger the conneciton of all the nodes in that branch...
     */
    def "testing getLongestChain by Block when creating a Tree structure in a no-linear-fashion"() {
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

            // We create first a tree like this:
            // - [genesis] - [A] - [B] - [C]
            // These bocks will be automatically connected to the Chain:
            HeaderReadOnly blockA = TestingUtils.buildBlock(genesisBlock.hash.toString())
            HeaderReadOnly blockB = TestingUtils.buildBlock(blockA.hash.toString())
            HeaderReadOnly blockC = TestingUtils.buildBlock(blockB.hash.toString())
            db.saveBlocks(Arrays.asList(blockA, blockB, blockC))

            // we check the Tips:
            List<Sha256Hash> tipsChainAfterFirstBranch = db.getTipsChains()
            List<Sha256Hash> tipsChainBlockBAfterFirstBranch = db.getTipsChains(blockB.getHash())

            // We check the FIRST Block of the PATH of each Tip:
            Optional<ChainInfo> firstBlockOfBlockC = db.getFirstBlockInHistory(blockC.hash) // genesis

            // Now we create a another Branch of blocks:
            //   [B]
            //     \- [D] - [E] - [F] - [G]
            //                 \- [H] - [I]
            // But the Block [D] will NOT be saved yet, so the branch starting from [E] wil be saved but will be
            // DISCONNECTED from the Chain

            HeaderReadOnly blockD = TestingUtils.buildBlock(blockB.hash.toString())
            HeaderReadOnly blockE = TestingUtils.buildBlock(blockD.hash.toString())
            HeaderReadOnly blockF = TestingUtils.buildBlock(blockE.hash.toString())
            HeaderReadOnly blockG = TestingUtils.buildBlock(blockF.hash.toString())
            HeaderReadOnly blockH = TestingUtils.buildBlock(blockE.hash.toString())
            HeaderReadOnly blockI = TestingUtils.buildBlock(blockH.hash.toString())

            List<HeaderReadOnly> blocksToSave = Arrays.asList(blockE, blockF, blockG, blockH, blockI)
            db.saveBlocks(blocksToSave)

            // we check the Tips:
            List<Sha256Hash> tipsChainAfterSecondBranch = db.getTipsChains()
            List<Sha256Hash> tipsChainBlockBAfterSecondBranch = db.getTipsChains(blockB.getHash())

            // Now we save the Block [D] ,which will trigger the connection of all the Nodes in the last branch
            db.saveBlock(blockD)

            // we check the Tips:
            List<Sha256Hash> tipsChainAfterConnectingBranch = db.getTipsChains()
            List<Sha256Hash> tipsChainBlockBAfterConnectingBranch = db.getTipsChains(blockB.getHash())

            // We check the FIRST Block of the PATH of each Tip:
            Optional<ChainInfo> firstBlockOfBlockG = db.getFirstBlockInHistory(blockG.hash) // genesis
            Optional<ChainInfo> firstBlockOfBlockI = db.getFirstBlockInHistory(blockI.hash) // genesis


            println(" - Genesis: " + genesisBlock.hash.toString())
            println("     |- Block A: " + blockA.hash.toString())
            println("           |- Block B: " + blockB.hash.toString())
            println("                |- Block C: " + blockC.hash.toString())
            println("                |- Block D: " + blockD.hash.toString())
            println("                     |- Block E: " + blockE.hash.toString())
            println("                          |- Block F: " + blockF.hash.toString())
            println("                                |- Block G: " + blockG.hash.toString())
            println("                          |- Block H: " + blockH.hash.toString())
            println("                                |- Block I: " + blockI.hash.toString())

            // We check the DB Content in the console...
            db.printKeys()

        then:
            tipsChainAfterFirstBranch.size() == 1
            tipsChainAfterFirstBranch.contains(blockC.hash)
            tipsChainBlockBAfterFirstBranch.contains(blockC.hash)

            firstBlockOfBlockC.get().header.hash.equals(genesisBlock.hash)

            tipsChainAfterSecondBranch.equals(tipsChainAfterFirstBranch)
            tipsChainBlockBAfterSecondBranch.equals(tipsChainBlockBAfterFirstBranch)

            tipsChainAfterConnectingBranch.size() == 3
            tipsChainAfterConnectingBranch.contains(blockC.hash)
            tipsChainAfterConnectingBranch.contains(blockG.hash)
            tipsChainAfterConnectingBranch.contains(blockI.hash)
            tipsChainBlockBAfterConnectingBranch.equals(tipsChainAfterConnectingBranch)

            firstBlockOfBlockG.get().header.hash.equals(blockF.hash)
            firstBlockOfBlockI.get().header.hash.equals(blockH.hash)


        cleanup:
            println(" - Cleanup...")
            // We first remove the separate branch...
            db.removeBlocks(Arrays.asList(blockD, blockE, blockF, blockG, blockH, blockI)
                .stream().map({b -> b.hash}).collect(Collectors.toList()))
            // Now we remove the initial branch...
            db.removeBlocks(Arrays.asList(blockA, blockB, blockC)
                .stream().map({b -> b.hash}).collect(Collectors.toList()))

            // and the genesis block:
            db.removeBlock(genesisBlock.hash)
            db.removeTipsChains()
            // We check the DB Content in the console...
            db.printKeys()
            db.clear()
            db.stop()
            println(" - Test Done.")
    }
}
