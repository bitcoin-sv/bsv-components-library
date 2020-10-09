package com.nchain.jcl.base.domain.bean;

import com.nchain.jcl.base.domain.api.BitcoinSerializableObject;
import com.nchain.jcl.base.serialization.BitcoinSerializerFactory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Base Implementation for a Bitcoin Serializable Object. It includs information about the Size taken by the
 * Object afer Serialization, tht size can be used to make business decisions.
 */
@NoArgsConstructor
@AllArgsConstructor
public class BitcoinSerializableObjectImpl extends BitcoinObjectImpl implements BitcoinSerializableObject {

    protected Long sizeInBytes;

    /**
     * It returns the Size in Bytes taken by this object, after being Serialized. This value is only calculated once.
     * The fact that this field is populated on demand (the first time this method is called)  means that this class
     * is not "true" immutable, but it is immutable in all practical sense.
     */
    @Override
    public synchronized Long getSizeInBytes() {
        if (sizeInBytes == null) {
            byte[] serialized = BitcoinSerializerFactory.getSerializer(this.getClass()).serialize(this);
            sizeInBytes = (long) serialized.length;
        }
        return sizeInBytes;
    }
}
