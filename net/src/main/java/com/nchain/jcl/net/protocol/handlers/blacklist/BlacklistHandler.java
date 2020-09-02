package com.nchain.jcl.net.protocol.handlers.blacklist;


import com.nchain.jcl.base.tools.handlers.Handler;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-07-15 14:53
 *
 * Blacklist Handler.
 * This handler takes care of blacklisting those Hosts tat missbehave based on some criteria.
 */
public interface BlacklistHandler extends Handler {
    String HANDLER_ID = "Blacklist-Handler";

    @Override
    default String getId() { return HANDLER_ID; }

}
