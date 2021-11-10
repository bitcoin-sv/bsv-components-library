package com.nchain.jcl.tools.chainStore


import spock.lang.Specification
import static com.nchain.jcl.tools.chainStore.NodeTestFactory.*


/**
 * Testing class for ChainMemStore. Basic operations (adding/removing nodes, creating/removing forks...)
 */
class ChainMemStoreAddRemoveSpec extends Specification {

    /**
     * We build [genesis]-[1]-[2]-[3].
     * [4] is NOT saved (parent not found)
     */
    def "adding Nodes to Trunk"() {
        given:
            ChainMemStore<String, NodeTest> treeNode = new ChainMemStore<>(genesis())
        when:
            boolean node1Saved = treeNode.addNode(genesis().getId(), node("1"))
            boolean node2Saved = treeNode.addNode(node("1").getId(), node("2"))
            boolean node3Saved = treeNode.addNode(node("2").getId(), node("3"))
            boolean node4Saved = treeNode.addNode(node("4").getId(), node("4"))         // NOT SAVED
            boolean node3SavedAgain = treeNode.addNode(node("2").getId(), node("3"))   // NOT SAVED

            int oneHeight = treeNode.getHeight("1").getAsInt()
            int twoHeight = treeNode.getHeight("2").getAsInt()
            int threeHeight = treeNode.getHeight("3").getAsInt()

            List<String> tips = treeNode.getTips()
            NodeTest bestNode = treeNode.getLastNode()

            // We extract a couple of Chain of Nodes:
            // Only Up to the Node:
            ChainPath<NodeTest> wholePath = treeNode.getPath(0, "3", false)
            ChainPath<NodeTest> partialPath = treeNode.getPath(1, "2", false)
            ChainPath<NodeTest> notFoundPath = treeNode.getPath(2, "6", false)

            // Now, including children after the Node and up to next fork:
            ChainPath<NodeTest> wholePath2 = treeNode.getPath(0, "3", true)
            ChainPath<NodeTest> partialPath2 = treeNode.getPath(1, "2", true)
            ChainPath<NodeTest> notFoundPath2 = treeNode.getPath(2, "6", true)

        then:
            treeNode.size() == 4
            node1Saved
            node2Saved
            node3Saved
            !node3SavedAgain
            !node4Saved
            oneHeight == 1
            twoHeight == 2
            threeHeight == 3
            tips.size() == 1
            tips.contains(node("3").getId())
            treeNode.getMaxLength() == 4
            bestNode.getId().equals("3")

            wholePath.getNodes().size() == 4
            partialPath.getNodes().size() == 2
            notFoundPath == null

            wholePath2.getNodes().size() == 4
            partialPath2.getNodes().size() == 3
            notFoundPath2 == null
    }

    /**
     * We build [genesis]-[1]-[2]
     *                         |-[3A]-[4]-[5]
     *                         |-[3B]
     */
    def "adding Branches"() {
        given:
            ChainMemStore<String, NodeTest> treeNode = new ChainMemStore<>(genesis())
        when:
            treeNode.addNode(genesis().getId(), node("1"))
            treeNode.addNode(node("1").getId(), node("2"))
            treeNode.addNode(node("2").getId(), node("3A"))
            treeNode.addNode(node("2").getId(), node("3B"))
            treeNode.addNode(node("3A").getId(), node("4"))
            treeNode.addNode(node("4").getId(), node("5"))

            int oneHeight = treeNode.getHeight("1").getAsInt()
            int twoHeight = treeNode.getHeight("2").getAsInt()
            int threeAHeight = treeNode.getHeight("3A").getAsInt()
            int threeBHeight = treeNode.getHeight("3B").getAsInt()
            int fourAHeight = treeNode.getHeight("4").getAsInt()
            int fiveBHeight = treeNode.getHeight("5").getAsInt()

            List<String> nodesAtHeight3 = treeNode.getNodesAtHeight(3);
            List<String> tips = treeNode.getTips()
            NodeTest bestNode = treeNode.getLastNode()

            // We get a couple of Paths:
            // Only up to the Node:
            ChainPath<NodeTest> path1 = treeNode.getPath(0, "5", false)
            ChainPath<NodeTest> path2 = treeNode.getPath(0, "1", false)
            ChainPath<NodeTest> path3 = treeNode.getPath(1, "3B", false)

            // including children AFTER the node and up to next fork
            ChainPath<NodeTest> path1B = treeNode.getPath(0, "5", true)
            ChainPath<NodeTest> path2B = treeNode.getPath(0, "1", true)
            ChainPath<NodeTest> path3B = treeNode.getPath(1, "3B", true)


        then:
            treeNode.size() == 7
            oneHeight == 1
            twoHeight == 2
            threeAHeight == 3
            threeBHeight == 3
            fourAHeight == 4
            fiveBHeight == 5
            nodesAtHeight3.size() == 2
            nodesAtHeight3.contains(node("3A").getId())
            nodesAtHeight3.contains(node("3B").getId())
            tips.size() == 2
            tips.contains(node("5").getId())
            tips.contains(node("3B").getId())
            treeNode.getMaxLength() == 6
            bestNode.getId().equals("5")

            path1.getNodes().size() == 6
            path2.getNodes().size() == 2
            path3.getNodes().size() == 3

            path1B.getNodes().size() == 6
            path2B.getNodes().size() == 6
            path3B.getNodes().size() == 3

    }

