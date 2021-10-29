package com.nchain.jcl.tools.chainStore;

import java.util.*;
import java.util.stream.IntStream;

/**
 * @author i.fernandez@nchain.com
 * @author d.vrankar@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A Chain Tree represents a Tree-structure bit more suited for BlockChain, so:
 *  - If usually contains a long list of Nodes in the TRUNK.
 *  - If it has some branches, they are at the end. Each branch is another ChainTree.
 *  - The Root of this Tree might another Tree, or null if this is the ROOT)
 *
 * (based on initial implementation by Domen Vrankar)
 */
public class ChainTree<NodeId, NodeData extends Node<NodeId>> {
    // Reference to the Parent of this Tree:
    protected ChainTree<NodeId, NodeData> parent;

    // Contains the Nodes that make the Trunk of this Tree
    protected List<NodeData> trunk = new ArrayList<>();

    // For each Node in the TRUNK, it contains the relative height (index of the trunk), for fast random access
    protected Map<NodeId, Integer> nodesRelativeHeight = new HashMap<>();

    // If any, branches of this Tree are stored here:
    protected List<ChainTree<NodeId, NodeData>> branches = new ArrayList<>();

    // Height of this Tre in the "global" blockchain it might belong to. The height of all the nodes in the TRUNK are
    // (startingHeight + relativeHeight). relativeHeight is the index of each Node in the trunk.
    protected int startingHeight;

    // A reference to a Node Locator that keeps track of ALL the nodes in the blockchain (not only the ones in this
    // particular Tree). WE use this to update Nodes's location when we move them to a different Tree, remove them, etc
    private NodeIndexer<NodeId, NodeData> nodeLocator;

    /** Constructor */
    public ChainTree(ChainTree<NodeId, NodeData> parentTree, int height, NodeIndexer<NodeId, NodeData> nodeLocator) {
        this.parent = parentTree;
        this.startingHeight = height;
        this.nodeLocator = nodeLocator;
    }

    /** Constructor */
    public ChainTree(ChainTree<NodeId, NodeData> parentTree, int height, List<NodeData> nodes, NodeIndexer<NodeId, NodeData> nodeLocator) {
        this(parentTree, height, nodeLocator);
        trunk.addAll(nodes);
        IntStream.range(0, nodes.size()).forEach(i -> updateRelativeHeight(nodes.get(i).getId(), i));
    }

    /** Constructor */
    public ChainTree(ChainTree<NodeId, NodeData> parentTree, int height, NodeData node, NodeIndexer<NodeId, NodeData> nodeLocator) {
        this(parentTree, height, Arrays.asList(node), nodeLocator);
    }

    /** It adds a branch to this Tree. We assume the branch has already the right 'startingHeight' */
    protected void addBranch(ChainTree<NodeId, NodeData> branch) {
        this.branches.add(branch);
    }
    /** It adds branches to this Tree. We assume the branches have already the right 'startingHeight' */
    protected void addBranches(List<ChainTree<NodeId, NodeData>> branches) {
        this.branches.addAll(branches);
    }

    /* It updates the relative Height of the Node given */
    protected void updateRelativeHeight(NodeId nodeId, int relativeHeight) {
        nodesRelativeHeight.put(nodeId, relativeHeight);
    }

    /**
     * It cuts the trunk at the height given (included) and returns the remaining Trunk.
     * The relative heights of the Nodes cut is also removed from this tree.
     */
    private List<NodeData> cutTrunkAtHeight(int height) {
        int numNodesToExtract = trunk.size() - height;
        List<NodeData> nodesExtracted = new ArrayList<>();
        for (int i = 0; i < numNodesToExtract; i++) {
            NodeData nodeExtracted = trunk.get(height);
            nodesExtracted.add(nodeExtracted);
            nodesRelativeHeight.remove(nodeExtracted.getId());
            trunk.remove(height);
        }
        return nodesExtracted;
    }

    /**
     * It Cuts this Tree from a height given in the TRUNK. The state after this method is:
     * - The current Tree will truncate its TRUNK
     * - The current Tree will have NO branches at all.
     * This method returns a Tre that contains everything that has been "cut" from this Tree:
     *  - The remaining TRUNK
     *  - The branches that the original Trunk might have had.
     */
    private ChainTree<NodeId, NodeData> cutTreeAtHeight(int height) {
        // We extract from the trunk the nodes from "height" until the end...
        List<NodeData> nodesExtracted = cutTrunkAtHeight(height);

        // This is the Tree remaining, after cutting this one:
        int remainingTreeStartingHeight = this.startingHeight + height;
        ChainTree<NodeId, NodeData> remainingTree = new ChainTree<>(this, remainingTreeStartingHeight, nodesExtracted, this.nodeLocator);
        remainingTree.addBranches(new ArrayList<>(this.branches));
        this.branches.clear();
        return remainingTree;
    }


    /**
     * We create a Fork due to a new Node being added. This happens because the parent of the new Node is in the middle
     * of the TRUNk (not at the end). The state after this method is:
     * - the current Tree is TRUNCATED:
     *      - The TRUNK is Cut, so it only oes up to the Parent of the new Node
     *      - 2 branches are created and added:
     *          - 1 branch is a Tree with only the new Node
     *          - 1 branch is a Tree with the "extracted" part this tree after cutting at the parent Height.
     */
    private void addFork(int parentIndex, NodeData nodeData) {
        // One branch is "extracted/copied from the current trunk". Te other contains only the new Node added
        ChainTree extractedTree = cutTreeAtHeight(parentIndex + 1);
        ChainTree newTree = new ChainTree(this, this.startingHeight + parentIndex + 1, nodeData, this.nodeLocator);

        // We update the node locations of both branches:
        nodeLocator.update(extractedTree);
        nodeLocator.update(newTree);

        // We "cut" the trunk at the parent height, and we add the new 2 branches at the end...
        this.branches.clear();
        this.branches.add(extractedTree);
        this.branches.add(newTree);
    }

