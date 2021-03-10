package com.nchain.jcl.tools.config;


import com.nchain.jcl.tools.bytes.ByteArrayConfig;
import com.nchain.jcl.tools.files.FileUtils;

import java.time.Duration;

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
    protected Duration maxWaitingTimeForBytesInRealTime;
    protected FileUtils fileUtils;

    public RuntimeConfigImpl(ByteArrayConfig byteArrayMemoryConfig,
                             int msgSizeInBytesForRealTimeProcessing,
                             Duration maxWaitingTimeForBytesInRealTime,
                             FileUtils fileUtils) {
        this.byteArrayMemoryConfig = byteArrayMemoryConfig;
        this.msgSizeInBytesForRealTimeProcessing = msgSizeInBytesForRealTimeProcessing;
        this.maxWaitingTimeForBytesInRealTime = maxWaitingTimeForBytesInRealTime;
        this.fileUtils = fileUtils;
    }

    public RuntimeConfigImpl() { }

    public static RuntimeConfigImplBuilder builder()                { return new RuntimeConfigImplBuilder(); }
    public ByteArrayConfig getByteArrayMemoryConfig()  { return this.byteArrayMemoryConfig; }
    public int getMsgSizeInBytesForRealTimeProcessing()             { return this.msgSizeInBytesForRealTimeProcessing; }
    public Duration getMaxWaitingTimeForBytesInRealTime()           { return this.maxWaitingTimeForBytesInRealTime; }
    public FileUtils getFileUtils()                                 { return this.fileUtils; }

    public RuntimeConfigImplBuilder toBuilder() {
        return new RuntimeConfigImplBuilder().byteArrayMemoryConfig(this.byteArrayMemoryConfig).msgSizeInBytesForRealTimeProcessing(this.msgSizeInBytesForRealTimeProcessing).maxWaitingTimeForBytesInRealTime(this.maxWaitingTimeForBytesInRealTime).fileUtils(this.fileUtils);
    }

    /**
     * Builder class
     */
    public static class RuntimeConfigImplBuilder {
        private ByteArrayConfig byteArrayMemoryConfig;
        private int msgSizeInBytesForRealTimeProcessing;
        private Duration maxWaitingTimeForBytesInRealTime;
        private FileUtils fileUtils;

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

        public RuntimeConfigImplBuilder maxWaitingTimeForBytesInRealTime(Duration maxWaitingTimeForBytesInRealTime) {
            this.maxWaitingTimeForBytesInRealTime = maxWaitingTimeForBytesInRealTime;
            return this;
        }

        public RuntimeConfigImplBuilder fileUtils(FileUtils fileUtils) {
            this.fileUtils = fileUtils;
            return this;
        }

        public RuntimeConfigImpl build() {
            return new RuntimeConfigImpl(byteArrayMemoryConfig, msgSizeInBytesForRealTimeProcessing, maxWaitingTimeForBytesInRealTime, fileUtils);
        }
    }
}
