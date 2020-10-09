package com.nchain.jcl.net.network.events;

import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.base.tools.events.Event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
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
