package com.nchain.jcl.store.blockStore;

import com.nchain.jcl.base.domain.api.base.BlockHeader;
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper;
import lombok.Builder;
import lombok.Value;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This class stores the result of the comparison of 2 blocks, regarding the Txs they contain in common, Txs only
 * contained in one of them, etc.
 */
@Value
@Builder
public class BlocksCompareResult {
    private BlockHeader blockA;
    private BlockHeader blockB;
    private Iterable<Sha256Wrapper> txsInCommonIt;
    private Iterable<Sha256Wrapper> txsOnlyInA;
    private Iterable<Sha256Wrapper> txsOnlyInB;

}
