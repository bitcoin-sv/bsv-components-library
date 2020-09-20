package com.nchain.jcl.base.domain.bean.extended;

import com.nchain.jcl.base.domain.api.extended.BlockMeta;
import com.nchain.jcl.base.domain.bean.BitcoinObjectImpl;
import lombok.Builder;
import lombok.Value;

/**
 * @author Steve Shadders
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An implementation of a BlockMeta
 * This class is IMMUTABLE. Instances can be created by using a Lombok generated Builder.
 */
@Builder(toBuilder = true)
@Value
public class BlockMetaBean extends BitcoinObjectImpl implements BlockMeta {
    private int txCount;
    @Builder.Default
    private long blockSize = -1;
}
