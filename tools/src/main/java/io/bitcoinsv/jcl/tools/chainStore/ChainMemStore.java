package io.bitcoinsv.jcl.tools.chainStore;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * @author i.fernandez@nchain.com
 * @author d.vrankar@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This class stores a blockchhain, where each Node can hoold any data type that implements the "NodeData" interface.
 * All the nodes are stored in memory, and structured internally as a Tree-like structure using the "ChainTree" class.
 * It also keeps track of the TIPS of all the current chains
 *
 * NOTE: IT DOES NOT SUPPORT ORPHANS. ANY BLOCK MUST BE ABLE TO TRAVERSE BACK TO THE GENESIS NODE
 *
 * This class is Thread-Safe
 *
 * (based on initial implementation by Domen Vrankar)
 */
public class ChainMemStore<NodeId, NodeData extends Node<NodeId>> {

    // A node Locator used ot keep the references to all the Nodes
    private NodeIndexer<NodeId, NodeData> nodeLocator;
    // Root ChainTree:
    private ChainTree<NodeId, NodeData> rootNode;
    // For Multi-thread sake:
    private ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Constructor
     * @param genesisNode Genesis Node that will make the Root of the Blockchain.
     */
    public ChainMemStore(NodeData genesisNode, int startingHeight) {
        this.nodeLocator = new NodeIndexer<>();
        this.rootNode = new ChainTree<>(null, startingHeight, genesisNode, nodeLocator);
        this.nodeLocator.update(this.rootNode);
    }

    /** Convenience constructor */
    public ChainMemStore(NodeData genesisNode) {
        this(genesisNode, 0);
    }

