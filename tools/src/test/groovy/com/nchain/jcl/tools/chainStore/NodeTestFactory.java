package com.nchain.jcl.tools.chainStore;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A Factory to create instances of NodeTest that are used in testing
 */
public class NodeTestFactory {

    public static NodeTest genesis() { return new NodeTest("0", "genesis");}
    public static NodeTest node(String id) {return new NodeTest(id, "Node-" + id);}
}
