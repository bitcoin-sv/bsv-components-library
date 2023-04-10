package io.bitcoinsv.bsvcl.net.protocol.handlers.blacklist;


import com.google.common.base.Objects;
import io.bitcoinsv.bsvcl.net.network.events.PeersBlacklistedEvent;
import io.bitcoinsv.bsvcl.tools.handlers.HandlerState;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This event stores the state of the Blacklist Handler at a point in time.
 * The Blacklist Handler takes care of Blacklisting (and whitelisting) Peers when some conditions are
 * met: they failed during the handshake, or broken the timeout specified by the PingPong Handler, etc.
 *
 * NOTE: We do NOT store the list of Hosts blacklisted here since that list might be too big. If you want it, ask
 * blacklistHandlerImpl.
 */
public final class BlacklistHandlerState extends HandlerState {
    private final long numTotalBlacklisted;
    // A map containing the number of Host blacklisted grouped by Reason
    private  Map<PeersBlacklistedEvent.BlacklistReason, Integer> blacklistedReasons = new ConcurrentHashMap<>();

    public BlacklistHandlerState(long numTotalBlacklisted, Map<PeersBlacklistedEvent.BlacklistReason, Integer> blacklistedReasons) {
        this.numTotalBlacklisted = numTotalBlacklisted;
        if (blacklistedReasons != null)
            this.blacklistedReasons = blacklistedReasons;
    }

    @Override
    public String toString() {
        return "Blacklist Status: [ "
                + numTotalBlacklisted + " Addresses blacklisted ]"
                + " count: " + blacklistedReasons;
    }

    public long getNumTotalBlacklisted() {
        return this.numTotalBlacklisted;
    }

    public Map<PeersBlacklistedEvent.BlacklistReason, Integer> getBlacklistedReasons() {
        return this.blacklistedReasons;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(numTotalBlacklisted, blacklistedReasons);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) { return false; }
        if (obj == this) { return true; }
        if (obj.getClass() != getClass()) { return false; }
        BlacklistHandlerState other = (BlacklistHandlerState) obj;
        return Objects.equal(this.numTotalBlacklisted, other.numTotalBlacklisted)
                && Objects.equal(this.blacklistedReasons, other.blacklistedReasons);
    }


    public BlacklistHandlerStateBuilder toBuilder() {
        return new BlacklistHandlerStateBuilder().numTotalBlacklisted(this.numTotalBlacklisted).blacklistedReasons(this.blacklistedReasons);
    }

    public static BlacklistHandlerStateBuilder builder() {
        return new BlacklistHandlerStateBuilder();
    }

    /**
     * Builder
     */
    public static class BlacklistHandlerStateBuilder {
        private long numTotalBlacklisted;
        private Map<PeersBlacklistedEvent.BlacklistReason, Integer> blacklistedReasons = new HashMap<>();

        BlacklistHandlerStateBuilder() {}

        public BlacklistHandlerState.BlacklistHandlerStateBuilder numTotalBlacklisted(long numTotalBlacklisted) {
            this.numTotalBlacklisted = numTotalBlacklisted;
            return this;
        }

        public BlacklistHandlerState.BlacklistHandlerStateBuilder blacklistedReasons(Map<PeersBlacklistedEvent.BlacklistReason, Integer> blacklistedReasons) {
            this.blacklistedReasons = blacklistedReasons;
            return this;
        }

        public BlacklistHandlerState build() {
            return new BlacklistHandlerState(numTotalBlacklisted, blacklistedReasons);
        }
    }
}
