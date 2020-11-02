package com.nchain.jcl.base.domain.bean;

import com.nchain.jcl.base.domain.api.BitcoinHashableObject;
import com.nchain.jcl.base.serialization.BitcoinSerializerFactory;
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import static com.google.common.base.Preconditions.checkState;

/**
 * @author Steve Shadders
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Imlementation of the Hashable Interface. Any Object extendind this class will provide a "hash" field which
 * value will be calculated based on the object content itself.
 */
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BitcoinHashableImpl extends BitcoinSerializableObjectImpl implements BitcoinHashableObject {

    // This value will be calculated on demand when the getter is called.
    @EqualsAndHashCode.Exclude
    protected Sha256Wrapper hash;

    public BitcoinHashableImpl(Long sizeInBytes, Sha256Wrapper hash) {
        super(sizeInBytes);
        this.hash = hash;
    }

    // It serializes the object content into a Byte Array. This aray wil be used as a base for the Gash calculation
    private byte[] serialize() {
        checkState(BitcoinSerializerFactory.hasFor(this.getClass()), "No Serializer for " + getClass().getSimpleName());
        return BitcoinSerializerFactory.getSerializer(getClass()).serialize(this);
    }

    /**
     * It returns the "hash" of this object. This value is only calculated once.
     * The fact that this field is populated on demand (the first time this method is called)  means that this class
     * is not "true" immutable, but it is immutable in all practical sense.
     */
    public synchronized Sha256Wrapper getHash() {
        if (hash == null) {
            hash = Sha256Wrapper.wrapReversed(Sha256Wrapper.twiceOf(serialize()).getBytes());
        }
        return hash;
    }
}
