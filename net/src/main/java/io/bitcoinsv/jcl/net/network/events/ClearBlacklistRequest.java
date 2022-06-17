package io.bitcoinsv.jcl.net.network.events;

import java.util.Objects;

/**
 * @author j.pomer@nchain.com
 * Copyright (c) 2018-2022 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * <p>
 * A Request to Clear the list of Blacklisted Peers
 */
public final class ClearBlacklistRequest extends P2PRequest {
    @Override
    public String toString() {
        return "ClearBlacklistPeersRequest()";
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode());
    }
}