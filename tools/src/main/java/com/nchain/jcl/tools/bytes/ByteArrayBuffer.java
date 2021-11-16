package com.nchain.jcl.tools.bytes;


import javax.annotation.concurrent.GuardedBy;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;


import static com.google.common.base.Preconditions.checkArgument;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This class allows for creating a Byte Array content with unlimited size. It manages an internal structure (a buffer),
 * a collection where each item is a ByteArray Implementation (byteArrayNIO implementation).
 *
 * This class also implements the ByteArray inrterface, so once the content has been fed, it can be used as a
 * regular ByteArray.
 *
 * NOTE: This class is NOT Thread-Safe. At this moment, operformance is the highest concern, so making it Thread-Safe
 * is on our list but it will taken up only if needed, reason geing: This class is massively used by the Streams open
 * with each Remorte Peer:
 *  - The remote peer sends data to us, and that data is ADDED to this class (to the end, it's right side)
 *  - We read the data, socnuming bytes from the beginning (left side)
 *
 *  The two operations above are happening at the same time and at incredible high rate (several thousands per sec). I'm
 *  afraid that if I add lock management to these oeprations, that will affect performance. In the real scenarios, the
 *  data is written ont he left and red fromt he right, so the only problme will come if we try to read data is still not
 *  there, but we always verify that before reading, so there shouldn't be an issue.
 */
public class ByteArrayBuffer implements ByteArray {

    @GuardedBy("this") private ByteArrayConfig config;
    @GuardedBy("this") private List<ByteArray> buffers = new ArrayList<>();

    // For Performance sake:
    AtomicLong size = new AtomicLong();
    AtomicLong capacity = new AtomicLong();
    AtomicLong available = new AtomicLong();


    public ByteArrayBuffer(){
        this.config = new ByteArrayConfig();
    }

    public ByteArrayBuffer(ByteArrayConfig memoryConfig) {
        this.config = memoryConfig;
    }

    public ByteArrayBuffer(byte[] initialData, ByteArrayConfig memoryConfig) {
        this.config = memoryConfig;
        add(initialData);
    }
    public ByteArrayBuffer(byte[] initialData) {
        this(initialData, new ByteArrayConfig());
    }

    public ByteArrayBuffer(int initialSize, ByteArrayConfig memoryConfig) {
        this.config = memoryConfig;
        addBuffer(initialSize);
    }

    // Adds a new buffer to the list and returns it
    private synchronized ByteArray addBuffer(int size) {
        ByteArray byteArray = new ByteArrayNIO(size);
        buffers.add(byteArray);

        // Performance counters:
        capacity.addAndGet(byteArray.capacity());
        available.addAndGet(byteArray.available());
        return buffers.get(buffers.size() - 1);
    }

    /** Initialization (we do nothing here) */
    public void init() {}

    /** Adds a byte Array to the end of the current data */
    public synchronized void add(byte[] data) {
        int bytesRemaining = data.length;
        if (buffers.size() == 0) addBuffer(config.getByteArraySize());
        while (bytesRemaining > 0) {

            // The data is added in the last buffer. if the buffer is already full, we create another one and that one
            // will become the "new" last one. If the last buffer still has free capacity, we addBytes as many bytes as
            // possible and leave the rest for the next iteration.

            ByteArray buffer = buffers.get(buffers.size()-1);
            if (buffer.available() == 0)
                buffer = addBuffer(config.getByteArraySize());

            int writeLength = (int) ((buffer.available() >= bytesRemaining) ?  bytesRemaining : buffer.available());
            buffer.add(data, data.length - bytesRemaining, writeLength);
            bytesRemaining -= writeLength;
        }
        size.addAndGet(data.length);

    }

    /** Adds a byte Array at the specific location */
    public synchronized void add(byte[] data, int offset, int length) {
        throw new UnsupportedOperationException("Not supported at the moment");
    }

    /**
     * "Extracts" data from the beginning and returns it as a byteArray. After this
     * operation, the data size will be reduced in "length" bytes.
     * If we try to extractReader more bytes than stored in the Builder it will throw an Exception
     * */
    public synchronized byte[] extract(int length) {
        byte[] result = new byte[length];

        extractInto(length, result, 0);

        return result;
    }


    @Override
    public void extractInto(int length, byte[] array, int writeOffset) {
        checkArgument(size() >= length,
                "trying to extract too many bytes, current: " + size() + ", requested: " + length);

        // For removing empty buffers after the extraction:
        List<ByteArray> buffersToRemove = new ArrayList<>();

        int bytesRemaining = length;
        int indexBuffer = 0;
        while (bytesRemaining > 0) {
            ByteArray buffer = buffers.get(indexBuffer);

            int bytesToWriteLength = (int) ((buffer.size() >= bytesRemaining) ? bytesRemaining : (buffer.size()));

            buffer.extractInto(bytesToWriteLength, array, length - bytesRemaining);

            // We prepare for next iteration. if this buffer has been emptied, we store if for future cleaning...
            if (buffer.isEmpty()) buffersToRemove.add(buffer);
            bytesRemaining -= bytesToWriteLength;
            indexBuffer++;
        }
        // We remove those ByteArrays that are now empty after the extraction...
        buffersToRemove.forEach(b -> b.clear());
        buffers.removeAll(buffersToRemove);

        size.addAndGet(-length);
        capacity.set(buffers.stream().mapToLong(b -> b.capacity()).sum());
        available.set(buffers.stream().mapToLong(b -> b.available()).sum());
    }

    /** Returns the number of bytes stored */
    //public long size() { return buffers.stream().mapToLong(b -> b.size()).sum(); }
    public long size() { return size.get(); }

    /** Returns the remaining capacity (if more bytes are added, this number might change */
    //public long available() { return buffers.stream().mapToLong(b -> b.available()).sum(); }
    public long available() { return available.get(); }

    /** Returns the total capacity */
    //public long capacity() {return buffers.stream().mapToLong(b -> b.capacity()).sum();}
    public long capacity() {
        return capacity.get();
    }

    /** Indicates if there are no bytes at all */
    public boolean isEmpty() { return (size() == 0);}

    /**
     * Cleans the data. For the data stored in Memory, it
     * becomes eligible for the Garbage Collector, so it will be eventually cleaned as well.
     */
    public synchronized void clear() {
        buffers.forEach(b -> b.clear());
        buffers.clear();
    }

    /**
     * It returns a piece of data from the buffer, but it does NOT consume it.
     * @param length    length of the data
     */
    public synchronized byte[] get(int length) {
        checkArgument(length <= this.size(),
                " trying to extractReader too much data (not enough in the byteArray)");

        byte[] result = new byte[length];
        long bytesRemaining = length;
        int index = 0;
        while (bytesRemaining > 0) {
            ByteArray buffer = buffers.get(index);
            long bytesToWriteLength = (buffer.size() >= bytesRemaining) ? bytesRemaining : (buffer.size());
            byte[] bytesToAdd = buffer.get(0, (int) bytesToWriteLength);
            System.arraycopy(bytesToAdd, 0, result, (int) (length - bytesRemaining), (int) bytesToWriteLength);
            bytesRemaining -= bytesToWriteLength;
            index++;
        }

        return result;
    }

    /** Returns a Byte Array starting at the given position with the given length */
    public synchronized byte[] get(int offset, int length) {
        checkArgument(length + offset <= this.size(),
                " trying to extractReader too much data (not enough in the byteArray)");

        byte[] result = new byte[length];
        int size = 0;
        int bytesRemaining = length;

        boolean initialBufferOffsetFound = false;
        int bufferOffset;
        for(ByteArray buffer : buffers){

            if(size + buffer.size() < offset){
                size += buffer.size();
                continue;
            }

            if(bytesRemaining == 0){
                break;
            }

            //we only need to offset the first buffer, the rest of the buffers data will be sequential
            if(initialBufferOffsetFound){
                bufferOffset = 0;
            } else {
                bufferOffset = offset - size;
                initialBufferOffsetFound = true;
            }

            long availableDataInBuffer = buffer.size() - bufferOffset;
            long bytesToWriteLength = (availableDataInBuffer >= bytesRemaining) ? bytesRemaining : availableDataInBuffer;
            byte[] bytesToAdd = buffer.get(bufferOffset, (int) bytesToWriteLength);
            System.arraycopy(bytesToAdd, 0, result, length - bytesRemaining, (int) bytesToWriteLength);
            bytesRemaining -= bytesToWriteLength;

        }

        return result;
    }

    /**
     * Returns the Full Content of the Buffer.
     */
    public synchronized byte[] get() {
        ByteArray result =  new ByteArrayNIO((int) this.size());
        for (int i = 0; i < buffers.size(); i++) {
            ByteArray bufferItem = buffers.get(i);
            byte[] bufferItemContent = bufferItem.get();
            result.add(bufferItemContent);
        }
        return result.get();
    }

    public synchronized void updateConfig(ByteArrayConfig memoryConfig) {
        this.config = memoryConfig;
    }
}
