package com.nchain.jcl.base.domain.bean.extended;

import com.nchain.jcl.base.domain.api.base.BlockHeader;
import com.nchain.jcl.base.domain.api.extended.ChainInfo;
import com.nchain.jcl.base.domain.bean.BitcoinObjectImpl;
import com.nchain.jcl.base.domain.bean.BitcoinSerializableObjectImpl;
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

@Value
public class ChainInfoBean extends BitcoinSerializableObjectImpl implements ChainInfo {
    private BlockHeader header;
    private BigInteger chainWork;
    private int height;
    private long totalChainTxs;
    private long totalChainSize;

    @Builder(toBuilder = true)
    public ChainInfoBean(Long sizeInBytes, BlockHeader header, BigInteger chainWork,
                         Integer height, Long totalChainTxs, Long totalChainSize) {
        super(sizeInBytes);
        this.header = header;
        this.chainWork = chainWork;
        this.height = (height != null)? height : 0;
        this.totalChainTxs = (totalChainTxs != null) ? totalChainTxs : 0;
        this.totalChainSize = (totalChainSize != null)? totalChainSize : -1;
    }
}
