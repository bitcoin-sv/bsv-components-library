package com.nchain.jcl.tools.tree;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * @date 26/10/2021
 */
public class NodeLocator<NodeId, NodeData extends Node<NodeId>> {

    // Keeps the location of every single Node in the Tree.
    //private Map<NodeId, NodeLocation<NodeId, NodeData>> nodesIndex = new HashMap<NodeId, NodeLocation<NodeId, NodeData>>(100_000, 0.5F);
    private Map<NodeId, NodeLocation<NodeId, NodeData>> nodesIndex = new HashMap<>();

    // Keeps the Nodes indexed by Height
   // private Map<Long, Set<NodeId>> nodesByHeight = new ConcurrentHashMap<>();

    public void update(PartialTree<NodeId, NodeData> nodeTree) {
        IntStream.range(0, nodeTree.trunk.size()).forEach(i -> update(nodeTree.trunk.get(i), nodeTree, i));
    }

    public void update(NodeData nodeData, PartialTree<NodeId, NodeData> partialTree, int relativeHeight) {

        nodesIndex.merge(nodeData.getId(), new NodeLocation<>(partialTree, relativeHeight), (oldValue, newValue) -> {
            oldValue.partialTree = newValue.partialTree;
            oldValue.relativeHeight = newValue.relativeHeight;
            return oldValue;
        });
        //nodesIndex.put(nodeData.getId(), new NodeLocation<>(partialTree, relativeHeight));

        /*
        nodesByHeight.merge(partialTree.height + relativeHeight,
                Stream.of(nodeData.getId()).collect(Collectors.toSet()),
                (oldValue, newValue) -> {
                    oldValue.addAll(newValue);
                    return oldValue;
        });
    */

    }

    public void remove(PartialTree<NodeId, NodeData> nodeTree) {
        nodeTree.trunk.forEach(this::remove);
        nodeTree.branches.forEach(branchTree -> remove(branchTree));
    }

    public void remove(NodeData nodeData) {
        NodeLocation<NodeId, NodeData> nodeLocation = nodesIndex.get(nodeData.getId());
       // nodesByHeight.get(nodeLocation.partialTree.height + nodeLocation.relativeHeight).remove(nodeData.getId());
        nodesIndex.remove(nodeData.getId());
    }

    public boolean contains(NodeId nodeId) {
        return nodesIndex.containsKey(nodeId);
    }

    public NodeLocation<NodeId, NodeData> getNodeLocation(NodeId nodeId) {
        return nodesIndex.get(nodeId);
    }
/*
    public List<NodeId> getNodesAtHeight(long height) {
        return nodesByHeight.containsKey(height) ? nodesByHeight.get(height).stream().collect(Collectors.toList()) : new ArrayList<>();
    }
*/
    public long size() {
        return nodesIndex.size();
    }

}
