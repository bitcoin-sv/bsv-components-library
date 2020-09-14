package com.nchain.jcl.base.domain.bean.base;

import com.nchain.jcl.base.domain.api.base.FullBlock;
import com.nchain.jcl.base.domain.api.base.Tx;
import com.nchain.jcl.base.domain.api.extended.LiteBlock;
import com.nchain.jcl.base.domain.bean.extended.BlockMetaBean;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * @date 2020-09-14
 */
@Builder(toBuilder = true)
@AllArgsConstructor
@Value
public class FullBlockBean extends AbstractBlockBean implements FullBlock {
    private BlockMetaBean metaData;
    private List<Tx> transactions;

    @Override
    public LiteBlock asLiteBlock() {
        LiteBlock result = LiteBlock.builder()
                .blockHeader(this.header)
                .blockMeta(metaData)
                .build();
        return result;
    }
}
