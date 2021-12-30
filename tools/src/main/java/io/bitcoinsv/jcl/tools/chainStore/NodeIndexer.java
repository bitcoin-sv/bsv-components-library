package io.bitcoinsv.jcl.tools.chainStore;

import java.util.*;

/**
 * @author i.fernandez@nchain.com
 * @author d.vrankar@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This class keeps track of the location of every Node in the ChainTree. The locatio of a Node is the ChainTree the
 * node is stored in. This class is used in conjunctio with ChainTree, so every time a ChainTree adds/removes a Node,
 * it makes a call to this class so the reference to that Node is updated.
 *
 * (based on initial implementation by Domen Vrankar)
 */
public class NodeIndexer<NodeId, NodeData extends Node<NodeId>> {

    // For each Node, it keeps a reference to the Tree the Node is stored in
    private Map<NodeId, ChainTree<NodeId, NodeData>> nodeTreesIndex = new HashMap<>();

    // it updates the references to all the Nodes in the given Tree to point to that Tree
    void update(ChainTree<NodeId, NodeData> nodeTree) {
        nodeTree.trunk.forEach(node -> update(node, nodeTree));
    }

    // It updates the reference to the given Node to point to the given Tree
    void update(NodeData nodeData, ChainTree<NodeId, NodeData> partialTree) {
        nodeTreesIndex.put(nodeData.getId(), partialTree);
    }

    // It removes the reference of the given Node
    void remove(NodeData nodeData) {
        nodeTreesIndex.remove(nodeData.getId());
    }

    // It indicates if the Node exists */
    boolean contains(NodeId nodeId) {
        return nodeTreesIndex.containsKey(nodeId);
    }

    // returns the Tree the node is stored in
    ChainTree<NodeId, NodeData> getNodeTree(NodeId nodeId) {
        return nodeTreesIndex.get(nodeId);
    }

    // Returns the total number of Nodes
    long size() {
        return nodeTreesIndex.size();
    }

    // Removes the references of all the nodes in the tree given: the ones in the trunk and in tis branches
    void remove(ChainTree<NodeId, NodeData> nodeTree) {
        nodeTree.trunk.forEach(this::remove);
        nodeTree.branches.forEach(branchTree -> remove(branchTree));
    }
}
