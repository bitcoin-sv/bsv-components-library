package com.nchain.jcl.tools.bytes;

import lombok.Builder;
import lombok.Getter;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-04-06 13:27
 *
 * This class stores the parameters that determine how much memory is used when ByteArrays are created, this is
 * heavily used during Serialization/Deserialization.
 *
 * As a rule of thumb, the bigger the array, the faster the Serialization/Deserialization is, but it might also be
 * a waste of space and a risk for out-of-memory errors. The safest approach is to use a "regular" size for most
 * situation, as defined in ARRAY_SIZE_NORMAL. and when we have to work with a Big Message or w expect a Big
 * Message coming down the wire, we use ARRAY_SIZE_FOR_DOWNLOADING.
 */

public class ByteArrayMemoryConfiguration {

    // The size of individual Bytes Array plays an important role in performance, and it depends on the type of
    // Operation (downloading bytes or deserializing them)
    public static final int ARRAY_SIZE_NORMAL = 10_000; // 10KB
    public static final int ARRAY_SIZE_FOR_DOWNLOADING = 100_000; // 100KB
    public static final int ARRAY_SIZE_FOR_SERIALIZING = 10_000; // 10KB

    // Capacity of each individual ByteArray created. Only applies to implementation that store the info in
    // Memory (like the ByteArrayImpl)
    @Getter
    private Integer byteArraySize = ARRAY_SIZE_NORMAL;


    @Builder(toBuilder = true)
    public ByteArrayMemoryConfiguration(Integer byteArraySize) {
        this.byteArraySize = (byteArraySize != null) ? byteArraySize: ARRAY_SIZE_NORMAL;
    }
}
