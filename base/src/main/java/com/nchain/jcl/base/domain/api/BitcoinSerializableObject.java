package com.nchain.jcl.base.domain.api;

/**
 * @author Steve Shadders
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A Bitcoin Serializable Object is a Bitcoin Object that can be serialized or deserialized. It also includes a
 * field that stores info about the size of the object in serialized form.
 */
public interface BitcoinSerializableObject extends BitcoinObject {
    long UNKNOWN_SIZE = Integer.MIN_VALUE;

    /** Returns the Size in bytes taken by this object after being serialized */
    Long getSizeInBytes();
}
