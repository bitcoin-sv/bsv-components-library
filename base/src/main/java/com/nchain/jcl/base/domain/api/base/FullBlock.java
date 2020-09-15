package com.nchain.jcl.base.domain.api.base;

import com.nchain.jcl.base.domain.api.extended.LiteBlock;
import com.nchain.jcl.base.domain.bean.base.FullBlockBean;

import java.util.List;

/**
 * @author Steve Shadders
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A full block consists of the {@BlockHeader} and the full content of all of the transactions needed to verify the
 * correctness of the Header.
 *
 * Don't use this. No matter how much RAM or virtual memory you add, full blocks will always get bigger.
 *
 */
@Deprecated
public interface FullBlock extends AbstractBlock {
    List<Tx> getTransactions();
    LiteBlock asLiteBlock();

    // Convenience methods to get a reference to the Builder
    static FullBlockBean.FullBlockBeanBuilder builder() { return FullBlockBean.builder(); }
    default FullBlockBean.FullBlockBeanBuilder toBuilder() { return ((FullBlock) this).toBuilder();}
}
