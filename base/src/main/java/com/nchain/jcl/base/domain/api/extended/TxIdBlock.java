package com.nchain.jcl.base.domain.api.extended;

import com.nchain.jcl.base.domain.api.base.AbstractBlock;
import com.nchain.jcl.base.domain.bean.extended.TxIdBlockBean;
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper;

import java.util.List;

/**
 * @author Steve Shadders
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */
public interface TxIdBlock extends AbstractBlock {
    List<Sha256Wrapper> getTxids();

    // Convenience method to get a reference to the Builder
    static TxIdBlockBean.TxIdBlockBeanBuilder builder() { return TxIdBlockBean.builder(); }
}
