package com.nchain.jcl.tools.tree

import spock.lang.Specification



class TreeNodeAddRemoveSpec extends Specification {

    class NodeTest implements Node<String> {
        String id;
        String title;
        NodeTest(String id, String title) {
            this.id = id;
            this.title = title;
        }
        @Override String getId() { return id;}
    }

    /**
     * We build [genesis]-[1]-[2]-[3].
     * [4] is NOT saved (parent not found)
     */
    def "adding Nodes to Trunk"() {
        given:
            NodeTest genesis = new NodeTest("0", "genesis")
            NodeTest node1 = new NodeTest("1", "one")
            NodeTest node2 = new NodeTest("2", "two")
            NodeTest node3 = new NodeTest("3", "three")
            NodeTest node4 = new NodeTest("4", "four")
        when:
            TreeNode<String, NodeTest> treeNode = new TreeNode<>(genesis)
            boolean node1Saved = treeNode.addNode(genesis.getId(), node1)
            boolean node2Saved = treeNode.addNode(node1.getId(), node2)
            boolean node3Saved = treeNode.addNode(node2.getId(), node3)
            boolean node4Saved = treeNode.addNode(node4.getId(), node4)         // NOT SAVED
            boolean node3SavedAgain =  treeNode.addNode(node2.getId(), node3)   // NOT SAVED

            long oneHeight = treeNode.getHeight("1").getAsLong()
            long twoHeight = treeNode.getHeight("2").getAsLong()
            long threeHeight = treeNode.getHeight("3").getAsLong()

            List<String> tips = treeNode.getTips()

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
            tips.contains(node3.getId())
    }

    /**
     * We build [genesis]-[1]-[2]
     *                         |-[3A]
     *                           |-[4]-[5]
     *                         |-[3B]
     */
    def "adding Branches"() {
        given:
            NodeTest genesis = new NodeTest("0", "genesis")
            NodeTest node1 = new NodeTest("1", "one")
            NodeTest node2 = new NodeTest("2", "two")
            NodeTest node3A = new NodeTest("3A", "three-A")
            NodeTest node3B = new NodeTest("3B", "three-B")
            NodeTest node4 = new NodeTest("4", "four")
            NodeTest node5 = new NodeTest("5", "five")
        when:
            TreeNode<String, NodeTest> treeNode = new TreeNode<>(genesis)
            treeNode.addNode(genesis.getId(), node1)
            treeNode.addNode(node1.getId(), node2)
            treeNode.addNode(node2.getId(), node3A)
            treeNode.addNode(node2.getId(), node3B)
            treeNode.addNode(node3A.getId(), node4)
            treeNode.addNode(node4.getId(), node5)

            long oneHeight = treeNode.getHeight("1").getAsLong()
            long twoHeight = treeNode.getHeight("2").getAsLong()
            long threeAHeight = treeNode.getHeight("3A").getAsLong()
            long threeBHeight = treeNode.getHeight("3B").getAsLong()
            long fourAHeight = treeNode.getHeight("4").getAsLong()
            long fiveBHeight = treeNode.getHeight("5").getAsLong()

            List<String> nodesAtHeight3 = treeNode.getNodesAtHeight(3);
            List<String> tips = treeNode.getTips()

        then:
            treeNode.size() == 7
            oneHeight == 1
            twoHeight == 2
            threeAHeight == 3
            threeBHeight == 3
            fourAHeight == 4
            fiveBHeight == 5
            nodesAtHeight3.size() == 2
            nodesAtHeight3.contains(node3A.getId())
            nodesAtHeight3.contains(node3B.getId())
            tips.size() == 2
            tips.contains(node5.getId())
            tips.contains(node3B.getId())
    }

    /**
     * We build this branch: We build [genesis]-[1]-[2]-[3].
     * Then we Remove [2], so we should get: [genesis]-[1].
     */
    def "adding and removing Blocks from Trunk"() {
        given:
            NodeTest genesis = new NodeTest("0", "genesis")
            NodeTest node1 = new NodeTest("1", "one")
            NodeTest node2 = new NodeTest("2", "two")
            NodeTest node3 = new NodeTest("3", "three")
        when:
            TreeNode<String, NodeTest> treeNode = new TreeNode<>(genesis)
            treeNode.addNode(genesis.getId(), node1)
            treeNode.addNode(node1.getId(), node2)
            treeNode.addNode(node2.getId(), node3)

            List<String> tipsBeforeRemoving = treeNode.getTips()
            boolean twoRemoved = treeNode.removeNode("2")
            boolean unknownRemoved = treeNode.removeNode("xx");
            List<String> tipsAfterRemoving = treeNode.getTips()

        then:
            treeNode.size() == 2
            twoRemoved
            !unknownRemoved
            tipsBeforeRemoving.size() == 1
            tipsBeforeRemoving.contains(node3.getId())
            tipsAfterRemoving.size() == 1
            tipsAfterRemoving.contains(node1.getId())
    }

    /**
     * We build [genesis]-[1]-[2]
     *                         |-[3A]
     *                           |-[4]-[5]
     *                         |-[3B]
     *
     * Then we remove [3A], so we should get:
     * [genesis]-[1]-[2]
     *                |-[3B]
     *
     */
    def "adding and removing Blocks from Branches"() {
        given:
            NodeTest genesis = new NodeTest("0", "genesis")
            NodeTest node1 = new NodeTest("1", "one")
            NodeTest node2 = new NodeTest("2", "two")
            NodeTest node3A = new NodeTest("3A", "three-A")
            NodeTest node3B = new NodeTest("3B", "three-B")
            NodeTest node4 = new NodeTest("4", "four")
            NodeTest node5 = new NodeTest("5", "five")
        when:
            TreeNode<String, NodeTest> treeNode = new TreeNode<>(genesis)
            treeNode.addNode(genesis.getId(), node1)
            treeNode.addNode(node1.getId(), node2)
            treeNode.addNode(node2.getId(), node3A)
            treeNode.addNode(node2.getId(), node3B)
            treeNode.addNode(node3A.getId(), node4)
            treeNode.addNode(node4.getId(), node5)

            List<String> nodesAt3BeforeRemoving = treeNode.getNodesAtHeight(3)
            List<String> tipsBeforeRemoving = treeNode.getTips()

            boolean threeARemoved = treeNode.removeNode("3A")

            List<String> tipsAfterRemoving = treeNode.getTips()
            long oneHeight = treeNode.getHeight("1").getAsLong()
            long twoHeight = treeNode.getHeight("2").getAsLong()
            Optional<NodeTest> nodeRemoved = treeNode.getNode("3A")
            List<String> nodesAt3AfterRemoving = treeNode.getNodesAtHeight(3)

        then:
            treeNode.size() == 4
            threeARemoved
            oneHeight == 1
            twoHeight == 2
            nodeRemoved.isEmpty()
            nodesAt3BeforeRemoving.size() == 2
            nodesAt3BeforeRemoving.contains(node3A.getId())
            nodesAt3BeforeRemoving.contains(node3A.getId())
            nodesAt3AfterRemoving.size() == 1
            nodesAt3AfterRemoving.contains(node3B.getId())
            !nodesAt3AfterRemoving.contains(node3A.getId())
            tipsBeforeRemoving.size() == 2
            tipsBeforeRemoving.contains(node5.getId())
            tipsBeforeRemoving.contains(node3B.getId())
            tipsAfterRemoving.size() == 1
            tipsAfterRemoving.contains(node3B.getId())
    }
}
