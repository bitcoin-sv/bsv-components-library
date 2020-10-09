package com.nchain.jcl.base.domain.bean.extended;

import com.nchain.jcl.base.domain.api.base.Tx;
import com.nchain.jcl.base.domain.api.extended.CoinbaseInfo;
import com.nchain.jcl.base.domain.bean.BitcoinObjectImpl;
import lombok.Builder;
import lombok.Value;

/**
 * @author Steve Shadders
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This class is THREAD-SAFE.
 */
@Builder(toBuilder = true)
@Value
public class CoinbaseInfoBean implements CoinbaseInfo {
    private Tx coinbase;
    private Object merkleProof;
    private Object txCountProof;
}
