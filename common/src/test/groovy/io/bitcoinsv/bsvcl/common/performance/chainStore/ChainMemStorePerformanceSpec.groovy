package io.bitcoinsv.bsvcl.common.performance.chainStore


import io.bitcoinjsv.bsvcl.common.unit.chainStore.NodeTest
import io.bitcoinsv.bsvcl.common.chainStore.ChainMemStore
import io.bitcoinsv.bsvcl.common.chainStore.ChainPath
import spock.lang.Ignore
import spock.lang.Specification

import java.time.Duration
import java.time.Instant

/**
 * Performance tests for ChainMemStore.
 */
@Ignore("Performance Test, dont work well in Github CI, no control over environment")
class ChainMemStorePerformanceSpec extends Specification {

    /**
     * - We create a long main branch.
     * - Then we crate a FORK in the MIDDLE of the previous chain and add a FORK chain
     * - Then we prune/remove the FORK chain
     *
     * Each of the 3 operations above can be configured to determine the size/length of the chains.
     * We expect each operation to be performed before some threshold that can also be configured. If any of the tasks
     * take longer the test fails.
     */
    def "testing forking & pruning with long trunks"() {
        given:
            // Size of the TREE
            final long MAIN_CHAIN_LENGTH = 500_000
            final long FORK_CHAIN_LENGTH = 1000
            final long FORK_HEIGHT = 200_000

            // Metrics/Thresholds we need to Meet to consider the Test a Success:
            final Duration TIME_TO_BUILD_MAIN_CHAIN = Duration.ofMillis(3000)
            final Duration TIME_TO_CREATE_FORK = Duration.ofMillis(7000)
            final Duration TIME_TO_BUILD_FORK_CHAIN = Duration.ofMillis(100)
            final Duration TIME_TO_PRUNE_FORK = Duration.ofMillis(100)
            final Duration TIME_TO_GET_PATHS = Duration.ofMillis(10)
            final Duration TIME_TO_GET_LASTNODE = Duration.ofMillis(5)

            // Tree initialization:
            NodeTest genesis = new NodeTest("0", "genesis")
            ChainMemStore<String, NodeTest> treeNode = new ChainMemStore<>(genesis)

        when:

            // Build a long chain, simulating the main chain, from genesis to the Tip:
            println("Adding Nodes to Main Chain...")
            Instant timestamp1 = Instant.now()
            for (long i = 1; i <= MAIN_CHAIN_LENGTH; i++) {
                treeNode.addNode(String.valueOf(i - 1), new NodeTest(String.valueOf(i), "Node-" + i))
            }
            Duration mainChainDuration = Duration.between(timestamp1, Instant.now())
            println(MAIN_CHAIN_LENGTH + " Nodes added to Main Chain added to the Tree in " + mainChainDuration.toMillis() + " milliseconds")

            // Create an fork chain, connected to the MAIN chain at FORK_HEIGHT
            println("Creating a Fork at Height " + FORK_HEIGHT + ", relocating " + (MAIN_CHAIN_LENGTH - FORK_HEIGHT) + " nodes...")
            Instant timestamp2 = Instant.now()
            String forkNodeId = (FORK_HEIGHT + 1) + "B"
            boolean forkCreated = treeNode.addNode(String.valueOf(FORK_HEIGHT), new NodeTest(forkNodeId, "Node-" + forkNodeId))
            Duration forkChainDuration = Duration.between(timestamp2, Instant.now())
            println("Fork created in " + forkChainDuration.toMillis() + " milliseconds")

            // Add more Nodes to the FORK CHAIN
            println("Adding Nodes to Fork Chain...")
            Instant timestamp3 = Instant.now()
            String parentId = FORK_HEIGHT + 1 + "B"
            for (long i = FORK_HEIGHT + 1; i < FORK_HEIGHT + FORK_CHAIN_LENGTH; i++) {
                String nodeId = (i + 1) + "B"
                treeNode.addNode(parentId, new NodeTest(nodeId, "Node-" + nodeId))
                parentId = (i + 1) + "B"
            }
            Duration forkNodesDuration = Duration.between(timestamp3, Instant.now())
            println(FORK_CHAIN_LENGTH + " Nodes added to Fork Chain in " + forkNodesDuration.toMillis() + " milliseconds")

            // Extract some Paths of Nodes in the middle part of the main chain and the fork:
            println("Calculating Paths of " + (FORK_CHAIN_LENGTH * 2) + " Nodes at different heights...")
            int avgGetPathMillisecs = 0
            for (int i = 0; i < FORK_CHAIN_LENGTH; i++) {
                Instant begin = Instant.now()
                ChainPath<NodeTest> pathNodeMainChain = treeNode.getPath(i, String.valueOf(FORK_HEIGHT + i), true)
                ChainPath<NodeTest> pathNodeFork = treeNode.getPath(i, String.valueOf(FORK_HEIGHT + i) + "B", true)
                avgGetPathMillisecs += Duration.between(begin, Instant.now()).toMillis()
            }

            avgGetPathMillisecs = (int) (avgGetPathMillisecs / (FORK_CHAIN_LENGTH * 2))
            println("Paths of " + (FORK_CHAIN_LENGTH * 2) + " nodes, each one calculated in : " + avgGetPathMillisecs + " milliseconds (avg)")

            // Calculate "getLastNode()" 1000 times and calculate avg time:
            final int NUM_LASTNODE_TIMES = 1000
            println("Calculating 'getLastNode()' " + NUM_LASTNODE_TIMES + " times...")
            int avgGetLastNodeMillisecs = 0
            for (int i = 0; i < 1000; i++) {
                Instant begin = Instant.now()
                NodeTest lastNode = treeNode.getLastNode()
                avgGetLastNodeMillisecs += Duration.between(begin, Instant.now()).toMillis()
            }
            avgGetLastNodeMillisecs = (int) (avgGetLastNodeMillisecs / (NUM_LASTNODE_TIMES))
            println("last node calculated for  " + NUM_LASTNODE_TIMES + ", each one calculated in: " + avgGetLastNodeMillisecs + " ms")

            // Prune the Fork (first node in the Alternative chain):
            println("Pruning Chain at height " + (FORK_HEIGHT + 1) + "...")
            Instant timestamp4 = Instant.now()
            treeNode.prune((int) (FORK_HEIGHT + 1))
            Duration pruneDuration = Duration.between(timestamp4, Instant.now())
            println("Fork Chain pruned in " + pruneDuration.toMillis() + " ms")

        then:
            // Check the times are acceptable:
            mainChainDuration.compareTo(TIME_TO_BUILD_MAIN_CHAIN) < 0
            forkChainDuration.compareTo(TIME_TO_CREATE_FORK) < 0
            forkNodesDuration.compareTo(TIME_TO_BUILD_FORK_CHAIN) < 0
            pruneDuration.compareTo(TIME_TO_PRUNE_FORK) < 0
            TIME_TO_GET_PATHS.toMillis() > avgGetPathMillisecs
            TIME_TO_GET_LASTNODE.toMillis() > avgGetLastNodeMillisecs
    }
}
