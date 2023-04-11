package io.bitcoinsv.bsvcl.common.writebuffer;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Optional;
import java.util.Queue;
import java.util.function.Supplier;

/**
 * Holds instances of ByteBuffers. This class is used for reusing existing
 * instances of ByteBuffers to reduce unnecessary memory allocations.
 * <p>
 * Potential improvements:
 * - reduce number of created instances if not needed
 */
public class ByteBufferManager {
    private final int capacity;
    private final Supplier<ByteBuffer> byteBufferCreator;
    private final Queue<ByteBuffer> emptyByteBuffers = new ArrayDeque<>();

    public ByteBufferManager(int capacity, Supplier<ByteBuffer> byteBufferCreator) {
        this.capacity = capacity;
        this.byteBufferCreator = byteBufferCreator;

        for (int i = 0; i < capacity; i++) {
            emptyByteBuffers.add(byteBufferCreator.get());
        }
    }

    /**
     * Gets available instance. If there is no more instances available
     * it will return empty optional.
     *
     * @return optional instance of ByteBuffer
     */
    public Optional<ByteBuffer> getNew() {
        var byteBuffer = emptyByteBuffers.poll();

        if (byteBuffer == null) {
            return Optional.empty();
        }

        byteBuffer.clear();
        return Optional.of(byteBuffer);
    }

    /**
     * Clears instance and puts it into available instance queue.
     *
     * @param buffer instance to release from use
     */
    public void release(ByteBuffer buffer) {
        buffer.clear();
        emptyByteBuffers.add(buffer);
    }
}