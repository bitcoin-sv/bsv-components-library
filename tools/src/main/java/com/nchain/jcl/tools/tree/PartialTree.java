package com.nchain.jcl.tools.tree;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * @date 26/10/2021
 */
public class PartialTree<NodeId, NodeData extends Node<NodeId>> {
    protected PartialTree<NodeId, NodeData> parent;
    protected List<NodeData> trunk = new ArrayList<>();
    protected List<PartialTree<NodeId, NodeData>> branches = new ArrayList<>();
    protected int height;

    public PartialTree(PartialTree<NodeId, NodeData> parentTree, int height) {
        this.parent = parentTree;
        this.height = height;
    }

    public PartialTree(PartialTree<NodeId, NodeData> parentTree, int height, List<NodeData> nodes) {
        this(parentTree, height);
        trunk.addAll(nodes);
    }

    public PartialTree(PartialTree<NodeId, NodeData> parentTree, int height, NodeData node) {
        this(parentTree, height);
        trunk.add(node);
    }

    private PartialTree<NodeId, NodeData> copyFromHeight(int height) {
        PartialTree<NodeId, NodeData> branch = new PartialTree<>(this, this.height + height,
                trunk.subList(height, trunk.size()));
        branch.branches = new ArrayList<>(this.branches);
        return branch;
    }

    private void addFork(int parentIndex, NodeData nodeData, NodeLocator<NodeId, NodeData> nodeLocator) {
        // One branch is "extracted/copied from the current trunk". Te other contains only the new Node added
        PartialTree extractedTree = copyFromHeight(parentIndex + 1);
        PartialTree newTree = new PartialTree(this, this.height + parentIndex + 1, nodeData);

        // We update the node locations of both branches:
        nodeLocator.update(extractedTree);
        nodeLocator.update(newTree);

        // We "cut" the trunk at the parent height, and we add the new 2 branches at the end...
        this.trunk = this.trunk.subList(0, parentIndex + 1);
        this.branches.clear();
        this.branches.add(extractedTree);
        this.branches.add(newTree);
    }

    public boolean addNode(NodeData parentNode, NodeData nodeData, NodeLocator<NodeId, NodeData> nodeLocator) {
        if (!trunk.contains(parentNode)) return false;
        if (trunk.contains(nodeData))    return false;
        int parentIndex = trunk.indexOf(parentNode);

        // If the parent is in the middle of the trunk, its a Fork:
        if (parentIndex < (trunk.size() - 1)) {
            addFork(parentIndex, nodeData, nodeLocator);
            return true;
        }

        // The parent is the last node. If there are some branches already we add one more, otherwise we just add one
        // node to the trunk
        if (this.branches.isEmpty()) {
            this.trunk.add(nodeData);
            nodeLocator.update(nodeData, this, parentIndex + 1);
        } else {
            PartialTree newBranch = new PartialTree<>(this, this.height + this.trunk.size(), nodeData);
            this.branches.add(newBranch);
            nodeLocator.update(newBranch);
        }
        return true;
    }

    public boolean removeNode(NodeData parentNode, NodeData nodeData, NodeLocator<NodeId, NodeData> nodeLocator) {
        if (!trunk.contains(nodeData)) return false;
        int nodeIndex = trunk.indexOf(nodeData);

        // We wrap up everything that needs to be removed in a Tree and update its location in the NodeLocator
        PartialTree<NodeId, NodeData> treeToRemove = copyFromHeight(nodeIndex);
        nodeLocator.remove(treeToRemove);

        // If the node is the first one, we remove this Tree from its parent, otherwise we just cut part of the trunk.
        if (nodeIndex == 0) {
            this.parent.branches.remove(this);
            // If after removing this Tree from the parent, the parent only has one remaining branch, we flatten the parent
            if (parent.branches.size() == 1) {
                parent.flatten(nodeLocator);
            }
        } else {
            this.trunk = this.trunk.subList(0, nodeIndex);
            this.branches.clear();
        }
        return true;
    }

    public Optional<NodeData> getParentNode(NodeData node) {
        if (!trunk.contains(node)) return Optional.empty();

        int nodeIndex = trunk.indexOf(node);
        Optional<NodeData> result = (nodeIndex > 0)
                ? Optional.of(trunk.get(nodeIndex - 1))
                : (parent != null)
                    ? Optional.of(parent.trunk.get(parent.trunk.size() - 1))
                    : Optional.empty();
        return result;
    }

    private void flatten(NodeLocator<NodeId, NodeData> nodeLocator) {
        if (branches.size() > 1) throw new RuntimeException("Impossible to flatten where there are more than 1 Branch");
        if (branches.isEmpty()) return;
        PartialTree<NodeId, NodeData> remainingBranch = branches.get(0);

        // We update the location of the branch "flattened":
        for (int i = 0; i < remainingBranch.trunk.size(); i++) {
            nodeLocator.update(remainingBranch.trunk.get(i), this,trunk.size() + i);
        }
        // Now we add the trunk of the branch to our Trunk, and its branches become ours:
        trunk.addAll(remainingBranch.trunk);
        branches = new ArrayList<>(remainingBranch.branches);
        remainingBranch.branches.clear();
    }

    public long size() {
        return trunk.size() + branches.stream().mapToLong(b -> b.size()).sum();
    }

    public NodeData getNode(int relativeHeight) {
        return trunk.get(relativeHeight);
    }

}