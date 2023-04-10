package io.bitcoinsv.jcl.tools.config;


import io.bitcoinsv.jcl.tools.writebuffer.IWriteBufferConfig;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayConfig;
import io.bitcoinsv.jcl.tools.config.provided.RuntimeConfigDefault;
import io.bitcoinsv.jcl.tools.files.FileUtils;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An implementation for The RuntimeConfig interface. Provides placeholders that must be fullfilled with
 * values by extending classes.
 */
public class RuntimeConfigImpl implements RuntimeConfig {
    protected ByteArrayConfig byteArrayMemoryConfig;
    protected int msgSizeInBytesForRealTimeProcessing;
    protected FileUtils fileUtils;
    protected int maxNumThreadsForP2P;
    protected boolean useCachedThreadPoolForP2P;
    protected IWriteBufferConfig writeBufferConfig;

    public RuntimeConfigImpl(
            ByteArrayConfig byteArrayMemoryConfig,
            int msgSizeInBytesForRealTimeProcessing,
            FileUtils fileUtils,
            int maxNumThreadsForP2P,
            boolean useCachedThreadPoolForP2P,
            IWriteBufferConfig writeBufferConfig
    ) {
        this.byteArrayMemoryConfig = byteArrayMemoryConfig;
        this.msgSizeInBytesForRealTimeProcessing = msgSizeInBytesForRealTimeProcessing;
        this.fileUtils = fileUtils;
        this.maxNumThreadsForP2P = maxNumThreadsForP2P;
        this.useCachedThreadPoolForP2P = useCachedThreadPoolForP2P;
        this.writeBufferConfig = writeBufferConfig;
    }

    public RuntimeConfigImpl() {}

    @Override
    public ByteArrayConfig getByteArrayMemoryConfig() {
        return this.byteArrayMemoryConfig;
    }

    @Override
    public int getMsgSizeInBytesForRealTimeProcessing() {
        return this.msgSizeInBytesForRealTimeProcessing;
    }

    @Override
    public FileUtils getFileUtils() {
        return this.fileUtils;
    }

    @Override
    public int getMaxNumThreadsForP2P() {
        return this.maxNumThreadsForP2P;
    }

    @Override
    public boolean useCachedThreadPoolForP2P() {
        return this.useCachedThreadPoolForP2P;
    }

    @Override
    public IWriteBufferConfig getWriteBufferConfig() {
        return writeBufferConfig;
    }

    public static RuntimeConfigImplBuilder builder()                { return new RuntimeConfigImplBuilder(); }

    public RuntimeConfigImplBuilder toBuilder() {
        return new RuntimeConfigImplBuilder()
                .byteArrayMemoryConfig(this.byteArrayMemoryConfig)
                .msgSizeInBytesForRealTimeProcessing(this.msgSizeInBytesForRealTimeProcessing)
                .fileUtils(this.fileUtils)
                .maxNumThreadsForP2P(this.maxNumThreadsForP2P)
                .useCachedThreadPoolForP2P(this.useCachedThreadPoolForP2P)
                .writeBufferConfig(this.writeBufferConfig);
    }

    /**
     * Builder class
     */
    public static class RuntimeConfigImplBuilder {
        private ByteArrayConfig byteArrayMemoryConfig;
        private int msgSizeInBytesForRealTimeProcessing;
        private FileUtils fileUtils;
        protected int maxNumThreadsForP2P;
        protected boolean useCachedThreadPoolForP2P;
        private IWriteBufferConfig writeBufferConfig = RuntimeConfigDefault.DEF_WRITE_BUFFER_CONFIG;

        RuntimeConfigImplBuilder() {
        }

        public RuntimeConfigImplBuilder byteArrayMemoryConfig(ByteArrayConfig byteArrayMemoryConfig) {
            this.byteArrayMemoryConfig = byteArrayMemoryConfig;
            return this;
        }

        public RuntimeConfigImplBuilder msgSizeInBytesForRealTimeProcessing(int msgSizeInBytesForRealTimeProcessing) {
            this.msgSizeInBytesForRealTimeProcessing = msgSizeInBytesForRealTimeProcessing;
            return this;
        }

        public RuntimeConfigImplBuilder fileUtils(FileUtils fileUtils) {
            this.fileUtils = fileUtils;
            return this;
        }

        public RuntimeConfigImplBuilder maxNumThreadsForP2P(int maxNumThreadsForP2P) {
            this.maxNumThreadsForP2P = maxNumThreadsForP2P;
            return this;
        }

        public RuntimeConfigImplBuilder useCachedThreadPoolForP2P(boolean useCachedThreadPoolForP2P) {
            this.useCachedThreadPoolForP2P = useCachedThreadPoolForP2P;
            return this;
        }

        public RuntimeConfigImplBuilder writeBufferConfig(IWriteBufferConfig writeBufferConfig) {
            this.writeBufferConfig = writeBufferConfig;
            return this;
        }

        public RuntimeConfigImpl build() {
            return new RuntimeConfigImpl(byteArrayMemoryConfig, msgSizeInBytesForRealTimeProcessing, fileUtils, maxNumThreadsForP2P, useCachedThreadPoolForP2P, writeBufferConfig);
        }
    }
}