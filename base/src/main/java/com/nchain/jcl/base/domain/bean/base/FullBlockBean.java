package com.nchain.jcl.base.domain.bean.base;

import com.nchain.jcl.base.domain.api.base.BlockHeader;
import com.nchain.jcl.base.domain.api.base.FullBlock;
import com.nchain.jcl.base.domain.api.base.Tx;
import com.nchain.jcl.base.domain.api.extended.BlockMeta;
import com.nchain.jcl.base.domain.api.extended.LiteBlock;
import com.nchain.jcl.base.domain.bean.extended.BlockMetaBean;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * @date 2020-09-14
 */
@Deprecated
@Value
public class FullBlockBean extends AbstractBlockBean implements FullBlock {
    private BlockMeta metaData;
    private List<Tx> transactions;

    @Builder(toBuilder = true)
    public FullBlockBean(Long sizeInBytes, BlockHeader header, BlockMeta metaData, List<Tx> transactions) {
        super(sizeInBytes, header);
        if (metaData == null) {
            BlockMetaBean.BlockMetaBeanBuilder metaBuilder = BlockMeta.builder()
                    .txCount(transactions.size());
            if (sizeInBytes != null) metaBuilder.blockSize(sizeInBytes);
            this.metaData = metaBuilder.build();
        } else this.metaData = metaData;
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
