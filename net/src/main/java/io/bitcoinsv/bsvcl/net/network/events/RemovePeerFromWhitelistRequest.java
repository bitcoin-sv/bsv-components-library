package io.bitcoinsv.bsvcl.net.network.events;

import com.google.common.base.Objects;

import java.net.InetAddress;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2022 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * <p>
 * A Request to Remove a Peer from the Whitelist
 */
public final class RemovePeerFromWhitelistRequest extends P2PRequest {
    private final InetAddress address;

    public RemovePeerFromWhitelistRequest(InetAddress address) {
        this.address = address;
    }

    public InetAddress getAddress() {
        return this.address;
    }

    @Override
    public String toString() {
        return "RemovePeerFromWhitelistRequest(peerAddress=" + address + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) { return false; }
        RemovePeerFromWhitelistRequest other = (RemovePeerFromWhitelistRequest) obj;
        return Objects.equal(this.address, other.address);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), address);
    }
}