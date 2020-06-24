package com.nchain.jcl.tools.config.provided;

import com.nchain.jcl.tools.config.RuntimeConfigImpl;
import com.nchain.jcl.tools.bytes.ByteArrayMemoryConfiguration;

import java.time.Duration;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-11 11:52
 *
 * Default RuntimeConfiguration.
 */
public class RuntimeConfigDefault extends RuntimeConfigImpl {
    private static final ByteArrayMemoryConfiguration byteArrayMemoryConfiguration =
            ByteArrayMemoryConfiguration.builder().byteArraySize(ByteArrayMemoryConfiguration.ARRAY_SIZE_NORMAL).build();

    // Any message bigger than 10MB is considered a  "Big" Message:
    private static final int REAL_TIME_PROCESSING_MIN_BYTES = 10_000_000;

    // We Wait for 5 secs at the most in Real-time
    private static final Duration MAX_WAIING_REAL_TIME = Duration.ofMillis(2000);

    /** Constructor */
    public RuntimeConfigDefault() {
        super(byteArrayMemoryConfiguration, REAL_TIME_PROCESSING_MIN_BYTES, MAX_WAIING_REAL_TIME);
    }
}
