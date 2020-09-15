package com.nchain.jcl.base.domain.api.base;

import com.nchain.jcl.base.core.JCLBase;
import com.nchain.jcl.base.domain.api.BitcoinObject;
import com.nchain.jcl.base.domain.bean.base.BlockHeaderBean;
import com.nchain.jcl.base.exception.VerificationException;

import com.nchain.jcl.base.tools.bytes.ByteTools;
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper;
import java.math.BigInteger;


/**
 * @author Steve Shadders
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Operations provided by a Block Header, retuning different types of info.
 */
public interface BlockHeader extends BitcoinObject, HashableObject {

    // Block Header fields:
    long getVersion();
    Sha256Wrapper getPrevBlockHash();
    Sha256Wrapper getMerkleRoot();
    long getTime();
    long getDifficultyTarget();
    long getNonce();

    // Convenience methods to get a reference to a Builder, so we can build instances of BlockHeader.
    static BlockHeaderBean.BlockHeaderBeanBuilder builder() { return BlockHeaderBean.builder();}
    static BlockHeaderBean.BlockHeaderBeanBuilder builder(byte[] bytes) { return BlockHeaderBean.toBuilder(bytes);}
    static BlockHeaderBean.BlockHeaderBeanBuilder builder(String hex) { return BlockHeaderBean.toBuilder(hex);}
    default BlockHeaderBean.BlockHeaderBeanBuilder toBuilder() { return ((BlockHeaderBean) this).toBuilder();}

    /**
     * Returns the difficulty target as a 256 bit value that can be compared to a SHA-256 hash. Inside a block the
     * target is represented using a compact form. If this form decodes to a value that is out of bounds, an exception
     * is thrown.
     */
    default BigInteger getDifficultyTargetAsInteger() throws VerificationException {
        BigInteger target = ByteTools.decodeCompactBits(getDifficultyTarget());
        return target;
    }

    /**
     * Returns the work represented by this block.<p>
     *
     * Work is defined as the number of tries needed to solve a block in the
     * average case. Consider a difficulty target that covers 5% of all possible
     * hash values. Then the work of the block will be 20. As the target gets
     * lower, the amount of work goes up.
     */
    default BigInteger getWork() throws VerificationException {
        BigInteger target = getDifficultyTargetAsInteger();
        return JCLBase.LARGEST_HASH.divide(target.add(BigInteger.ONE));
    }

}
