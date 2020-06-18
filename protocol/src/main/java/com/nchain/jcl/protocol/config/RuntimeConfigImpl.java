package com.nchain.jcl.protocol.config;

import com.nchain.jcl.tools.bytes.ByteArrayMemoryConfiguration;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

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
public class RuntimeConfigImpl implements RuntimeConfig {
    @Getter
    ByteArrayMemoryConfiguration byteArrayMemoryConfig;
    @Getter
    int msgSizeInBytesForRealTimeProcessing;
    @Getter
    int minSpeedBytesPerSec;
}
