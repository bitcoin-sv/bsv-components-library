package com.nchain.jcl.tools.chainStore


import spock.lang.Specification

import java.time.Duration
import java.time.Instant

/**
 * Performance tests for ChainMemStore.
 */
class ChainMemStorePerformanceSpec extends Specification {

    /** Data Type stored in the Chain */
    class NodeTest implements Node<String> {
        String id;
        String title;
        byte[] data = new byte[100]; // simulate some payload
        NodeTest(String id, String title) {
            this.id = id;
            this.title = title;
        }
        @Override String getId() { return id;}
    }

    /**
     * - We crate a long main branch.
     * - Then we crate a FORK in the MIDDLE of the previous chain and add a FORK chain
     * - Then we prune/remove the FORK chain
     *
     * Each of the 3 operations above can be configured to determine the size/length of the chains.
     * We expect each operation to be performed before some threshod that can also be configured. If any of the tasks
     * take loner the test fails.
     */
    def "testing forking & prunning with long trunks"() {
        given:
            // Size of the TREE
            final long MAIN_CHAIN_LENGTH = 500_000;
            final long FORK_CHAIN_LENGTH = 1000;
            final long FORK_HEIGHT = 200_000;

            // Metrics/Thresholds we need to Meet to consider the Test a Success:
            final Duration TIME_TO_BUILD_MAIN_CHAIN = Duration.ofSeconds(180)
            final Duration TIME_TO_ADD_FORK = Duration.ofSeconds(30)
            final Duration TIME_TO_PRUNE_FORK = Duration.ofSeconds(10)

            // Tree initialization:
            NodeTest genesis = new NodeTest("0", "genesis")
            ChainMemStore<String, NodeTest> treeNode = new ChainMemStore<>(genesis)

        when:

            // We build a long chain, simulating the main chain, from genesis to the Tip:
            Instant timestamp1 = Instant.now()
            for (long i = 1; i <= MAIN_CHAIN_LENGTH; i++) {
                treeNode.addNode(String.valueOf(i - 1), new NodeTest(String.valueOf(i), "Node-" + i))
                if ((i % 1000) == 0) { println(i + " Nodes added to the MAIN Chain");}
            }
            Duration mainChainDuration = Duration.between(timestamp1, Instant.now())
            println("Main Chain added to the Tree in " + mainChainDuration.toSeconds() + " seconds");

            // Now we create an fork chain, connected to the MAIN chain at FORK_HEIGHT
            Instant timestamp2 = Instant.now()
            String parentId = FORK_HEIGHT
            for (long i = FORK_HEIGHT; i < FORK_HEIGHT + FORK_CHAIN_LENGTH; i++) {
                String nodeId = (i + 1) + "B";
                treeNode.addNode(parentId, new NodeTest(nodeId, "Node-" + nodeId))
                parentId = (i + 1) + "B";
                if ((i % 1000) == 0) { println(i + " Nodes added to the FORK Chain");}
            }
            Duration forkChainDuration = Duration.between(timestamp2, Instant.now())
            println("Fork Chain added to the Tree in " + forkChainDuration.toSeconds() + " seconds");

            // Now we prune the Fork (first node in the Alternative chain):
            Instant timestamp3 = Instant.now()
            treeNode.removeNode(String.valueOf(FORK_HEIGHT + 1) + "B")
            Duration pruneDuration = Duration.between(timestamp3, Instant.now())
            println("Fork Chain prunned in " + pruneDuration.toMillis() + " millisecs");

        then:
            // We check the times are acceptable:
            mainChainDuration.compareTo(TIME_TO_BUILD_MAIN_CHAIN) < 0
            forkChainDuration.compareTo(TIME_TO_ADD_FORK) < 0
            pruneDuration.compareTo(TIME_TO_PRUNE_FORK) < 0
    }
}
