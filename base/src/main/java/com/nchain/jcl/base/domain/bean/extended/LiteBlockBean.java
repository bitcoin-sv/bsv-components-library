package com.nchain.jcl.base.domain.bean.extended;

import com.nchain.jcl.base.domain.api.base.BlockHeader;
import com.nchain.jcl.base.domain.api.extended.BlockMeta;
import com.nchain.jcl.base.domain.api.extended.ChainInfo;
import com.nchain.jcl.base.domain.api.extended.LiteBlock;
import com.nchain.jcl.base.domain.bean.base.AbstractBlockBean;
import com.nchain.jcl.base.serialization.BitcoinSerializerFactory;
import com.nchain.jcl.base.tools.bytes.HEX;
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper;
import lombok.Builder;
import lombok.Value;
import static com.google.common.base.Preconditions.checkState;

/**
 * @author Steve Shadders
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Implementation of a LiteBlock
 * This class is IMMUTABLE. Instances can be created by using a Lombok generated Builder.
 */
@Value
public class LiteBlockBean extends AbstractBlockBean implements LiteBlock {
    private BlockMeta blockMeta;
    private ChainInfo chainInfo;

    @Builder(toBuilder = true)
    public LiteBlockBean(Long sizeInBytes, BlockHeader header, BlockMeta blockMeta, ChainInfo chainInfo) {
        super(sizeInBytes, header);
        this.blockMeta = blockMeta;
        this.chainInfo = chainInfo;
    }

    /** Adding a method to create a Builder after Deserializing an object from a source of bytes */
    public static LiteBlockBeanBuilder build(byte[] bytes) {
        checkState(BitcoinSerializerFactory.hasFor(LiteBlock.class), "No Serializer found for " + LiteBlock.class.getSimpleName());
        return ((LiteBlock) BitcoinSerializerFactory.getSerializer(LiteBlock.class).deserialize(bytes)).toBuilder();
    }
    /** Adding a method to create a Builder after Deserialzing an object from a HEX */
    public static LiteBlockBeanBuilder build(String hex) {
        return build(HEX.decode(hex));
    }
}
