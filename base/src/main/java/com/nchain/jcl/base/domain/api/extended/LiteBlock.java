package com.nchain.jcl.base.domain.api.extended;

import com.nchain.jcl.base.domain.api.base.AbstractBlock;
import com.nchain.jcl.base.domain.bean.extended.LiteBlockBean;

/**
 * @author Steve Shadders
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A LiteBlock is the representation of a Block that provides info abut the Block Headers (since it extends the
 * AbstractBlock interface), but also contains references to BlockMeta and ChainInfo
 *
 * @see BlockMeta
 * @see ChainInfo
 */
public interface LiteBlock extends AbstractBlock {
    BlockMeta getBlockMeta();
    ChainInfo getChainInfo();

    // Convenience methods to get a reference to the Builder
    static LiteBlockBean.LiteBlockBeanBuilder builder() { return LiteBlockBean.builder();}
    static LiteBlockBean.LiteBlockBeanBuilder builder(byte[] bytes) { return LiteBlockBean.build(bytes);}
    static LiteBlockBean.LiteBlockBeanBuilder builder(String hex) { return LiteBlockBean.build(hex);}
    default LiteBlockBean.LiteBlockBeanBuilder toBuilder() { return ((LiteBlockBean) this).toBuilder();}


}
