package com.nchain.jcl.tools;

import com.nchain.jcl.tools.bytes.ByteArrayMemoryConfiguration;
import lombok.Builder;

import java.util.function.Consumer;


/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-03-10 12:43
 *
 * An implementation of the Runtime Config interface. It provides a Builder useful for creating instances of
 * this class, and also creates automatically the BackArrayMemoryManager within.
 */
public class RuntimeConfigAutoImpl implements RuntimeConfig {

    @Builder
    private RuntimeConfigAutoImpl(  Integer maxMemoryUsedByByteArraysInMB,
                                    Integer byteArraySize) {

        Integer maxMemoryInMB = (maxMemoryUsedByByteArraysInMB != null)
                ? maxMemoryUsedByByteArraysInMB
                : ByteArrayMemoryConfiguration.DEFAULT_MAX_BYTES_MEMORY.intValue();

        ByteArrayMemoryConfiguration memoryConfig = ByteArrayMemoryConfiguration.builder()
                .maxNumBytesMemory((long) maxMemoryInMB * 1_000_000)
                .byteArraySize(byteArraySize)
                .build();
    }
}
