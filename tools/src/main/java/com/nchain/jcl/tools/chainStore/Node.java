package com.nchain.jcl.tools.chainStore;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A Generic representation of a Node. It can hold anything, but it must provide a method to return its Id.
 * @param <NodeId> Node Id/Pk
 *
 * NOTE: If the class that implements this interface also overrides the "hashCode" and "equals" methods, that might
 *       have a huge effect on performance, so be careful with that.
 */
public interface Node<NodeId> {
    /** Returns the Id of this Node */
    NodeId getId();

    /**
     * An alternative version of "equals" that is used when comparing instances of "ChainPath<Node>" classes. We use
     * this version instead of overriding the "equals" method because tht might have a huge effect on performance in
     * the ChainMemStore class.
     */
    default boolean isEqual(Node<NodeId> other) { return this == other;}
}
