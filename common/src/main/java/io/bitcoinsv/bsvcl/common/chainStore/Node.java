package io.bitcoinsv.bsvcl.common.chainStore;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A Generic representation of a Node. It can hold anything, but it must provide a method to return its Id.
 * @param <NodeId> Node Id/Pk
 *
 */
public interface Node<NodeId> {
    /** Returns the Id of this Node */
    NodeId getId();
}
