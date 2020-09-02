package com.nchain.jcl.net.protocol.handlers.discovery;

import com.nchain.jcl.base.tools.handlers.Handler;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-07-13 12:08
 *
 * Operations provided by a Discovery Handler.
 * The Discovery Handler takes care of implementing the Node Discovery Algorithm.
 */
public interface DiscoveryHandler extends Handler {
    String HANDLER_ID = "Discovery-Handler";

    @Override
    default String getId() { return HANDLER_ID; }
}
