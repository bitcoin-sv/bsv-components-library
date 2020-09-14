package com.nchain.jcl.base.domain.bean.base;

import com.nchain.jcl.base.domain.api.base.AbstractBlock;
import com.nchain.jcl.base.domain.api.base.BlockHeader;

import lombok.Getter;

/**
 * @author Steve Shadders
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */

public abstract class AbstractBlockBean implements AbstractBlock {
    @Getter private BlockHeader header;
}