    /**
     * We build this branch: We build [genesis]-[1]-[2]-[3].
     * Then we Remove [2], so we should get: [genesis]-[1].
     */
    def "adding and removing Blocks from Trunk"() {
        given:
            ChainMemStore<String, NodeTest> treeNode = new ChainMemStore<>(genesis())
        when:
            treeNode.addNode(genesis().getId(), node("1"))
            treeNode.addNode(node("1").getId(), node("2"))
            treeNode.addNode(node("2").getId(), node("3"))

            List<String> tipsBeforeRemoving = treeNode.getTips()
            long maxLengthBeforeRemoving = treeNode.getMaxLength()
            NodeTest bestNodeBeforeRemoving = treeNode.getLastNode()

            boolean twoRemoved = treeNode.removeNode("2")
            boolean unknownRemoved = treeNode.removeNode("xx");

            List<String> tipsAfterRemoving = treeNode.getTips()
            long maxLengthAfterRemoving = treeNode.getMaxLength()
            NodeTest bestNodeAfterRemoving = treeNode.getLastNode()

        then:
            treeNode.size() == 2
            twoRemoved
            !unknownRemoved
            tipsBeforeRemoving.size() == 1
            tipsBeforeRemoving.contains(node("3").getId())
            tipsAfterRemoving.size() == 1
            tipsAfterRemoving.contains(node("1").getId())
            maxLengthBeforeRemoving == 4
            maxLengthAfterRemoving == 2
            bestNodeBeforeRemoving.getId().equals("3")
            bestNodeAfterRemoving.getId().equals("1")
    }

    /**
     * We build [genesis]-[1]-[2]
     *                         |-[3A] -[4]-[5]
     *                         |-[3B]
     *
     * Then we remove [3A], so we should get:
     * [genesis]-[1]-[2]-[3B]
     *
     */
    def "adding and removing Blocks from Branches"() {
        given:
            ChainMemStore<String, NodeTest> treeNode = new ChainMemStore<>(genesis())
        when:
            treeNode.addNode(genesis().getId(), node("1"))
            treeNode.addNode(node("1").getId(), node("2"))
            treeNode.addNode(node("2").getId(), node("3A"))
            treeNode.addNode(node("2").getId(), node("3B"))
            treeNode.addNode(node("3A").getId(), node("4"))
            treeNode.addNode(node("4").getId(), node("5"))

            List<String> nodesAt3BeforeRemoving = treeNode.getNodesAtHeight(3)
            List<String> tipsBeforeRemoving = treeNode.getTips()
            long maxLengthBeforeRemoving = treeNode.getMaxLength()
            NodeTest bestNodeBeforeRemoving = treeNode.getLastNode()

            boolean threeARemoved = treeNode.removeNode("3A")

            List<String> tipsAfterRemoving = treeNode.getTips()
            int oneHeight = treeNode.getHeight("1").getAsInt()
            int twoHeight = treeNode.getHeight("2").getAsInt()
            Optional<NodeTest> nodeRemoved = treeNode.getNode("3A")
            List<String> nodesAt3AfterRemoving = treeNode.getNodesAtHeight(3)
            long maxLengthAfterRemoving = treeNode.getMaxLength()
            NodeTest bestNodeAfterRemoving = treeNode.getLastNode()

        then:
            treeNode.size() == 4
            threeARemoved
            oneHeight == 1
            twoHeight == 2
            nodeRemoved.isEmpty()
            nodesAt3BeforeRemoving.size() == 2
            nodesAt3BeforeRemoving.contains(node("3A").getId())
            nodesAt3AfterRemoving.size() == 1
            nodesAt3AfterRemoving.contains(node("3B").getId())
            !nodesAt3AfterRemoving.contains(node("3A").getId())
            tipsBeforeRemoving.size() == 2
            tipsBeforeRemoving.contains(node("5").getId())
            tipsBeforeRemoving.contains(node("3B").getId())
            tipsAfterRemoving.size() == 1
            tipsAfterRemoving.contains(node("3B").getId())
            maxLengthBeforeRemoving == 6
            maxLengthAfterRemoving == 4
            bestNodeBeforeRemoving.getId().equals("5")
            bestNodeAfterRemoving.getId().equals("3B")
    }
}
