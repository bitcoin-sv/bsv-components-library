package io.bitcoinsv.jcl.net.network.events;

import com.google.common.base.Objects;

import java.net.InetAddress;

/**
 * @author j.pomer@nchain.com
 * Copyright (c) 2018-2022 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * <p>
 * A Request to Whitelist a Peer
 */
public final class WhitelistPeerRequest extends P2PRequest {
    private final InetAddress address;

    public WhitelistPeerRequest(InetAddress address) {
        this.address = address;
    }

    public InetAddress getAddress() {
        return this.address;
    }

    @Override
    public String toString() {
        return "WhitelistPeerRequest(peerAddress=" + address + ")";
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