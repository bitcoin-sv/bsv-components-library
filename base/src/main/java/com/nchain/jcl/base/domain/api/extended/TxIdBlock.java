package com.nchain.jcl.base.domain.api.extended;

import com.nchain.jcl.base.domain.api.base.AbstractBlock;
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper;

import java.util.List;

/**
 * @author Steve Shadders
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */
public interface TxIdBlock extends AbstractBlock {
    List<Sha256Wrapper> getTxids();
}
