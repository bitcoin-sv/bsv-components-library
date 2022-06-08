package io.bitcoinsv.jcl.net.protocol.handlers.blacklist;


import io.bitcoinsv.jcl.tools.handlers.Handler;

import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Blacklist Handler.
 * This handler takes care of blacklisting those Hosts tat missbehave based on some criteria.
 */
public interface BlacklistHandler extends Handler {
    String HANDLER_ID = "Blacklist";

    @Override
    default String getId() { return HANDLER_ID; }
    List<BlacklistView> getBlacklistedHosts();
}
