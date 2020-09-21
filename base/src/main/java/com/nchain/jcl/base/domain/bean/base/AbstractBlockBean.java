package com.nchain.jcl.base.domain.bean.base;

import com.nchain.jcl.base.domain.api.base.AbstractBlock;
import com.nchain.jcl.base.domain.api.base.BlockHeader;
import com.nchain.jcl.base.domain.bean.BitcoinSerializableObjectImpl;

import lombok.Builder;
import lombok.Getter;


/**
 * @author Steve Shadders
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */

public abstract class AbstractBlockBean extends BitcoinSerializableObjectImpl implements AbstractBlock {
    @Getter protected BlockHeader header;

    public AbstractBlockBean(Long sizeInBytes, BlockHeader header) {
        super(sizeInBytes);
        this.header = header;
    }
}
