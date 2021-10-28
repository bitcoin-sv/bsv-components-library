package com.nchain.jcl.tools.tree;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * @author i.fernandez@nchain.com
 * @author d.vrankar@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A generic implementation of a Tree that can hold Nodes containind "NodeData" and can be searched by "NodeId".
 * This can be used to represent the clockchain and store any kind of data. The structure of the chain is technically
 * a Tree, but in the majority of scenarios multiple branches are rare, so that' why a more-coomong graph library might
 * not be efficiente and we developed this custom solution instead.
 *
 *
 */
public class TreeNode<NodeId, NodeData extends Node<NodeId>> {

    private NodeLocator<NodeId, NodeData> nodeLocator;

    private PartialTree<NodeId, NodeData> rootNode;


    private ReadWriteLock lock = new ReentrantReadWriteLock();

    public TreeNode(NodeData genesisNode) {
        this.nodeLocator = new NodeLocator<>();
        this.rootNode = new PartialTree<>(null, 0, genesisNode);
        this.nodeLocator.update(this.rootNode);
    }

    public Boolean addNode(NodeId parentId, NodeData nodeData) {
        try {
            lock.writeLock().lock();
            NodeLocation<NodeId, NodeData> nodeLocation = nodeLocator.getNodeLocation(parentId);
            if (nodeLocation == null) return false;
            NodeData parentNode = nodeLocation.partialTree.getNode(nodeLocation.relativeHeight);
            return nodeLocation.partialTree.addNode(parentNode, nodeData, nodeLocator);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Optional<NodeData> getNode(NodeId nodeId) {
        try {
            lock.readLock().lock();
            NodeLocation<NodeId, NodeData> nodeLocation = nodeLocator.getNodeLocation(nodeId);
            if (nodeLocation == null) return Optional.empty();
            return Optional.of(nodeLocation.partialTree.getNode(nodeLocation.relativeHeight));
        } finally {
            lock.readLock().unlock();
        }
    }

    public Optional<NodeData> getParentNode(NodeId nodeId) {
        try {
            lock.readLock().lock();
            NodeLocation<NodeId, NodeData> nodeLocation = nodeLocator.getNodeLocation(nodeId);
            if (nodeLocation == null) return Optional.empty();

            NodeData nodeData = nodeLocation.partialTree.getNode(nodeLocation.relativeHeight);
            Optional<NodeData> result = nodeLocation.partialTree.getParentNode(nodeData);
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

    private List<PartialTree<NodeId, NodeData>> getTreeContainingHeight(long height, PartialTree<NodeId, NodeData> currentTree) {
        if (currentTree.height <= height) {
            if (height <= (currentTree.height + currentTree.trunk.size() - 1)) {
                return Arrays.asList(currentTree);
            } else {
                return currentTree.branches.stream().map(tree -> getTreeContainingHeight(height, tree)).flatMap(List::stream).collect(Collectors.toList());
            }
        } else return new ArrayList<>();
    }

    public List<NodeId> getNodesAtHeight(long height) {
        try {
            lock.readLock().lock();
            List<PartialTree<NodeId, NodeData>> treesContainingHeight = getTreeContainingHeight(height, rootNode);
            List<NodeId> result = treesContainingHeight.stream().map(tree -> tree.getNode((int)(height - tree.height)).getId()).collect(Collectors.toList());
            return result;
            //return nodeLocator.getNodesAtHeight(height);
        } finally {
            lock.readLock().unlock();
        }
    }

    public OptionalInt getHeight(NodeId nodeId) {
        try {
            lock.readLock().lock();
            NodeLocation<NodeId, NodeData> nodeLocation = nodeLocator.getNodeLocation(nodeId);
            if (nodeLocation == null) return OptionalInt.empty();
            return OptionalInt.of(nodeLocation.partialTree.height + nodeLocation.relativeHeight);
        } finally {
            lock.readLock().unlock();
        }
    }

    public Boolean removeNode(NodeId nodeId) {
        try {
            lock.writeLock().lock();
            NodeLocation<NodeId, NodeData> nodeLocation = nodeLocator.getNodeLocation(nodeId);
            if (nodeLocation == null) return false;
            Optional<NodeData> tipParent = getParentNode(nodeId);
            if (tipParent.isEmpty()) return false;
            NodeData node = nodeLocation.partialTree.getNode(nodeLocation.relativeHeight);
            return nodeLocation.partialTree.removeNode(tipParent.get(), node, nodeLocator);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public long size() {
        try {
            lock.readLock().lock();
            return nodeLocator.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    private List<NodeId> getTips(PartialTree<NodeId, NodeData> tree) {
        if (tree.branches.size() == 0) {
            return Arrays.asList(tree.trunk.get(tree.trunk.size() - 1).getId());
        } else {
            return tree.branches.stream().map(this::getTips).flatMap(List::stream).collect(Collectors.toList());
        }
    }

    public List<NodeId> getTips() {
        try {
            lock.readLock().lock();
            return getTips(rootNode);
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean contains(NodeId nodeId) {
        try {
            lock.readLock().lock();
            return nodeLocator.contains(nodeId);
        } finally {
            lock.readLock().unlock();
        }
    }


}
