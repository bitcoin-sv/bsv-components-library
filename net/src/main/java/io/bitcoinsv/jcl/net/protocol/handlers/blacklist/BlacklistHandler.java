/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.protocol.handlers.blacklist;


import io.bitcoinsv.jcl.tools.handlers.Handler;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Blacklist Handler.
 * This handler takes care of blacklisting those Hosts tat missbehave based on some criteria.
 */
public interface BlacklistHandler extends Handler {
    String HANDLER_ID = "Blacklist-Handler";

    @Override
    default String getId() { return HANDLER_ID; }

}
