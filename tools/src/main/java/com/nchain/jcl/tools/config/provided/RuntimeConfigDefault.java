package com.nchain.jcl.tools.config.provided;


import com.nchain.jcl.tools.bytes.ByteArrayConfig;
import com.nchain.jcl.tools.config.RuntimeConfig;
import com.nchain.jcl.tools.config.RuntimeConfigImpl;
import com.nchain.jcl.tools.files.FileUtilsBuilder;


/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Default RuntimeConfiguration.
 */
public final class RuntimeConfigDefault extends RuntimeConfigImpl implements RuntimeConfig {

    // Default values:
    private static ByteArrayConfig byteArrayMemoryConfig = new ByteArrayConfig();
    private static int msgSizeInBytesForRealTimeProcessing = 10_000_000;;
    private static int maxNumThreadsForP2P = 50;
    private static boolean useCachedThreadPoolForP2P = false;

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
        super.byteArrayMemoryConfig = byteArrayMemoryConfig;
        super.msgSizeInBytesForRealTimeProcessing = msgSizeInBytesForRealTimeProcessing;
        super.maxNumThreadsForP2P = maxNumThreadsForP2P;
        super.useCachedThreadPoolForP2P = useCachedThreadPoolForP2P;

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
}
