package com.nchain.jcl.base.domain.bean.base;

import com.nchain.jcl.base.domain.api.base.BlockHeader;
import com.nchain.jcl.base.domain.api.base.FullBlock;
import com.nchain.jcl.base.domain.api.base.Tx;
import com.nchain.jcl.base.domain.api.extended.BlockMeta;
import com.nchain.jcl.base.domain.api.extended.LiteBlock;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * @date 2020-09-14
 */
@Value
public class FullBlockBean extends AbstractBlockBean implements FullBlock {
    private BlockMeta metaData;
    private List<Tx> transactions;

    @Builder(toBuilder = true)
    public FullBlockBean(BlockHeader header, BlockMeta metaData, List<Tx> transactions) {
        this.header = header;
        this.metaData = metaData;
        this.transactions = transactions;
    }

    @Override
    public LiteBlock asLiteBlock() {
        LiteBlock result = LiteBlock.builder()
                .header(this.header)
                .blockMeta(metaData)
                .build();
        return result;
    }
}
