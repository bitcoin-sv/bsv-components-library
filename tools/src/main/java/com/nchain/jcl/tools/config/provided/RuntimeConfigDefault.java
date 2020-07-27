package com.nchain.jcl.tools.config.provided;

import com.nchain.jcl.tools.config.RuntimeConfigImpl;
import com.nchain.jcl.tools.bytes.ByteArrayMemoryConfiguration;
import com.nchain.jcl.tools.files.FileUtils;
import com.nchain.jcl.tools.files.FileUtilsBuilder;
import lombok.Builder;
import lombok.Getter;
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
    @Getter
    private ByteArrayMemoryConfiguration byteArrayMemoryConfig = ByteArrayMemoryConfiguration.builder()
            .byteArraySize(ByteArrayMemoryConfiguration.ARRAY_SIZE_NORMAL)
            .build();
    @Getter
    private int msgSizeInBytesForRealTimeProcessing = 10_000_000;
    @Getter
    private Duration maxWaitingTimeForBytesInRealTime = Duration.ofMillis(2000);
    @Getter
    private FileUtils fileUtils;

    /** Constructor */
    public RuntimeConfigDefault() {
        super();
        super.byteArrayMemoryConfig = byteArrayMemoryConfig;
        super.msgSizeInBytesForRealTimeProcessing = msgSizeInBytesForRealTimeProcessing;
        super.maxWaitingTimeForBytesInRealTime = maxWaitingTimeForBytesInRealTime;
        try {
            fileUtils = new FileUtilsBuilder().useTempFolder().copyFromClasspath().build();
            super.fileUtils = fileUtils;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