    /**
     * It adds a Node to this Tree. If the parent of the Node is at the end of the Tree, then its just added to the
     * Trunk at the end. But if the parent is in the middle of the trunk, then we create a FORK: After that parent, the
     * tree is cut and 2 branches are created:
     *  - 1 branch containing the new Node
     *  - 1 branch containing the previous tree after the cut below the parent
     */
    public boolean addNode(NodeData parentNode, NodeData nodeData) {
        if (!trunk.contains(parentNode)) return false;
        if (trunk.contains(nodeData))    return false;

        int parentIndex = nodesRelativeHeight.get(parentNode.getId());

        // If the parent is in the middle of the trunk, its a Fork:
        if (parentIndex < (trunk.size() - 1)) {
            addFork(parentIndex, nodeData);
            return true;
        }

        // The parent is the last node. If there are some branches already we add one more, otherwise we just add one
        // node to the trunk
        if (this.branches.isEmpty()) {
            this.trunk.add(nodeData);
            updateRelativeHeight(nodeData.getId(), this.trunk.size() - 1);
            nodeLocator.update(nodeData, this);
        } else {
            ChainTree newBranch = new ChainTree<>(this, this.startingHeight + this.trunk.size(), nodeData, this.nodeLocator);
            this.branches.add(newBranch);
            nodeLocator.update(newBranch);
        }
        return true;
    }

    /**
     * It removes a Node from this Tree. The sate after this method is:
     * - if the node is at the end of the Trunk we just remove it
     * - if the Node is in the middle of the Trunk, we vut the tree at that point and remove the whole "extracted" tree.
     * - If the Node is the first one, then the whole tree is removed. In this case, if this tree is a branch of a
     *   parent Tree, we remove the branch form the parent. And in this case, if the parent Tree only has one Branch
     *   left after deleting this one, we "flatten" the paren to simplify the Tree structure
     *
     */
    public boolean removeNode(NodeData nodeData) {
        if (!trunk.contains(nodeData)) return false;

        int nodeIndex = nodesRelativeHeight.get(nodeData.getId());

        // We wrap up everything that needs to be removed in a Tree and update its location in the NodeLocator
        ChainTree<NodeId, NodeData> treeToRemove = cutTreeAtHeight(nodeIndex);
        nodeLocator.remove(treeToRemove);

        // If the node is the first one, we remove this Tree from its parent, otherwise we just cut part of the trunk.
        if (nodeIndex == 0) {
            this.parent.branches.remove(this);
            // If after removing this Tree from the parent, the parent only has one remaining branch, we flatten the parent
            if (parent.branches.size() == 1) {
                parent.flatten();
            }
        }
        return true;
    }

    /**
     * Returns the PArent of this Node
     */
    public Optional<NodeData> getParentNode(NodeData node) {
        if (!trunk.contains(node)) return Optional.empty();

        int nodeIndex = nodesRelativeHeight.get(node.getId());
        Optional<NodeData> result = (nodeIndex > 0)
                ? Optional.of(trunk.get(nodeIndex - 1))
                : (parent != null)
                    ? Optional.of(parent.trunk.get(parent.trunk.size() - 1))
                    : Optional.empty();
        return result;
    }

    /**
     * If this Tree has ONLY one branch, then it adds the TRUNK of that branch to the current TRUNK and its branches.
     * So the structure is flattened.
     */
    private void flatten() {
        if (branches.size() > 1) throw new RuntimeException("Impossible to flatten where there are more than 1 Branch");
        if (branches.isEmpty()) return;
        ChainTree<NodeId, NodeData> remainingBranch = branches.get(0);

        // We update the location of the nodes in the branch that will be merged into the TRUNK:
        for (int i = 0; i < remainingBranch.trunk.size(); i++) {
            nodeLocator.update(remainingBranch.trunk.get(i), this);
            updateRelativeHeight(remainingBranch.trunk.get(i).getId(), trunk.size() + i);
        }
        // Now we add the trunk of the branch to our Trunk, and its branches become ours:
        trunk.addAll(remainingBranch.trunk);
        branches = new ArrayList<>(remainingBranch.branches);
        remainingBranch.branches.clear();
    }

    /** Returns the Total number of nodes including both the TRUNK and he BRANCHES */
    public long size() {
        return trunk.size() + branches.stream().mapToLong(b -> b.size()).sum();
    }

    /** Returns the maxLength of this Tree, that is the length of the best chain */
    public long maxLength() {
        return  branches.isEmpty()
                ? trunk.size()
                : trunk.size() + branches.stream().mapToLong(b -> b.maxLength()).max().getAsLong();
    }

    /** Returns the Node of this Tree at the relative Height*/
    public NodeData getNode(int relativeHeight) {
        return trunk.get(relativeHeight);
    }

    /** Returns the Node of this Tree */
    public NodeData getNode(NodeId nodeId) {
        return trunk.get(nodesRelativeHeight.get(nodeId));
    }

}