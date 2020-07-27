package com.nchain.jcl.network.events;

import com.nchain.jcl.network.PeerAddress;
import com.nchain.jcl.tools.events.Event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2009-2010 Satoshi Nakamoto
 * Copyright (c) 2009-2016 The Bitcoin Core developers
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-23 10:15
 *
 * An Event Triggered when a connection to a Peer has been Rejected. So the connectin never took place in
 * the first place.
 */
@Value
@Builder(toBuilder = true)
@AllArgsConstructor
public class PeerRejectedEvent extends Event {
    /** Different Reasons why the conneciton has been rejected */
    public enum RejectedReason {
        INTERNAL_ERROR,
        TIMEOUT
    }
    private PeerAddress peerAddress;
    private RejectedReason reason;
    private String detail; // might be null

    @Override
    public String toString() {
        return "Event[PerRejected]: " + peerAddress.toString() + " : " + reason + " : " + ((detail != null)? detail : " no reason specified");
    }
}
