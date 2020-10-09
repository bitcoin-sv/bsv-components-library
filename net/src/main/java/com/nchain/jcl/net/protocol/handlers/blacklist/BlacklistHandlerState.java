package com.nchain.jcl.net.protocol.handlers.blacklist;

import com.nchain.jcl.base.tools.handlers.HandlerState;
import com.nchain.jcl.net.network.events.PeersBlacklistedEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This event stores the state of the Blacklist Handler at a point in time.
 * The Blacklist Handler takes care of Blacklisting (and whitelisting) Peers when some conditions are
 * met: they failed during the handshake, or broken the timeout specified by the PingPong Handler, etc.
 *
 */
@Value
@Builder(toBuilder = true)
@AllArgsConstructor
public class BlacklistHandlerState extends HandlerState {
    private long numTotalBlacklisted;
    @Builder.Default
    private Map<PeersBlacklistedEvent.BlacklistReason, Integer> blacklistedReasons = new ConcurrentHashMap<>();

    @Override
    public String toString() {
        return "Blacklist Status: [ "
                + numTotalBlacklisted + " Addresses blacklisted ]"
                + " count: " + blacklistedReasons;
    }
}
