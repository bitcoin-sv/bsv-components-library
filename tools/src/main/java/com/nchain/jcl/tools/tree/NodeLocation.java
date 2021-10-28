package com.nchain.jcl.tools.tree;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * @date 26/10/2021
 */
public class NodeLocation<NodeId, NodeData extends Node<NodeId>> {
    PartialTree<NodeId, NodeData> partialTree;
    int relativeHeight;

    public NodeLocation(PartialTree<NodeId, NodeData> partialTree, int relativeHeight) {
        this.partialTree = partialTree;
        this.relativeHeight = relativeHeight;
    }
}