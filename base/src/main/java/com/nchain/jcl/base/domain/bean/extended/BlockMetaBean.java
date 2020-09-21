package com.nchain.jcl.base.domain.bean.extended;

import com.nchain.jcl.base.domain.api.extended.BlockMeta;
import com.nchain.jcl.base.domain.bean.BitcoinObjectImpl;
import com.nchain.jcl.base.domain.bean.BitcoinSerializableObjectImpl;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.SuperBuilder;

/**
 * @author Steve Shadders
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An implementation of a BlockMeta
 * This class is IMMUTABLE. Instances can be created by using a Lombok generated Builder.
 */
@Value
public class BlockMetaBean extends BitcoinSerializableObjectImpl implements BlockMeta {
    private int txCount;
    private long blockSize;

    @Builder(toBuilder = true)
    public BlockMetaBean(Long sizeInBytes, Integer txCount, Long blockSize) {
        super(sizeInBytes);
        this.txCount = (txCount != null)? txCount : 0;
        this.blockSize = (blockSize != null)? blockSize : -1; // Default Value
    }
}
