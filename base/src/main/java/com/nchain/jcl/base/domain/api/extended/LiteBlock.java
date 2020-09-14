package com.nchain.jcl.base.domain.api.extended;

import com.nchain.jcl.base.domain.api.base.AbstractBlock;

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
}
