package com.nchain.jcl.base.domain.bean.base;

import com.nchain.jcl.base.domain.api.base.BlockHeader;
import com.nchain.jcl.base.domain.bean.BitcoinHashableImpl;
import com.nchain.jcl.base.serialization.BitcoinSerializerFactory;
import com.nchain.jcl.base.tools.bytes.HEX;
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import static com.google.common.base.Preconditions.checkState;

/**
 * @author Steve Shadders
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Implementation of blockHeader.
 * This class is IMMUTABLE. Instances can be created by using a Lombok generated Builder.
 */
@Builder(toBuilder = true)
@Value
@AllArgsConstructor
public class BlockHeaderBean extends BitcoinHashableImpl implements BlockHeader {

    // Fields defined as part of the protocol format.
    private long version;
    private Sha256Wrapper prevBlockHash;
    private Sha256Wrapper merkleRoot;
    private long time;
    private long difficultyTarget; // "nBits"
    private long nonce;

    /** Adding a method to create a Builder after Deserialize an object from a source of bytes */
    public static BlockHeaderBean.BlockHeaderBeanBuilder toBuilder(byte[] bytes) {
        checkState(BitcoinSerializerFactory.hasFor(BlockHeader.class), "No Serializer for " + BlockHeader.class.getSimpleName());
        return ((BlockHeaderBean) BitcoinSerializerFactory.getSerializer(BlockHeader.class).deserialize(bytes)).toBuilder();
    }

    /** Adding a method to create a Builder after Deserialize an object from a n HEX String */
    public static BlockHeaderBean.BlockHeaderBeanBuilder toBuilder(String hex) {
        return ((BlockHeaderBean) BitcoinSerializerFactory.getSerializer(BlockHeader.class).deserialize(HEX.decode(hex))).toBuilder();
    }
}
