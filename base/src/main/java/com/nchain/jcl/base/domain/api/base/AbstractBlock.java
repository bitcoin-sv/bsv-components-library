package com.nchain.jcl.base.domain.api.base;

import com.nchain.jcl.base.domain.api.BitcoinObject;
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper;

/**
 * @author Steve Shadders
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A common interface for a Block. Any Class that represents a block will implements this interface (at least).
 * this interface only provides info about the Block header, future subclasses might add more info to it.
 */
public interface AbstractBlock extends BitcoinObject {
    BlockHeader getHeader();
    default Sha256Wrapper getHash() { return getHeader().getHash();}
}
