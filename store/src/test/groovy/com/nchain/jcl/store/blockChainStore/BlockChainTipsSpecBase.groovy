package com.nchain.jcl.store.blockChainStore

import com.nchain.jcl.base.domain.api.base.BlockHeader
import com.nchain.jcl.base.domain.api.extended.ChainInfo
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper
import com.nchain.jcl.store.common.TestingUtils

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
            BlockHeader genesisBlock = TestingUtils.buildBlock(Sha256Wrapper.ZERO_HASH.toString())
            println(" - Using block genesis: " + genesisBlock.getHash())
            BlockChainStore db = getInstance("BSV-Main", false, false, genesisBlock, Duration.ofMillis(100), null, null, null, null)

        when:
            db.start()

            // We create a tree like this:
            // - [genesis] - [A] - [B] - [D]
            //                  \- [C] - [E]
            //                        \- [F] - [G]

            // This should create 3 Tips: D, E and G.

            BlockHeader blockA = TestingUtils.buildBlock(genesisBlock.hash.toString())
            BlockHeader blockB = TestingUtils.buildBlock(blockA.hash.toString())
            BlockHeader blockC = TestingUtils.buildBlock(blockA.hash.toString())
            BlockHeader blockD = TestingUtils.buildBlock(blockB.hash.toString())
            BlockHeader blockE = TestingUtils.buildBlock(blockC.hash.toString())
            BlockHeader blockF = TestingUtils.buildBlock(blockC.hash.toString())
            BlockHeader blockG = TestingUtils.buildBlock(blockF.hash.toString())

            List<BlockHeader> blocksToSave = Arrays.asList(blockA, blockB, blockC, blockD, blockE, blockF, blockG)
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

            List<Sha256Wrapper> tipsChain = db.getTipsChains()

            // Now we try "getTipsChainChain()" with different Blocks:
            List<Sha256Wrapper> tipsForBlockA = db.getTipsChains(blockA.hash) // D, E, G
            List<Sha256Wrapper> tipsForBlockB = db.getTipsChains(blockB.hash) // D
            List<Sha256Wrapper> tipsForBlockC = db.getTipsChains(blockC.hash) // E, G

            // We check the FIRST Block of the PATH of each Tip:
            ChainInfo firstBlockOfBlockD = db.getFirstBlockInPath(blockD.hash) // B
            ChainInfo firstBlockOfBlockE = db.getFirstBlockInPath(blockE.hash) // E
            ChainInfo firstBlockOfBlockG = db.getFirstBlockInPath(blockG.hash) // F

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

                firstBlockOfBlockD.header.hash.equals(blockB.hash)
                firstBlockOfBlockE.header.hash.equals(blockE.hash)
                firstBlockOfBlockG.header.hash.equals(blockF.hash)

        cleanup:
            println(" - Cleanup...")
            db.removeBlocks(blocksToSave.stream().map({b -> b.hash}).collect(Collectors.toList()))
            db.removeBlock(genesisBlock.hash)
            db.removeTipsChains()
            // We check the DB Content in the console...
            db.printKeys()
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
            BlockHeader genesisBlock = TestingUtils.buildBlock(Sha256Wrapper.ZERO_HASH.toString())
            println(" - Using block genesis: " + genesisBlock.getHash())
            BlockChainStore db = getInstance("BSV-Main", false, false, genesisBlock, Duration.ofMillis(100), null, null, null, null)

        when:
            db.start()

            // We create first a tree like this:
            // - [genesis] - [A] - [B] - [C]
            // These bocks will be automatically connected to the Chain:
            BlockHeader blockA = TestingUtils.buildBlock(genesisBlock.hash.toString())
            BlockHeader blockB = TestingUtils.buildBlock(blockA.hash.toString())
            BlockHeader blockC = TestingUtils.buildBlock(blockB.hash.toString())
            db.saveBlocks(Arrays.asList(blockA, blockB, blockC))

            // we check the Tips:
            List<Sha256Wrapper> tipsChainAfterFirstBranch = db.getTipsChains()
            List<Sha256Wrapper> tipsChainBlockBAfterFirstBranch = db.getTipsChains(blockB.getHash())

            // We check the FIRST Block of the PATH of each Tip:
            ChainInfo firstBlockOfBlockC = db.getFirstBlockInPath(blockC.hash) // genesis

            // Now we create a another Branch of blocks:
            //   [B]
            //     \- [D] - [E] - [F] - [G]
            //                 \- [H] - [I]
            // But the Block [D] will NOT be saved yet, so the branch starting from [E] wil be saved but will be
            // DISCONNECTED from the Chain

            BlockHeader blockD = TestingUtils.buildBlock(blockB.hash.toString())
            BlockHeader blockE = TestingUtils.buildBlock(blockD.hash.toString())
            BlockHeader blockF = TestingUtils.buildBlock(blockE.hash.toString())
            BlockHeader blockG = TestingUtils.buildBlock(blockF.hash.toString())
            BlockHeader blockH = TestingUtils.buildBlock(blockE.hash.toString())
            BlockHeader blockI = TestingUtils.buildBlock(blockH.hash.toString())

            List<BlockHeader> blocksToSave = Arrays.asList(blockE, blockF, blockG, blockH, blockI)
            db.saveBlocks(blocksToSave)

            // we check the Tips:
            List<Sha256Wrapper> tipsChainAfterSecondBranch = db.getTipsChains()
            List<Sha256Wrapper> tipsChainBlockBAfterSecondBranch = db.getTipsChains(blockB.getHash())

            // Now we save the Block [D] ,which will trigger the connection of all the Nodes in the last branch
            db.saveBlock(blockD)

            // we check the Tips:
            List<Sha256Wrapper> tipsChainAfterConnectingBranch = db.getTipsChains()
            List<Sha256Wrapper> tipsChainBlockBAfterConnectingBranch = db.getTipsChains(blockB.getHash())

            // We check the FIRST Block of the PATH of each Tip:
            ChainInfo firstBlockOfBlockG = db.getFirstBlockInPath(blockG.hash) // genesis
            ChainInfo firstBlockOfBlockI = db.getFirstBlockInPath(blockI.hash) // genesis


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

            firstBlockOfBlockC.header.hash.equals(genesisBlock.hash)

            tipsChainAfterSecondBranch.equals(tipsChainAfterFirstBranch)
            tipsChainBlockBAfterSecondBranch.equals(tipsChainBlockBAfterFirstBranch)

            tipsChainAfterConnectingBranch.size() == 3
            tipsChainAfterConnectingBranch.contains(blockC.hash)
            tipsChainAfterConnectingBranch.contains(blockG.hash)
            tipsChainAfterConnectingBranch.contains(blockI.hash)
            tipsChainBlockBAfterConnectingBranch.equals(tipsChainAfterConnectingBranch)

            firstBlockOfBlockG.header.hash.equals(blockF.hash)
            firstBlockOfBlockI.header.hash.equals(blockH.hash)


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
            db.stop()
            println(" - Test Done.")
    }
}
