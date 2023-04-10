package io.bitcoinsv.jcl.tools.config.provided;


import io.bitcoinsv.jcl.tools.writebuffer.WriteBufferConfig;
import io.bitcoinsv.jcl.tools.writebuffer.IWriteBufferConfig;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayConfig;
import io.bitcoinsv.jcl.tools.config.RuntimeConfig;
import io.bitcoinsv.jcl.tools.config.RuntimeConfigImpl;
import io.bitcoinsv.jcl.tools.files.FileUtilsBuilder;


/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Default RuntimeConfiguration.
 */
public final class RuntimeConfigDefault extends RuntimeConfigImpl implements RuntimeConfig {

    // Default values:
    public static final ByteArrayConfig DEF_BYTE_ARRAY_MEM_CONFIG = new ByteArrayConfig();
    public static final int DEF_MSG_SIZE_BYTES_REAL_TIME = 10_000_000;
    public static final int DEF_P2P_THREADS_MAX = 50;
    public static final boolean DEF_P2P_THREADS_CACHED = false;
    public static final IWriteBufferConfig DEF_WRITE_BUFFER_CONFIG = new WriteBufferConfig();

    /** Constructor */
    public RuntimeConfigDefault() {
        super();
        init(null);
    }

    /** Constructor */
    public RuntimeConfigDefault(ClassLoader classLoader) {
        this();
        init(classLoader);
    }

    private void init(ClassLoader classLoader) {
        // We initialize all the parent fields:
        super.byteArrayMemoryConfig = DEF_BYTE_ARRAY_MEM_CONFIG;
        super.msgSizeInBytesForRealTimeProcessing = DEF_MSG_SIZE_BYTES_REAL_TIME;
        super.maxNumThreadsForP2P = DEF_P2P_THREADS_MAX;
        super.useCachedThreadPoolForP2P = DEF_P2P_THREADS_CACHED;
        super.writeBufferConfig = DEF_WRITE_BUFFER_CONFIG;

        try {
            FileUtilsBuilder fileUtilsBuilder = new FileUtilsBuilder().useTempFolder();
            if (classLoader != null) {
                fileUtilsBuilder.copyFromClasspath();
                super.fileUtils = fileUtilsBuilder.build(classLoader);
            } else super.fileUtils = fileUtilsBuilder.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return "RuntimeConfigDefault";
    }

    @Override
    public IWriteBufferConfig getWriteBufferConfig() {
        return new WriteBufferConfig();
    }
}