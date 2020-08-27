package com.nchain.jcl.tools.config.provided;

import com.nchain.jcl.tools.config.RuntimeConfigImpl;
import com.nchain.jcl.tools.bytes.ByteArrayMemoryConfiguration;
import com.nchain.jcl.tools.files.FileUtils;
import com.nchain.jcl.tools.files.FileUtilsBuilder;
import lombok.Value;

import java.time.Duration;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-11 11:52
 *
 * Default RuntimeConfiguration.
 */
@Value
public class RuntimeConfigDefault extends RuntimeConfigImpl {

    private static final ByteArrayMemoryConfiguration BYTE_ARRAY_MEMORY_CONFIGURATION = ByteArrayMemoryConfiguration.builder()
            .byteArraySize(ByteArrayMemoryConfiguration.ARRAY_SIZE_NORMAL)
            .build();

    private static final int MSG_SIZE_IN_BYTES_FOR_REAL_TIME_PROCESSING = 10_000_000;

    private static final Duration MAX_WAITING_TIME_FOR_BYTES_IN_REAL_TIME = Duration.ofMillis(15000);

    /** Constructor */
    public RuntimeConfigDefault() {
        super();
        init(null);
    }

    /** Constructor */
    public RuntimeConfigDefault(ClassLoader classLoader) {
        super();
        init(classLoader);
    }

    private void init(ClassLoader classLoader) {
        super.byteArrayMemoryConfig = BYTE_ARRAY_MEMORY_CONFIGURATION;
        super.msgSizeInBytesForRealTimeProcessing = MSG_SIZE_IN_BYTES_FOR_REAL_TIME_PROCESSING;
        super.maxWaitingTimeForBytesInRealTime = MAX_WAITING_TIME_FOR_BYTES_IN_REAL_TIME;
        try {
            FileUtilsBuilder fileUtilsBuilder = new FileUtilsBuilder().useTempFolder();
            if (classLoader != null) {
                fileUtilsBuilder.copyFromClasspath();
                fileUtils = fileUtilsBuilder.build(classLoader);
            } else fileUtils = fileUtilsBuilder.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
