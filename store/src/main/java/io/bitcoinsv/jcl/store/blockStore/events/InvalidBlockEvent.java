/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.store.blockStore.events;

import io.bitcoinsv.bitcoinjsv.core.Sha256Hash;

/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2021 nChain Ltd
 * @date 23/02/2021
 *
 * An event triggered when an invalid block has been detected
 */
public final class InvalidBlockEvent extends BlockStoreEvent {
    private Sha256Hash blockHash;
    private String reason;

    public InvalidBlockEvent(Sha256Hash blockHash, String reason) {
        this.blockHash = blockHash;
        this.reason = reason;
    }

    public Sha256Hash getBlockHash() {
        return this.blockHash;
    }

    public String getReason() {return this.reason;}

    @Override
    public String toString() {
        return "InvalidBlockEvent(blockHash=" + this.getBlockHash() + ", " + "reason= " + reason + ")";
    }
}
