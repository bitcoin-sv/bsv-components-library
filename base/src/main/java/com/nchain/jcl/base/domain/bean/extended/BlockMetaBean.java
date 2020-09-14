package com.nchain.jcl.base.domain.bean.extended;

import com.nchain.jcl.base.domain.api.extended.BlockMeta;
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
public class BlockMetaBean implements BlockMeta {
    private int txCount;
    private long blockSize;
}
