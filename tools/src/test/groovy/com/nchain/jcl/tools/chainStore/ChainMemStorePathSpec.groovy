package com.nchain.jcl.tools.chainStore

import spock.lang.Specification
import static com.nchain.jcl.tools.chainStore.NodeTestFactory.*

/**
 * Testing class for the "getPath()" method
 */
class ChainMemStorePathSpec extends Specification {

    /**
     * We test getting a Path from a linear chain, at different heights:
     * Chain Tree:
     * genesis - 1 - 2 - 3 - 4 - 5 - 6
     */
    def "Simple Path"() {
        given:
            // We build the Chain at height zero:
            ChainMemStore<String, NodeTest> chain = new ChainMemStore<>(genesis(), 0)
            chain.addNode(null, genesis())
            chain.addNode(genesis().getId(), node("1"))
            chain.addNode(node("1").getId(), node("2"))
            chain.addNode(node("2").getId(), node("3"))
            chain.addNode(node("3").getId(), node("4"))
            chain.addNode(node("4").getId(), node("5"))
            chain.addNode(node("5").getId(), node("6"))

        when:
            // we get different Paths:

            // Path to node3, from genesis, NOT including descendents
            ChainPath<NodeTest> path3 = chain.getPath(0, node("3").getId(), false)
            ChainPath<NodeTest> path3Expected = new ChainPath<>(
                    Arrays.asList(genesis(), node("1"), node("2"), node("3")), 0)

            // Path to node3, from genesis, including descendents
            ChainPath<NodeTest> path3B = chain.getPath(0, node("3").getId(), true)
            ChainPath<NodeTest> path3BExpected = new ChainPath<>(
                    Arrays.asList(genesis(), node("1"), node("2"), node("3"), node("4"), node("5"), node("6")), 0)

            // Path to node3, from height 2, not including descendents
            ChainPath<NodeTest> path3C = chain.getPath(2, node("3").getId(), false)
            ChainPath<NodeTest> path3CExpected = new ChainPath<>(
                Arrays.asList(node("2"), node("3")), 0)

            // Path to node3, from height 10, not including descendents
            ChainPath<NodeTest> path3D = chain.getPath(10, node("3").getId(), false)
            ChainPath<NodeTest> path3DExpected = new ChainPath<>(new ArrayList<Node>(), 10)

            // Path to an non-existing node:
            ChainPath<NodeTest> path9 = chain.getPath(0, node("9").getId(), false)

        then:
            // And we check:
            path3.equals(path3Expected)
            path3B.equals(path3BExpected)
            path3C.equals(path3CExpected)
            path3D.equals(path3DExpected)
            path9 == null
    }

    /**
     * We test the same operations in a Chain with multiple forks:
     * chain:
     * genesis - 1 - 2 - 3  - 4  - 5
     *                |- 3B - 4B - 5B
     *                |- 3C - 4C - 5C
     *                     |- 4D - 5D
     */
    def "multiple branches"() {
        given:
            // We build the Chain at height zero:
            ChainMemStore<String, NodeTest> chain = new ChainMemStore<>(genesis(), 0)
            chain.addNode(null, genesis())
            chain.addNode(genesis().getId(), node("1"))
            chain.addNode(node("1").getId(), node("2"))
            chain.addNode(node("2").getId(), node("3"))
            chain.addNode(node("3").getId(), node("4"))
            chain.addNode(node("4").getId(), node("5"))

            chain.addNode(node("2").getId(), node("3B"))
            chain.addNode(node("3B").getId(), node("4B"))
            chain.addNode(node("4B").getId(), node("5B"))

            chain.addNode(node("2").getId(), node("3C"))
            chain.addNode(node("3C").getId(), node("4C"))
            chain.addNode(node("4C").getId(), node("5C"))

            chain.addNode(node("3C").getId(), node("4D"))
            chain.addNode(node("4D").getId(), node("5D"))

        when:
            // we get different Paths:

            // Path to 3C, including descendents
            ChainPath<NodeTest> path3C = chain.getPath(0, node("3C").getId(), true)
            ChainPath<NodeTest> path3CExpected = new ChainPath<>(
                Arrays.asList(genesis(), node("1"), node("2"), node("3C"), node("4C"), node("5C")), 0)

            // Path to 5D, including descendents
            ChainPath<NodeTest> path5D = chain.getPath(0, node("5D").getId(), true)
            ChainPath<NodeTest> path5DExpected = new ChainPath<>(
                    Arrays.asList(genesis(), node("1"), node("2"), node("3C"), node("4D"), node("5D")), 0)

        then:
            // And we check:
            path3C.equals(path3CExpected)
            path5D.equals(path5DExpected)
    }
}
