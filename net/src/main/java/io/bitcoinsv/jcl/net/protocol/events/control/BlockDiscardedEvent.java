package io.bitcoinsv.jcl.net.protocol.events.control;


import io.bitcoinsv.jcl.net.network.events.P2PEvent;

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
public final class BlockDiscardedEvent extends P2PEvent {

    /** Definition of possible reasons why a block might be Discarded during Downloaded */
    public enum DiscardedReason {
        TIMEOUT // It takes too long to download the bock
    }

    private final String hash;
    private final DiscardedReason reason;

    public BlockDiscardedEvent(String hash, DiscardedReason reason) {
        this.hash = hash;
        this.reason = reason;
    }

    public String getHash()             { return this.hash; }
    public DiscardedReason getReason()  { return this.reason; }
    @Override
    public String toString() {
        return "BlockDiscardedEvent(hash=" + this.getHash() + ", reason=" + this.getReason() + ")";
    }
}
