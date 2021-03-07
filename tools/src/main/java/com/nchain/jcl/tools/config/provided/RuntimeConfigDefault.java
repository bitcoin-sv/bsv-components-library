package com.nchain.jcl.tools.config.provided;


import com.nchain.jcl.tools.bytes.ByteArrayConfig;
import com.nchain.jcl.tools.config.RuntimeConfigImpl;
import com.nchain.jcl.tools.files.FileUtilsBuilder;

import java.time.Duration;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Default RuntimeConfiguration.
 */
public final class RuntimeConfigDefault extends RuntimeConfigImpl {

    private static final ByteArrayConfig BYTE_ARRAY_MEMORY_CONFIGURATION = new ByteArrayConfig();

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

    public String toString() {
        return "RuntimeConfigDefault";
    }
}
