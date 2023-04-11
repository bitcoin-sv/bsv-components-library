package io.bitcoinsv.bsvcl.common.unit.chainStore


import spock.lang.Specification

/**
 * A testing class for ChainPath
 */
class ChainPathSpec extends Specification {

    /**
     * We check that nodes are located properly inside the Chain Path
     */
    def "testing node existence, height zero"() {
        given:
            // A list of 3 nodes:
            List<String> path = Arrays.asList("A", "B", "C")
        when:
        io.bitcoinsv.bsvcl.common.chainStore.ChainPath<String> chainPath = new io.bitcoinsv.bsvcl.common.chainStore.ChainPath<>(path, 0)
            // Found:
            boolean oneFoundAt0 = chainPath.isInChain("A", 0)
            // Not Found (wrong height)
            boolean oneFoundAt1 = chainPath.isInChain("A", 1)
            // Found (checking a node at the end of the path):
            boolean threeFoundAt2 = chainPath.isInChain("C", 2)
            // Not Found (checking a node at the end of the path):
            boolean threeFoundAt1 = chainPath.isInChain("C", 1)
            // Not Found (wrong height and outside path's limits)
            boolean oneFoundAt100 = chainPath.isInChain("A", 100)
            // Not Found (unknown node)
            boolean notFound = chainPath.isInChain("X", 0)

        then:
            oneFoundAt0
            !oneFoundAt1
            threeFoundAt2
            !threeFoundAt1
            !oneFoundAt100
            !notFound
    }

    /**
     * We check that nodes are located properly inside the Chain Path
     */
    def "testing node existence, height not zero"() {
        given:
            // A list of 3 nodes:
            List<String> path = Arrays.asList("C", "D", "E")
        when:
            io.bitcoinsv.bsvcl.common.chainStore.ChainPath<String> chainPath = new io.bitcoinsv.bsvcl.common.chainStore.ChainPath<>(path, 2)
            // Found:
            boolean oneFoundAt0 = chainPath.isInChain("C", 2)
            // Not Found (wrong height)
            boolean oneFoundAt1 = chainPath.isInChain("C", 3)
            // Found (checking a node at the end of the path):
            boolean threeFoundAt2 = chainPath.isInChain("E", 4)
            // Not Found (checking a node at the end of the path):
            boolean threeFoundAt1 = chainPath.isInChain("E", 3)
            // Not Found (wrong height and outside path's limits)
            boolean oneFoundAt100 = chainPath.isInChain("C", 100)
            // Found (node is in a part of the chain BEFORE this ChainPath)
            boolean foundBeforePath = chainPath.isInChain("X", 0)

        then:
            oneFoundAt0
            !oneFoundAt1
            threeFoundAt2
            !threeFoundAt1
            !oneFoundAt100
            foundBeforePath
    }

    def "testing getting Blocks in a Chain with height Zero"() {
        given:
            List<String> path = Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H")
        when:
            io.bitcoinsv.bsvcl.common.chainStore.ChainPath<String> chainPath = new io.bitcoinsv.bsvcl.common.chainStore.ChainPath<>(path, 0)
            Optional<String> nodeA = chainPath.getNode(0)
            Optional<String> nodeC = chainPath.getNode(2)
            Optional<String> nodeH = chainPath.getNode(path.size() - 1)

            Optional<String> nodeX = chainPath.getNode(10)
        then:
            nodeA.isPresent() && nodeA.get().equals("A")
            nodeC.isPresent() && nodeC.get().equals("C")
            nodeH.isPresent() && nodeH.get().equals("H")
            nodeX.isEmpty()
    }

    def "testing getting Blocks in a Chain with height Not Zero"() {
        given:
            List<String> path = Arrays.asList("D", "E", "F", "G", "H")
        when:
            io.bitcoinsv.bsvcl.common.chainStore.ChainPath<String> chainPath = new io.bitcoinsv.bsvcl.common.chainStore.ChainPath<>(path, 3)
            Optional<String> nodeA = chainPath.getNode(0)
            Optional<String> nodeD = chainPath.getNode(3)
            Optional<String> nodeF = chainPath.getNode(5)
            Optional<String> nodeH = chainPath.getNode(7)

            Optional<String> nodeX = chainPath.getNode(10)
        then:
            nodeA.isEmpty()
            nodeD.isPresent() && nodeD.get().equals("D")
            nodeF.isPresent() && nodeF.get().equals("F")
            nodeH.isPresent() && nodeH.get().equals("H")
            nodeX.isEmpty()
    }

