package io.bitcoinsv.jcl.net.network.events;

import com.google.common.base.Objects;

import java.net.InetAddress;
import java.time.Duration;
import java.util.Optional;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-07-09 12:57
 *
 * A Request to Blacklist a Peer
 */
public final class BlacklistPeerRequest extends P2PRequest {
    private final InetAddress address;
    private PeersBlacklistedEvent.BlacklistReason reason;
    private Optional<Duration> duration;

    /**
     * Constructor. The Duration specified will override the default one.
     */
    public BlacklistPeerRequest(InetAddress address, PeersBlacklistedEvent.BlacklistReason reason, Optional<Duration> duration) {
        this.address = address;
        this.reason = reason;
        this.duration = duration;
    }

    public InetAddress getAddress() {
        return this.address;
    }

    public PeersBlacklistedEvent.BlacklistReason getReason() {
        return reason;
    }

    public Optional<Duration> getDuration() {
        return this.duration;
    }

    @Override
    public String toString() {
        return "BlacklistPeerRequest(address=" + this.address + ", reason=" + reason + ", duration=" + this.duration + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) { return false; }
        BlacklistPeerRequest other = (BlacklistPeerRequest) obj;
        return Objects.equal(this.address, other.address)
                && Objects.equal(this.reason, other.reason)
                && Objects.equal(this.duration, other.duration);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), address, reason, duration);
    }


}
