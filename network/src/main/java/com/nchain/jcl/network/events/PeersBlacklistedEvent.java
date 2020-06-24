package com.nchain.jcl.network.events;

import com.nchain.jcl.tools.events.Event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.net.InetAddress;
import java.util.Map;

/**
 * @author i.bfernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-23 10:23
 *
 * Event triggered when a set of Nodes is blacklisted. (Ahole IP address is blacklisted,
 * no mater the port number)
 */
@Value
@Builder(toBuilder = true)
@AllArgsConstructor
public class PeersBlacklistedEvent extends Event {
    /** Definition of the Reason of the Blacklist */
    public enum BlacklistedReason {
        CONNECTION_REJECTED
    }
    private Map<InetAddress, BlacklistedReason> inetAddress;
}
