package com.nchain.jcl.base.domain.api;

/**
 * @author Steve Shadders
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A Base marker interface for a Bitcoin Object.
 */
public interface BitcoinObject {

    long UNKNOWN_SIZE = Integer.MIN_VALUE;

    /**
     * Returns the Size of this Object in Bytes.
     * It represents the number of bytes this objects takes when it's serialized and sent over the wire.
     * Even though this value is closer to the "hardware" layer of things, some business logic depends on its
     * value, so we need to store it here.
     *
     */
    Long getSizeInBytes();
}
