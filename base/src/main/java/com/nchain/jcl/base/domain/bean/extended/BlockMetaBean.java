package com.nchain.jcl.base.domain.bean.extended;

import com.nchain.jcl.base.domain.api.extended.BlockMeta;
import com.nchain.jcl.base.domain.bean.BitcoinObjectImpl;
import com.nchain.jcl.base.domain.bean.BitcoinSerializableObjectImpl;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.SuperBuilder;

/**
 * @author Steve Shadders
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This class is THREAD-SAFE.
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class BlockMetaBean extends BitcoinSerializableObjectImpl implements BlockMeta {
    private int txCount;
    private long blockSize;

    /** USe "BlockMEta.builder()" instead */
    @Builder(toBuilder = true)
    public BlockMetaBean(Long sizeInBytes, Integer txCount, Long blockSize) {
        super(sizeInBytes);
        this.txCount = (txCount != null)? txCount : 0;
        this.blockSize = (blockSize != null)? blockSize : -1; // Default Value
    }
}
