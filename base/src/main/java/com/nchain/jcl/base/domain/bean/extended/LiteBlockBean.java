package com.nchain.jcl.base.domain.bean.extended;

import com.nchain.jcl.base.domain.api.base.BlockHeader;
import com.nchain.jcl.base.domain.api.extended.BlockMeta;
import com.nchain.jcl.base.domain.api.extended.ChainInfo;
import com.nchain.jcl.base.domain.api.extended.LiteBlock;
import com.nchain.jcl.base.domain.bean.base.AbstractBlockBean;
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper;
import lombok.Builder;
import lombok.Value;

/**
 * @author Steve Shadders
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Implementation of a LiteBlock
 * This class is IMMUTABLE. Instances can be created by using a Lombok generated Builder.
 */
@Value
public class LiteBlockBean extends AbstractBlockBean implements LiteBlock {
    private BlockMeta blockMeta;
    private ChainInfo chainInfo;

    @Builder(toBuilder = true)
    public LiteBlockBean(Long sizeInBytes, BlockHeader header, BlockMeta blockMeta, ChainInfo chainInfo) {
        super(sizeInBytes, header);
        this.blockMeta = blockMeta;
        this.chainInfo = chainInfo;
    }
}
