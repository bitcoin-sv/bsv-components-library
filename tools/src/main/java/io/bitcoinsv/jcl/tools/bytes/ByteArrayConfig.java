package io.bitcoinsv.jcl.tools.bytes;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This class stores the parameters that determine how much memory is used when an individual ByteArray is created, this is
 * heavily used during Serialization/Deserialization.
 *
 * As a rule of thumb, the bigger the array, the faster the Serialization/Deserialization is, but it might also be
 * a waste of space and a risk for out-of-memory errors. The safest approach is to use a "regular" size for most
 * situations, as defined in ARRAY_SIZE_NORMAL. and when we have to work with a Big Message, we use
 * ARRAY_SIZE_BIG.
 */

public class ByteArrayConfig {

    // Regular size for a ByteArray. big enough to handle any individual message, but small enough to avoid memory problems
    public static final int ARRAY_SIZE_NORMAL = 10_000; // 10KB
    // Big Size. Only suitable when we expect a Big Message coming...
    public static final int ARRAY_SIZE_BIG = 10_000_000; // 10 MB

    // Capacity of each individual ByteArray created:
    private Integer byteArraySize = ARRAY_SIZE_NORMAL;

    public ByteArrayConfig(Integer byteArraySize) {
        this.byteArraySize = (byteArraySize != null) ? byteArraySize: ARRAY_SIZE_NORMAL;
    }

    public ByteArrayConfig() {
        this(null);
    }

    public Integer getByteArraySize()  {
        return this.byteArraySize;
    }

}
