package com.nchain.jcl.base.domain.bean;

import com.nchain.jcl.base.domain.api.BitcoinSerializableObject;
import com.nchain.jcl.base.serialization.BitcoinSerializerFactory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * @date 2020-09-17
 */
@NoArgsConstructor
@AllArgsConstructor
public class BitcoinSerializableObjectImpl extends BitcoinObjectImpl implements BitcoinSerializableObject {

    protected Long sizeInBytes;

    // If the Size is not present, then we serialize the object, and update the size.
    @Override
    public synchronized Long getSizeInBytes() {
        if (sizeInBytes == null) {
            byte[] serialized = BitcoinSerializerFactory.getSerializer(this.getClass()).serialize(this);
            sizeInBytes = (long) serialized.length;
        }
        return sizeInBytes;
    }
}
