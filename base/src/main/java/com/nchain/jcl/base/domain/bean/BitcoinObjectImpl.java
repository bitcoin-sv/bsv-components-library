package com.nchain.jcl.base.domain.bean;

import com.nchain.jcl.base.domain.api.BitcoinObject;
import com.nchain.jcl.base.serialization.BitcoinSerializerFactory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * @author Steve Shadders
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A Base Class for a Bitcoin Object
 */
@AllArgsConstructor
@NoArgsConstructor
public class BitcoinObjectImpl<B> implements BitcoinObject {

    protected Long sizeInBytes;

    // If the Size is not present, then we serialize the object, and update the size.
    @Override
    public Long getSizeInBytes() {
        if (sizeInBytes == null) {
            byte[] serialized = BitcoinSerializerFactory.getSerializer(this.getClass()).serialize(this);
            sizeInBytes = (long) serialized.length;
        }
        return sizeInBytes;
    }
}