    /**
     * Adds a Node to the ChainTree.
     * @param parentId  Id of the Parent
     * @param nodeData  Node to add
     * @return          TRUE if inserted, FALSE if node already saved, nul if parent does NOT exist
     */
    public Boolean addNode(NodeId parentId, NodeData nodeData) {
        try {
            lock.writeLock().lock();
            ChainTree<NodeId, NodeData> parentTree = nodeLocator.getNodeTree(parentId);
            if (parentTree == null) return null;
            NodeData parentNode = parentTree.getNode(parentId);
            return parentTree.addNode(parentNode, nodeData);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Returns the Node specified, if its exists
     */
    public Optional<NodeData> getNode(NodeId nodeId) {
        try {
            lock.readLock().lock();
            ChainTree<NodeId, NodeData> nodeTree = nodeLocator.getNodeTree(nodeId);
            if (nodeTree == null) return Optional.empty();
            return Optional.of(nodeTree.getNode(nodeId));
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * It returns the Parent Node of the node specified, if exists
     * @return The Parent, or and Empty Optional if either Parent ot node itself do not exist
     */
    public Optional<NodeData> getParentNode(NodeId nodeId) {
        try {
            lock.readLock().lock();
            ChainTree<NodeId, NodeData> nodeTree = nodeLocator.getNodeTree(nodeId);
            if (nodeTree == null) return Optional.empty();

            NodeData nodeData = nodeTree.getNode(nodeId);
            Optional<NodeData> result = nodeTree.getParentNode(nodeData);
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

    // Returns the list of Trees that contains Nodes at the given Height
    // NOTE: This is a recursive function, but in a blockchain-like structure the number of branches is small so we
    // should not have to worry about StackOverflow and this should be very close to O(1)
    private List<ChainTree<NodeId, NodeData>> getTreeContainingHeight(long height, ChainTree<NodeId, NodeData> currentTree) {
        if (currentTree.startingHeight <= height) {
            if (height <= (currentTree.startingHeight + currentTree.trunk.size() - 1)) {
                return Arrays.asList(currentTree);
            } else {
                return currentTree.branches.stream().map(tree -> getTreeContainingHeight(height, tree)).flatMap(List::stream).collect(Collectors.toList());
            }
        } else return new ArrayList<>();
    }

    /**
     * Returns the list of Nodes at a certain height (more than 1 if there is a fork)
     */
    public List<NodeId> getNodesAtHeight(long height) {
        try {
            lock.readLock().lock();
            List<ChainTree<NodeId, NodeData>> treesContainingHeight = getTreeContainingHeight(height, rootNode);
            List<NodeId> result = treesContainingHeight.stream().map(tree -> tree.getNode((int)(height - tree.startingHeight)).getId()).collect(Collectors.toList());
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }


    /**
     * Returns the Height of the Node given, or empty if it doesn't exist
     */
    public OptionalInt getHeight(NodeId nodeId) {
        try {
            lock.readLock().lock();
            ChainTree<NodeId, NodeData> nodeTree = nodeLocator.getNodeTree(nodeId);
            if (nodeTree == null) return OptionalInt.empty();
            return OptionalInt.of(nodeTree.startingHeight + nodeTree.nodesRelativeHeight.get(nodeId));
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * It removes the node given and re-adjusts the Tree accordingly. It returns TRUE if the node has been removed, or
     * FALSE if it doesn't exist.
     */
    public Boolean removeNode(NodeId nodeId) {
        try {
            lock.writeLock().lock();
            ChainTree<NodeId, NodeData> nodeTree = nodeLocator.getNodeTree(nodeId);
            if (nodeTree == null) return false;
            NodeData node = nodeTree.getNode(nodeTree.nodesRelativeHeight.get(nodeId));
            return nodeTree.removeNode(node);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Returns the length of the chain from this Node to the Tip, following the longest chain
     */
    public OptionalLong getLengthUpToTip(NodeId nodeId) {
        ChainTree<NodeId, NodeData> nodeTree = nodeLocator.getNodeTree(nodeId);
        if (nodeTree == null) { return OptionalLong.empty();}

        return OptionalLong.of(nodeTree.maxLength() - (nodeTree.nodesRelativeHeight.get(nodeId) - nodeTree.startingHeight));
    }

    /**
     * Returns the total number of Nodes
     */
    public long size() {
        try {
            lock.readLock().lock();
            return nodeLocator.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns the length of the longest chain
     */
    public long getMaxLength() {
        try {
            lock.readLock().lock();
            return rootNode.maxLength();
        } finally {
            lock.readLock().unlock();
        }
    }

    // Recursive function to get the Tips.
    // NOTE: In a blockchain-like structure, number of branches is small so we should not have to worry about StackOverflow
    private List<NodeId> getTips(ChainTree<NodeId, NodeData> tree) {
        if (tree.branches.size() == 0) {
            return Arrays.asList(tree.trunk.get(tree.trunk.size() - 1).getId());
        } else {
            return tree.branches.stream().map(this::getTips).flatMap(List::stream).collect(Collectors.toList());
        }
    }

    /**
     * Returns the List of Nodes that represents Tips of the BlockChain.
     */
    public List<NodeId> getTips() {
        try {
            lock.readLock().lock();
            return getTips(rootNode);
        } finally {
            lock.readLock().unlock();
        }
    }

    /** Indicates if it contains the Node given */
    public boolean contains(NodeId nodeId) {
        try {
            lock.readLock().lock();
            return nodeLocator.contains(nodeId);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns the Last Node of the Longest Chain. If There are more than one chain we return one of them
     */
    public NodeData getLastNode() {
        try {
            lock.readLock().lock();
            long longestChainLength = getMaxLength();
            List<NodeId> nodes = getNodesAtHeight(longestChainLength - 1);
            return getNode(nodes.get(0)).get(); // The first one. does it matter?
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns a List of Nodes that form a linear chain:
     * - It begins at the node at "fromHeight", and connects to "nodeId", and:
     *   if "includeChildrenLongestChain" is TRUE, the Path continues and also includes the blocks built AFTER "nodeId"
     *   and up to the tip of the LONGEST chain.
     * @param fromHeight height the Path will start on
     * @param nodeId Node Id that will make the end of the Path (if includeChildrenUpToNExtFork is false)
     * @param includeChildrenLongestChain It controls the length of the Path:
     *                                     - If FALSE: the Path will start at "fromHeight" and will finish on NodeId.
     *                                     - If TRUE: the path will start at "fromHeight" and will finish at the last
     *                                       Node AFTER "nodeId", following the longest chain after it.
     */
    // TODO: CHECK PERFORMANCE OF THIS METHOD!!!!!!!!
    public ChainPath<NodeData> getPath(int fromHeight, NodeId nodeId, boolean includeChildrenLongestChain) {
        try {
            lock.readLock().lock();

            OptionalInt nodeHeight = getHeight(nodeId);
            if (nodeHeight.isEmpty())               { return null;}
            if (nodeHeight.getAsInt() < fromHeight) { return new ChainPath<>(fromHeight);}

            ChainPath<NodeData> result = nodeLocator.getNodeTree(nodeId).getPath(nodeId, fromHeight, includeChildrenLongestChain);

            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * It prunes the Tree at the height given. If there are more than 1 block at the height given, representing different
     * branches, all of them but one is removed. the longest branch will be preserved.
     * NOTE: This method does NOT check the chain BEFORE this height, so its up to the caller to provide the right height.
     * Examples:
     * 1 - 2 - 2A - 3A
     *       - 2B - 3B
     *
     * If we call "prune(2)", the result will be:
     * 1 - 2
     *
     * If we call "prune(3) the result will be:
     * 1 - 2 - 2A - 3A
     *       - 2B
     * @param height height from whcih branches will be pruned, remaining only the loongest one.
     */
    public void prune(int height) {
        try {
            lock.writeLock().lock();
            List<NodeId> nodesAtHeight = getNodesAtHeight(height + 1);
            if (nodesAtHeight.size() > 1) {
                // We only remove all the nodes at this height expect the longest one.
                long maxLength = nodesAtHeight.stream().mapToLong(h -> getLengthUpToTip(h).getAsLong()).max().getAsLong();
                nodesAtHeight.stream().filter(n -> getLengthUpToTip(n).getAsLong() < maxLength).forEach(this::removeNode);

                // At this moment there should be only 1 branch remaining. But if some branches have the same
                // max length, then none of them will have been removed. So just in case, we remove again all the
                // branches but the first one
                nodesAtHeight = getNodesAtHeight(height + 1);
                if (nodesAtHeight.size() > 1) {
                    nodesAtHeight.stream().skip(1).forEach(this::removeNode);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
}
