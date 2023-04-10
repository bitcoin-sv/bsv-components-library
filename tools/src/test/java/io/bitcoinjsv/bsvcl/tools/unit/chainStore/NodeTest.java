package io.bitcoinjsv.bsvcl.tools.unit.chainStore;

import io.bitcoinsv.bsvcl.tools.chainStore.Node;

import java.util.Objects;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Class that represents the objects stored in the Chain in all th tests in this package.
 */
public class NodeTest implements Node<String> {
    String id;
    String title;

    /** Constructor */
    NodeTest(String id, String title) {
        this.id = id;
        this.title = title;
    }
    @Override
    public String getId() {
        return id;
    }
    @Override
    public String toString() {
        return id + " - " + title;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof NodeTest)) return false;
        NodeTest otherNode = (NodeTest) other;
        return Objects.equals(this.id, otherNode.id) && Objects.equals(this.title, otherNode.title);
    }
}
