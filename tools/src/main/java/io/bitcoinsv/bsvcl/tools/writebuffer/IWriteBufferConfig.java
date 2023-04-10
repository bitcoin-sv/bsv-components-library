package io.bitcoinsv.bsvcl.tools.writebuffer;

/**
 * Configuration interface used in {@link WriteBuffer}.
 */
public interface IWriteBufferConfig {
    long DEFAULT_MAX_SIZE = 10_000_000L; // 10MB

    int DEFAULT_BATCH_SIZE = 10_000; // 10 KB

    boolean DEFAULT_MONITORING_ENABLED = false;


    WriteBufferConfig setMaxSize(long maxSize);

    WriteBufferConfig setBatchSize(int batchSize);

    WriteBufferConfig setMonitoringEnabled(boolean enabled);

    /**
     * maximum size of bytes used by byte buffer
     *
     * @return size in bytes
     */
    long getMaxSize();

    /**
     * maximum size of batch in buffer
     *
     * @return size in bytes
     */
    int getBatchSize();

    /**
     * is monitoring enabled flag
     *
     * @return monitoring enabled flag
     */
    boolean isMonitoringEnabled();
}