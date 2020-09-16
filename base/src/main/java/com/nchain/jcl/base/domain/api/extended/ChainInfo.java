package com.nchain.jcl.base.domain.api.extended;

import com.nchain.jcl.base.domain.api.BitcoinObject;
import com.nchain.jcl.base.domain.api.base.BlockHeader;
import com.nchain.jcl.base.domain.bean.extended.ChainInfoBean;

import java.math.BigInteger;

/**
 * @author Steve Shadders
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This interface provides additional information about a block header's position within a blockchain.
 */
public interface ChainInfo extends BitcoinObject {
    /** Get the {@link BlockHeader}.
     *
     * @return The Header for which this object provides more information.
     */
    BlockHeader getHeader();

    /** <p>Get the amount of work performed on the chain to this point.</p>
     *
     * <p>The blockchain accumulates proof of work as it is extended. Each block that is added to the blockchain increases
     * the amount of work that has been performed. The work performed at a particular block is the sum of the work
     * performed on that block and all of its previous blocks.</p>
     *
     * @return The amount of work performed on the tree to this point.
     */
    BigInteger getChainWork();

    /** <p>Get the height of the header in the tree of block headers.</p>
     *
     * <p>The height of a header is defined inductively:
     * <ul>
     *   <li>the height of the Genesis header is zero</li>
     *   <li>the height of any other header is the height of the previous header plus one</li>
     * </ul></p>
     *
     * <p>The height can always be calculated for a particular header however this can be an intensive process so
     * implementations of this interface usually store this information.</p>
     *
     * @return The height of the header (always greater or equal to zero).
     */
    int getHeight();

    /** Get the total number of transactions contained within the blocks from the Genesis block to this block.
     *
     * @return the total number of transactions in the chain so far.
     */
    long getTotalChainTxs();

    /** Get the total size of the chain up to and including this block.
     *
     * The total size of the chain is the sum of the sizes of every block from this block to the root.
     *
     * @return the total size of the chain in bytes. If unknown, returns -1.
     */
    default long getTotalChainSize() { return -1; }

    // Convenience methods to get a reference to the Builder:
    static ChainInfoBean.ChainInfoBeanBuilder builder() { return ChainInfoBean.builder();}
    default ChainInfoBean.ChainInfoBeanBuilder toBuilder() { return ((ChainInfoBean) this).toBuilder();}
}
