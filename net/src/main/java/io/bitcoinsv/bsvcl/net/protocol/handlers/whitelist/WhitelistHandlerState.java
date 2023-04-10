package io.bitcoinsv.bsvcl.net.protocol.handlers.whitelist;


import com.google.common.base.Objects;
import io.bitcoinsv.bsvcl.tools.handlers.HandlerState;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This event stores the state of the Whitelist Handler at a point in time.*
 */
public final class WhitelistHandlerState extends HandlerState {
    private final Set<InetAddress> whitelistedHosts;

    /** Constructor */
    public WhitelistHandlerState(Set<InetAddress> whitelistedHosts) {
        this.whitelistedHosts = whitelistedHosts;
    }

    @Override
    public String toString() {
        return "Whitelist Status: [ "
                + whitelistedHosts.size() + " Addresses whitelisted ]";
    }

    public Set<InetAddress> getWhitelistedHosts() { return this.whitelistedHosts;}


    @Override
    public int hashCode() {
        return Objects.hashCode(whitelistedHosts);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) { return false; }
        if (obj == this) { return true; }
        if (obj.getClass() != getClass()) { return false; }
        WhitelistHandlerState other = (WhitelistHandlerState) obj;
        return  Objects.equal(this.whitelistedHosts, other.whitelistedHosts);
    }


    public WhitelistHandlerStateBuilder toBuilder() {
        return new WhitelistHandlerStateBuilder().whitelistedHosts(this.whitelistedHosts);
    }

    public static WhitelistHandlerStateBuilder builder() {
        return new WhitelistHandlerStateBuilder();
    }

    /**
     * Builder
     */
    public static class WhitelistHandlerStateBuilder {
        private Set<InetAddress> whitelistedHosts = new HashSet<>();

        WhitelistHandlerStateBuilder() {}

        public WhitelistHandlerStateBuilder whitelistedHosts(Set<InetAddress> whitelistedHosts) {
            this.whitelistedHosts = whitelistedHosts;
            return this;
        }

        public WhitelistHandlerStateBuilder addHost(InetAddress host) {
            this.whitelistedHosts.add(host);
            return this;
        }

        public WhitelistHandlerState build() {
            return new WhitelistHandlerState(whitelistedHosts);
        }
    }
}
