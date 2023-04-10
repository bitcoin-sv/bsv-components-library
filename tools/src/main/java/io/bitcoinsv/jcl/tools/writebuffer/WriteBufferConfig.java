package io.bitcoinsv.jcl.tools.writebuffer;

public class WriteBufferConfig implements IWriteBufferConfig {
    private long maxSize = DEFAULT_MAX_SIZE;

    private int batchSize = DEFAULT_BATCH_SIZE;

    private boolean monitoringEnabled = DEFAULT_MONITORING_ENABLED;


    public WriteBufferConfig() {
    }

    public WriteBufferConfig setMaxSize(long maxSize) {
        this.maxSize = maxSize;
        return this;
    }

    public WriteBufferConfig setBatchSize(int batchSize) {
        this.batchSize = batchSize;
        return this;
    }

    public WriteBufferConfig setMonitoringEnabled(boolean enabled) {
        this.monitoringEnabled = enabled;
        return this;
    }

    @Override
    public long getMaxSize() {
        return maxSize;
    }

    @Override
    public int getBatchSize() {
        return batchSize;
    }

    @Override
    public boolean isMonitoringEnabled() {
        return monitoringEnabled;
    }
}