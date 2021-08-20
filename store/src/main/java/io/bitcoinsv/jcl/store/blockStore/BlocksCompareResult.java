/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.store.blockStore;


import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.HeaderReadOnly;
import io.bitcoinsv.bitcoinjsv.core.Sha256Hash;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This class stores the result of the comparison of 2 blocks, regarding the Txs they contain in common, Txs only
 * contained in one of them, etc.
 */
public final class BlocksCompareResult {
    private final HeaderReadOnly blockA;
    private final HeaderReadOnly blockB;
    private final Iterable<Sha256Hash> txsInCommonIt;
    private final Iterable<Sha256Hash> txsOnlyInA;
    private final Iterable<Sha256Hash> txsOnlyInB;

    BlocksCompareResult(HeaderReadOnly blockA, HeaderReadOnly blockB, Iterable<Sha256Hash> txsInCommonIt, Iterable<Sha256Hash> txsOnlyInA, Iterable<Sha256Hash> txsOnlyInB) {
        this.blockA = blockA;
        this.blockB = blockB;
        this.txsInCommonIt = txsInCommonIt;
        this.txsOnlyInA = txsOnlyInA;
        this.txsOnlyInB = txsOnlyInB;
    }

    public HeaderReadOnly getBlockA()               { return this.blockA; }
    public HeaderReadOnly getBlockB()               { return this.blockB; }
    public Iterable<Sha256Hash> getTxsInCommonIt()  { return this.txsInCommonIt; }
    public Iterable<Sha256Hash> getTxsOnlyInA()     { return this.txsOnlyInA; }
    public Iterable<Sha256Hash> getTxsOnlyInB()     { return this.txsOnlyInB; }

    public String toString() {
        return "BlocksCompareResult(blockA=" + this.getBlockA() + ", blockB=" + this.getBlockB() + ", txsInCommonIt=" + this.getTxsInCommonIt() + ", txsOnlyInA=" + this.getTxsOnlyInA() + ", txsOnlyInB=" + this.getTxsOnlyInB() + ")";
    }

    public static BlocksCompareResultBuilder builder() {
        return new BlocksCompareResultBuilder();
    }

    /**
     * Builder
     */
    public static class BlocksCompareResultBuilder {
        private HeaderReadOnly blockA;
        private HeaderReadOnly blockB;
        private Iterable<Sha256Hash> txsInCommonIt;
        private Iterable<Sha256Hash> txsOnlyInA;
        private Iterable<Sha256Hash> txsOnlyInB;

        BlocksCompareResultBuilder() {
        }

        public BlocksCompareResult.BlocksCompareResultBuilder blockA(HeaderReadOnly blockA) {
            this.blockA = blockA;
            return this;
        }

        public BlocksCompareResult.BlocksCompareResultBuilder blockB(HeaderReadOnly blockB) {
            this.blockB = blockB;
            return this;
        }

        public BlocksCompareResult.BlocksCompareResultBuilder txsInCommonIt(Iterable<Sha256Hash> txsInCommonIt) {
            this.txsInCommonIt = txsInCommonIt;
            return this;
        }

        public BlocksCompareResult.BlocksCompareResultBuilder txsOnlyInA(Iterable<Sha256Hash> txsOnlyInA) {
            this.txsOnlyInA = txsOnlyInA;
            return this;
        }

        public BlocksCompareResult.BlocksCompareResultBuilder txsOnlyInB(Iterable<Sha256Hash> txsOnlyInB) {
            this.txsOnlyInB = txsOnlyInB;
            return this;
        }

        public BlocksCompareResult build() {
            return new BlocksCompareResult(blockA, blockB, txsInCommonIt, txsOnlyInA, txsOnlyInB);
        }

    }
}
