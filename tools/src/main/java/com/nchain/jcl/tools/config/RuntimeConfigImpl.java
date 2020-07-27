package com.nchain.jcl.tools.config;

import com.nchain.jcl.tools.bytes.ByteArrayMemoryConfiguration;
import com.nchain.jcl.tools.files.FileUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.nio.file.Path;
import java.time.Duration;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-11 11:51
 *
 * An implementation for The RuntimeConfig interface. Provides placeholders that must be fullfilled with
 * values by extending classes.
 */
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class RuntimeConfigImpl implements RuntimeConfig {
    @Getter
    protected ByteArrayMemoryConfiguration byteArrayMemoryConfig;
    @Getter
    protected int msgSizeInBytesForRealTimeProcessing;
    @Getter
    protected Duration maxWaitingTimeForBytesInRealTime;
    @Getter
    protected FileUtils fileUtils;
}
