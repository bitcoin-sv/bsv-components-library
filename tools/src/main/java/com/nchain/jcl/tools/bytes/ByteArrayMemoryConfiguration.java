package com.nchain.jcl.tools.bytes;

import lombok.Builder;
import lombok.Getter;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-04-06 13:27
 *
 * It stores the Byte Array configuration. This configure here will determine how the ByteArrays will
 * be created in terms of Size.
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
