package com.nchain.jcl.protocol.config;

import com.nchain.jcl.tools.bytes.ByteArrayMemoryConfiguration;

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
     * Returns the minimun (bytes/Sec) we expect in the network.
     * It only applies for Big Messages (Real-Time processing). When a Big Message is being received, its content is
     * processed as it arrives (in Real-Time), without waiting for the whole Message. If the speed by which that
     * message is received is slower than this value, then the deserialization is aborted.
     */
    int getMinSpeedBytesPerSec();
}