    def "testing Fork Comparison, same height"() {
        given:
            // Path 1:
            List<String> path1 = Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H")
            // Path 2:
            List<String> path2 = Arrays.asList("A", "B", "C", "D", "E2", "F2", "G2", "H2")
        when:
            io.bitcoinsv.bsvcl.common.chainStore.ChainPath<String> chainPath1 = new io.bitcoinsv.bsvcl.common.chainStore.ChainPath<>(path1, 0)
            io.bitcoinsv.bsvcl.common.chainStore.ChainPath<String> chainPath2 = new io.bitcoinsv.bsvcl.common.chainStore.ChainPath<>(path2, 0)
            Integer forkPoint = chainPath1.getForkPoint(chainPath2)
        then:
            forkPoint == 3
    }

    def "testing Fork Comparison, different height, this lower than other"() {
        given:
            // Path 1:
            List<String> path1 = Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H")
            // Path 2:
            List<String> path2 = Arrays.asList("C", "D", "E2", "F2", "G2", "H2")
        when:
            io.bitcoinsv.bsvcl.common.chainStore.ChainPath<String> chainPath1 = new io.bitcoinsv.bsvcl.common.chainStore.ChainPath<>(path1, 0)
            io.bitcoinsv.bsvcl.common.chainStore.ChainPath<String> chainPath2 = new io.bitcoinsv.bsvcl.common.chainStore.ChainPath<>(path2, 2)
            Integer forkPoint = chainPath1.getForkPoint(chainPath2)
        then:
            forkPoint == 3
    }

    def "testing Fork Comparison, different height, this higher than other"() {
        given:
            // Path 1:
            List<String> path1 = Arrays.asList("C", "D", "E2", "F2", "G2", "H2")
            // Path 2:
            List<String> path2 = Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H")
        when:
            io.bitcoinsv.bsvcl.common.chainStore.ChainPath<String> chainPath1 = new io.bitcoinsv.bsvcl.common.chainStore.ChainPath<>(path1, 2)
            io.bitcoinsv.bsvcl.common.chainStore.ChainPath<String> chainPath2 = new io.bitcoinsv.bsvcl.common.chainStore.ChainPath<>(path2, 0)
            Integer forkPoint = chainPath1.getForkPoint(chainPath2)
        then:
            forkPoint == 3
    }

    def "testing Fork Comparison, no overlap"() {
        given:
            // Path 1:
            List<String> path1 = Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H")
            // Path 2:
            List<String> path2 = Arrays.asList("C", "D", "E2", "F2", "G2", "H2")
        when:
            io.bitcoinsv.bsvcl.common.chainStore.ChainPath<String> chainPath1 = new io.bitcoinsv.bsvcl.common.chainStore.ChainPath<>(path1, 0)
            io.bitcoinsv.bsvcl.common.chainStore.ChainPath<String> chainPath2 = new io.bitcoinsv.bsvcl.common.chainStore.ChainPath<>(path2, 10)
            Integer forkPoint = chainPath1.getForkPoint(chainPath2)
        then:
            forkPoint == null
    }

    def "testing getting subList of Blocks"() {
        given:
            List<String> path1 = Arrays.asList("A", "B", "C", "D", "E")
        when:
            io.bitcoinsv.bsvcl.common.chainStore.ChainPath<String> chainPath = new io.bitcoinsv.bsvcl.common.chainStore.ChainPath<>(path1, 0)
            List<String> nodes = chainPath.getNodesFrom(2)
        then:
            nodes.size() == 3
            nodes.contains("C")
            nodes.contains("D")
            nodes.contains("E")
    }

    def "testing getting subList of Blocks, height before Path"() {
        given:
            List<String> path1 = Arrays.asList("D", "E", "F", "G", "H")
        when:
            io.bitcoinsv.bsvcl.common.chainStore.ChainPath<String> chainPath = new io.bitcoinsv.bsvcl.common.chainStore.ChainPath<>(path1, 3)
            chainPath.getNodesFrom(1)
        then:
            thrown(RuntimeException)

    }

    def "testing getting subList of Blocks, height after Path length"() {
        given:
            List<String> path1 = Arrays.asList("A", "B", "C", "D", "E")
        when:
            io.bitcoinsv.bsvcl.common.chainStore.ChainPath<String> chainPath = new io.bitcoinsv.bsvcl.common.chainStore.ChainPath<>(path1, 0)
            List<String> nodes = chainPath.getNodesFrom(10)
        then:
            nodes == null
    }

}
