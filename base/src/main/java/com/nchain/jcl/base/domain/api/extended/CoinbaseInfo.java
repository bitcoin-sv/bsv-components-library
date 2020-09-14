package com.nchain.jcl.base.domain.api.extended;

import com.nchain.jcl.base.domain.api.base.Tx;

/**
 * @author Steve Shadders
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */
public interface CoinbaseInfo {
    Tx getCoinbase();
    Object getMerkleProof();
    Object getTxCountProof();
}
