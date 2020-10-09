package com.nchain.jcl.base.core;

/**
 * @author Steve Shadders
 * Copyright (c) 2018-2020 nChain Ltd
 */

import java.io.Serializable;

// todo: type of comparable is different, see https://errorprone.info/bugpattern/ComparableType
@SuppressWarnings("ComparableType")
public interface Addressable extends Serializable, Cloneable, Comparable<VersionedChecksummedBytes> {

    String toBase58();

    int getVersion();

    byte[] getHash160();

}
