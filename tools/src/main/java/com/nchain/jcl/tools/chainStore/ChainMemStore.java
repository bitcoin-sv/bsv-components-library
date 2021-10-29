package com.nchain.jcl.tools.chainStore;

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
    public ChainMemStore(NodeData genesisNode) {
        this.nodeLocator = new NodeIndexer<>();
        this.rootNode = new ChainTree<>(null, 0, genesisNode, nodeLocator);
        this.nodeLocator.update(this.rootNode);
    }

    /**
     * Adds a Node to the ChainTree.
     * @param parentId  Id of the Parent
     * @param nodeData  Node to add
     * @return          TRUE if inserted, FALSE if not (parent not present, or node already saved)
     */
    public Boolean addNode(NodeId parentId, NodeData nodeData) {
        try {
            lock.writeLock().lock();
            ChainTree<NodeId, NodeData> parentTree = nodeLocator.getNodeTree(parentId);
            if (parentTree == null) return false;
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
    // NOTE: In a blockchain-like structure, number of branches is small so we should not have to worry about StackOverflow
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
     * It removes the node given and re-adjusts the Tree accordingly. It returns TRU if the node has been removed, or
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
}
