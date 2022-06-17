package io.bitcoinsv.jcl.net.network.events;

import com.google.common.base.Objects;

import java.net.InetAddress;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-07-09 12:57
 *
 * A Request to Whitelist a Peer
 */
public final class WhitelistPeerRequest extends P2PRequest {
    private final InetAddress address;

    /**
     * Constructor.
     * We assume that Whitelisting Peers is a rare event, so we accept a single Peer as parameter rather than a
     * Collection
     */
    public WhitelistPeerRequest(InetAddress address) {
        this.address = address;
    }

    public InetAddress getAddress() {
        return this.address;
    }


    @Override
    public String toString() {
        return "BlacklistPeerRequest(address=" + this.address + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) { return false; }
        WhitelistPeerRequest other = (WhitelistPeerRequest) obj;
        return Objects.equal(this.address, other.address);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), address);
    }

}
