package com.nchain.jcl.base.domain.bean.extended;

import com.nchain.jcl.base.tools.crypto.Sha256Wrapper;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * @date 2020-09-15
 *
 * <p>The transaction confirmations for a transaction.<p>
 * <p>A transaction can be confirmed in multiple blocks but only in one path from a block to the genesis block.</p>
 */

public class TxConfirmations {
    private final Set<Sha256Wrapper> blockHashes;

    public TxConfirmations() {
        blockHashes = Collections.emptySet();
    }

    public TxConfirmations(Set<Sha256Wrapper> blockHashes) {
        this.blockHashes = blockHashes;
    }

    public TxConfirmations(Set<Sha256Wrapper> hashes1, Set<Sha256Wrapper> hashses2) {
        Set<Sha256Wrapper> result = new HashSet<>(hashes1);
        result.addAll(hashses2);
        this.blockHashes = result;
    }

    public TxConfirmations addBlock(Sha256Wrapper blockHash) {
        return new TxConfirmations(blockHashes, Collections.singleton(blockHash));
    }

    public Set<Sha256Wrapper> getBlockHashes() {
        return blockHashes;
    }
}
