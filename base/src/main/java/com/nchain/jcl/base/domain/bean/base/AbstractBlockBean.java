package com.nchain.jcl.base.domain.bean.base;

import com.nchain.jcl.base.domain.api.base.AbstractBlock;
import com.nchain.jcl.base.domain.api.base.BlockHeader;

import com.nchain.jcl.base.domain.bean.BitcoinHashableImpl;
import com.nchain.jcl.base.domain.bean.BitcoinObjectImpl;
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * @author Steve Shadders
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */

public abstract class AbstractBlockBean extends BitcoinObjectImpl implements AbstractBlock {
    @Getter protected Long sizeInBytes;
    @Getter protected BlockHeader header;

    public AbstractBlockBean(Long sizeInBytes, BlockHeader header) {
        this.sizeInBytes = sizeInBytes;
        this.header = header;
    }
}
