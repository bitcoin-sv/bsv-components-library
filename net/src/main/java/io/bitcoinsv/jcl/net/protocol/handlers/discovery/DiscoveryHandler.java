package io.bitcoinsv.jcl.net.protocol.handlers.discovery;


import io.bitcoinsv.jcl.tools.handlers.Handler;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Operations provided by a Discovery Handler.
 * The Discovery Handler takes care of implementing the Node Discovery Algorithm.
 */
public interface DiscoveryHandler extends Handler {
    String HANDLER_ID = "Discovery-Handler";

    @Override
    default String getId() { return HANDLER_ID; }
}
