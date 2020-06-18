package com.nchain.jcl.protocol.config.provided;

import com.nchain.jcl.protocol.config.RuntimeConfigImpl;
import com.nchain.jcl.tools.bytes.ByteArrayMemoryConfiguration;

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

    // We expect at least 100 bytes/Sec when processing a Big Message
    private static final int MIN_SPEED_BYTES_PER_SEC = 100;

    /** Constructor */
    public RuntimeConfigDefault() {
        super(byteArrayMemoryConfiguration, REAL_TIME_PROCESSING_MIN_BYTES, MIN_SPEED_BYTES_PER_SEC);
    }
}
