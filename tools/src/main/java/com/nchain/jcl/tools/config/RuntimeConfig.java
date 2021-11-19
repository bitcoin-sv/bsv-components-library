package com.nchain.jcl.tools.config;


import com.nchain.jcl.tools.bytes.ByteArrayConfig;
import com.nchain.jcl.tools.files.FileUtils;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An interface to provide values adjusted for a specific Hardware Configuration (Memory, Network Speed, etc)
 */
public interface RuntimeConfig {
    /**
     * Returns a ByteArrayMemoryConfiguration, which determines the amount of memory that the ByteArrays are using
     * during Serialization/Deserialization.
     */
    ByteArrayConfig getByteArrayMemoryConfig();

    /** Returns a number of Bytes. Any Message bigger than that value, is a candidate for Real-Time Deserialization */
    int getMsgSizeInBytesForRealTimeProcessing();

    /** File Utils used to read/write info in disk */
    FileUtils getFileUtils();

    /**
     * Returns the maximum number of Threads used in the P2P Service during the process of all the messages in/out
     * the network. This is an aproximation, the real number of Threads might be a bit bigger if the system is
     * under stress.
     */
    int getMaxNumThreadsForP2P();

    /**
     * If TRUE, and CachedThreadPool for the Threads used in the P2P Service will be used, using a maxLimite dfined by
     * "getMaxNumThreadsForP2P" Otherwise, a FixedThreadPool will be used.
     * NOTE: A CachedThreadPool has much higher performance than a Fixed one ofr small tasks. Its been proved than JCL
     * works much better with a CachedThreadPool, but sometimes it might not work very well if we connect JCL to another
     * system.
     */
    boolean useCachedThreadPoolForP2P();
}
