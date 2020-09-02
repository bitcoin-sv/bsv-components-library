package com.nchain.jcl.base.tools.config;

import com.nchain.jcl.base.tools.bytes.ByteArrayMemoryConfiguration;
import com.nchain.jcl.base.tools.files.FileUtils;

import java.time.Duration;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-11 10:06
 *
 * An interface to provide values adjusted for a specific Hardware Configuration (Memory, Network Speed, etc)
 */
public interface RuntimeConfig {
    /**
     * Returns a ByteArrayMemoryConfiguration, which determines the amount of memory that the ByteArrays are using
     * during Serialization/Deserialization.
     */
    ByteArrayMemoryConfiguration getByteArrayMemoryConfig();

    /** Returns a number of Bytes. Any Message bigger than that value, is a candidate for Real-Time Deserialization */
    int getMsgSizeInBytesForRealTimeProcessing();

    /**
     * If we are processing bytes in real time and wehave to wait for longer than the value returned by this method,
     * then the process is interrupted.
     */
    Duration getMaxWaitingTimeForBytesInRealTime();

    /** File Utils used to read/write info in disk */
    FileUtils getFileUtils();
}
