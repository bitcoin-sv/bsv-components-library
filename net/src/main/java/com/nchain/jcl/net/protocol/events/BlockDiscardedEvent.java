package com.nchain.jcl.net.protocol.events;


import com.nchain.jcl.tools.events.Event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-07-27 12:11
 *
 * An Event triggered when a Block, which has been requested to download, is discarded for any reason. This event
 * provides info about the Block (Hash) and the reason why the Block has been discarded
 * A Block discarded might be attempted again after some time, depending on configuration.
 */
@Value
@Builder(toBuilder = true)
@AllArgsConstructor
public class BlockDiscardedEvent extends Event {

    /** Definition of possible reasons why a block might be Discarded during Downloaded */
    public enum DiscardedReason {
        TIMEOUT // It takes too long to download the bock
    }

    private String hash;
    private DiscardedReason reason;
}
