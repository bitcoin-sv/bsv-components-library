package io.bitcoinsv.jcl.net.protocol.handlers.whitelist;


import io.bitcoinsv.jcl.tools.handlers.Handler;
import java.util.Set;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Whitelist Handler.
 * This handler takes care of whitelisting those Peers that we want to hae specicial privileges
 */
public interface WhitelistHandler extends Handler {
    String HANDLER_ID = "Whitelist";

    @Override
    default String getId() { return HANDLER_ID; }

    /**
     * Returns the list of Hosts currently whitelisted
     */
    Set<WhitelistView> getWhitelistedHosts();
}
