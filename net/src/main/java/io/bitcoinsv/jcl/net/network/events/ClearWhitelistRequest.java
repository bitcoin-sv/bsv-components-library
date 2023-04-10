package io.bitcoinsv.jcl.net.network.events;

import java.util.Objects;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2022 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * <p>
 * A Request to Clear the list of whitelisted peers
 */
public final class ClearWhitelistRequest extends P2PRequest {
    @Override
    public String toString() {
        return "ClearWhitelistRequest()";
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