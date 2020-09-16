package com.nchain.jcl.base.domain.bean.extended;

import com.nchain.jcl.base.domain.api.base.BlockHeader;
import com.nchain.jcl.base.domain.api.extended.ChainInfo;
import com.nchain.jcl.base.domain.bean.BitcoinObjectImpl;
import lombok.Builder;
import lombok.Value;

import java.math.BigInteger;

/**
 * @author Steve Shadders
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An implementation of a ChainInfo
 * This class is IMMUTABLE. Instances can be created by using a Lombok generated Builder.
 */
@Builder(toBuilder = true)
@Value
public class ChainInfoBean extends BitcoinObjectImpl implements ChainInfo {
    private BlockHeader header;
    private BigInteger chainWork;
    private int height;
    private long totalChainTxs;
    @Builder.Default
    private long getTotalChainSize = -1;
}
