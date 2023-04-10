package io.bitcoinsv.jcl.tools.writebuffer;

import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader;
import io.bitcoinsv.jcl.tools.thread.ThreadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.Optional.ofNullable;

/**
 * @author j.pomer@nchain.com
 * <p>
 * This class provides custom abstract implementation for buffering bytes.
 */
public class WriteBuffer {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final SocketChannel socketChannel;
    private final long maxSize;
    private final int batchSize;
    private final AtomicInteger bytesRead = new AtomicInteger();
    private final AtomicInteger bytesWritten = new AtomicInteger();
    private final ReentrantLock writeLock = new ReentrantLock();
    private final Condition queueNotFull = writeLock.newCondition();
    private final AtomicBoolean queueFull = new AtomicBoolean();
    private final String name;
    private final ArrayBlockingQueue<ByteBuffer> buffers;
    private final ByteBufferManager byteBufferManager;
    private ByteBuffer activeBuffer;

    private IWriteBufferConfig config;

    private ScheduledExecutorService executorService;

    private AtomicBoolean isRunning = new AtomicBoolean(true);

    public WriteBuffer(String name, IWriteBufferConfig config, SocketChannel socketChannel) {
        this.name = name;
        this.config = config;

        this.socketChannel = socketChannel;

        this.maxSize = config.getMaxSize();
        this.batchSize = config.getBatchSize();

        int capacity = (int) (maxSize / batchSize);
        buffers = new ArrayBlockingQueue<>(capacity);

        byteBufferManager = new ByteBufferManager(capacity + 1, () -> ByteBuffer.allocate(batchSize));

        activeBuffer = getNewActiveBuffer();

        if (config.isMonitoringEnabled()) {
            executorService = ThreadUtils.getScheduledExecutorService(this.getClass().getSimpleName());
            executorService.scheduleAtFixedRate(this::monitor, 1L, 1L, TimeUnit.SECONDS);
        }
    }

    private void monitor() {
        if (log.isInfoEnabled()) {
            var write = bytesWritten.getAndSet(0) / 1_000_000.0;
            var read = bytesRead.getAndSet(0) / 1_000_000.0;

            log.info(
                "{} :: use: {}/{}MB :: write: {}MB/s :: read: {}MB/s",
                name,
                String.format("%.2f", (maxSize - getFreeSpace()) / 1_000_000.0),
                String.format("%.2f", maxSize / 1_000_000.0),
                String.format("%.2f", write),
                String.format("%.2f", read)
            );
        }
    }

    // return num of bytes written
    public synchronized int write(ByteArrayReader reader) {
        if (!isRunning.get()) {
            return 0;
        }

        int bytesWritten = 0;

        while (!reader.isEmpty()) {
            try {
                // We obtain new lock each iteration in case extraction was executed in between
                getWriteLock();

                int numBytesToRead = (int) Math.min(activeBuffer.remaining(), reader.size());

                var bytes = reader.read(numBytesToRead);

                activeBuffer.put(bytes);

                this.bytesWritten.addAndGet(numBytesToRead);
                bytesWritten += numBytesToRead;

                flushActiveBuffer();

            } catch (Exception e) {
                log.error("Write error!", e);
            } finally {
                writeLock.unlock();
            }
        }

        return bytesWritten;
    }

    private void getWriteLock() {
        writeLock.lock();

        try {
            // Write locks is obtained only when queue lock is released.
            // Queue lock is released when extraction is executed.
            if (queueFull.get()) {
                queueNotFull.await();
            }
        } catch (InterruptedException e) {
            writeLock.unlock();
            throw new IllegalStateException(e);
        }
    }

    public int extract() throws IOException {
        if (!isRunning.get()) {
            return 0;
        }

        int writeResult = 0;

        try {
            writeLock.lock();

            writeResult += extractQueue();

            if (buffers.isEmpty() && !isBatchEmpty(activeBuffer)) {
                prepareForExtraction(activeBuffer);
                buffers.add(activeBuffer);
                activeBuffer = byteBufferManager.getNew().get();

                writeResult += extractQueue();
            }

            if (writeResult > 0) {
                queueFull.set(false);
                queueNotFull.signalAll();
            }
        } catch (Exception e) {
            log.error("Extraction error!", e);
        } finally {
            writeLock.unlock();
        }

        return writeResult;
    }

    private int extractQueue() throws IOException {
        int writeResult = 0;

        while (!buffers.isEmpty()) {
            var buffer = buffers.peek();

            writeResult += extractBuffer(buffer);

            if (buffer.hasRemaining()) {
                break;
            }

            buffers.poll();
            byteBufferManager.release(buffer);
        }

        return writeResult;
    }

    /**
     * Propagates provided buffer into writer function handling it.
     * Tracks number of bytes being written in writer function provided
     *
     * @param buffer buffer being extracted
     * @return number of bytes being extracted in writer function
     */
    private int extractBuffer(ByteBuffer buffer) throws IOException {
        int numBytesWritten = socketChannel.write(buffer);
        bytesRead.addAndGet(numBytesWritten);
        return numBytesWritten;
    }

    /**
     * Gets buffer from {@link ByteBufferManager}
     *
     * @return clean buffer
     */
    public ByteBuffer getNewActiveBuffer() {
        var buffer = byteBufferManager.getNew();

        while (buffer.isEmpty()) {
            buffer = byteBufferManager.getNew();
        }

        return buffer.get();
    }

    /**
     * calculates free space available in buffer
     *
     * @return number of bytes available in buffer
     */
    private long getFreeSpace() {
        return maxSize - (buffers.size() * batchSize) - activeBuffer.position();
    }

    /**
     * prepared buffer to be ready for extraction
     *
     * @param buffer buffer being prepared.
     */
    private void prepareForExtraction(ByteBuffer buffer) {
        var size = buffer.position();
        buffer.limit(size);
        buffer.position(0);
    }

    /**
     * Flushes active buffer into queue.
     */
    private void flushActiveBuffer() {
        if (isBatchEmpty(activeBuffer)) {
            return;
        }

        if (!isBatchFull(activeBuffer)) {
            return;
        }

        prepareForExtraction(activeBuffer);

        buffers.add(activeBuffer);
        activeBuffer = getNewActiveBuffer();

        if (buffers.remainingCapacity() == 0) {
            queueFull.set(true);
        }
    }

    /**
     * Helper function for checking if buffer is empty
     *
     * @param buffer buffer we are checking
     * @return condition result
     */
    private boolean isBatchEmpty(ByteBuffer buffer) {
        return buffer.position() == 0;
    }

    /**
     * Helper function for checking if buffer is full
     *
     * @param buffer buffer we are checking
     * @return condition result
     */
    private boolean isBatchFull(ByteBuffer buffer) {
        return buffer.position() == buffer.capacity();
    }

    /**
     * Check if buffer is empty.
     * <p>
     * This checks queue and active buffers state
     *
     * @return condition result
     */
    public synchronized boolean isEmpty() {
        return buffers.isEmpty() && isBatchEmpty(activeBuffer);
    }

    public void stop() {
        isRunning.set(false);
        ofNullable(executorService).ifPresent(ExecutorService::shutdownNow);
    }
}