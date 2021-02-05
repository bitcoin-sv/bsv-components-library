package com.nchain.jcl.store.blockStore;


import io.bitcoinj.bitcoin.api.base.HeaderReadOnly;
import io.bitcoinj.core.Sha256Hash;
import lombok.Builder;
import lombok.Value;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This class stores the result of the comparison of 2 blocks, regarding the Txs they contain in common, Txs only
 * contained in one of them, etc.
 */
@Value
@Builder
public class BlocksCompareResult {
    private HeaderReadOnly blockA;
    private HeaderReadOnly blockB;
    private Iterable<Sha256Hash> txsInCommonIt;
    private Iterable<Sha256Hash> txsOnlyInA;
    private Iterable<Sha256Hash> txsOnlyInB;

}
