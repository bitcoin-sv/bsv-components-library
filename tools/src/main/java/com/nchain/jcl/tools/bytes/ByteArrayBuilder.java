package com.nchain.jcl.tools.bytes;

import javax.annotation.concurrent.GuardedBy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-03-08 21:06
 *
 * This class allows for creating a Byte Array content with unlimited size. It manages an internal structure (a buffer),
 * where each item is a ByteArray Implementation. This items will be a NIO(memory)-based implementation.
 */
public class ByteArrayBuilder {
    private ByteArrayMemoryConfiguration memoryConfig;

    protected List<ByteArray> buffers = Collections.synchronizedList(new ArrayList<ByteArray>());

    public ByteArrayBuilder(){
        this.memoryConfig = ByteArrayMemoryConfiguration.builder().build();
    }

    public ByteArrayBuilder(ByteArrayMemoryConfiguration memoryConfig) {
        this.memoryConfig = memoryConfig;
    }


    // Adds a new buffer to the list and returns it
    private ByteArray addBuffer() {
        buffers.add(new ByteArrayImpl(memoryConfig.getByteArraySize()));

        return buffers.get(buffers.size() - 1);
    }

    /** Adds a byte Array to the end of the current data */
    @GuardedBy("this")
    public ByteArrayBuilder add(byte[] data) {
        int bytesRemaining = data.length;
        if (buffers.size() == 0) addBuffer();
        while (bytesRemaining > 0) {

            // The data is added in the last buffer. if the buffer is already full, we create another one and that one
            // will become the "new" last one. If the last buffer still has free capacity, we add as many bytes as
            // possible and leave the rest for the next iteration.

            ByteArray buffer = buffers.get(buffers.size()-1);
            if (buffer.available() == 0)
                buffer = addBuffer();

            int writeLength = (int) ((buffer.available() >= bytesRemaining) ?  bytesRemaining : buffer.available());
            buffer.add(data, data.length - bytesRemaining, writeLength);
            bytesRemaining -= writeLength;
        }
        return this;
    }

    /** Adds a single byte to the end of the data */
    @GuardedBy("this")
    public ByteArrayBuilder add(ByteArray byteArray) {
        buffers.add(byteArray);
        return this;
    }

    /**
     * "Extracts" data from the beginning and wraps it up in a ByteArrayReader for further consumption. After this
     * operation, the data size will be reduced in "length" bytes.
     * If we try to extractReader more bytes than stored in the Builder it will throw an Exception
     * */
    @GuardedBy("this")
    public ByteArrayReader extractReader(long length, ByteArrayMemoryConfiguration memoryConfig) {
        checkArgument(size() >= length,
                "trying to extractReader too many bytes, current: " + size() + ", requested: " + length);

        // ByteArraySize to use when using another Builder:
        int byteArraySize = memoryConfig.getByteArraySize();

        // We build the result with another Builder:
        ByteArrayBuilder writeBuilder = new ByteArrayBuilder(memoryConfig);

        // For those buffers that need to be removed from this buffer
        List<ByteArray> buffersToRemove = new ArrayList<>();

        long bytesRemaining = length;
        int index = 0;
        while (bytesRemaining > 0) {
            ByteArray buffer = buffers.get(index);

            // We process this Buffer. If the whole Buffer fits into the result, we just add it to the result. Later
            // on, after the loop is over, the place this buffer was taking int he original buffer will be removed.
            // but if this Buffer has MORE info than we need, then we just extract what we need...
            long numBytesToExtractFromThisBuffer = (buffer.size() <= bytesRemaining)
                    ? buffer.size() : bytesRemaining;

            if (buffer.size() == numBytesToExtractFromThisBuffer) {
                writeBuilder.add(buffer);
                buffersToRemove.add(buffer);
            } else {
                // We are only taking part of this buffer. In order not to throw an OutOfMemory error, we extract the
                // bytes in blocks of a "safe" size...
                long numBytesLeftToExtract = numBytesToExtractFromThisBuffer;
                while (numBytesLeftToExtract > 0) {
                    int numBytesToExtractNow = (int) Math.min(byteArraySize, numBytesLeftToExtract);
                    byte[] bytesExtracted = buffer.extract(numBytesToExtractNow);
                    writeBuilder.add(bytesExtracted);
                    numBytesLeftToExtract -= numBytesToExtractNow;
                } // while...
            }

            bytesRemaining -= numBytesToExtractFromThisBuffer;
            index++;
        }
        // We remove those ByteArrays that are now empty after the extraction...
        buffers.removeAll(buffersToRemove);

        return new ByteArrayReader(writeBuilder);
    }

    @GuardedBy("this")
    public ByteArrayReader extractReader(long length) {
        return this.extractReader(length, memoryConfig);
    }

    @GuardedBy("this")
    public ByteArrayReader extractReader() {
        return new ByteArrayReader(this);
    }

    /**
     * "Extracts" data from the beginning and returns it as a byteArray. After this
     * operation, the data size will be reduced in "length" bytes.
     * Since we are returning a whole byte[], we control that we are extracting a "safe" number of bytes.
     * If we try to extractReader more bytes than stored in the Builder it will throw an Exception
     * */
    @GuardedBy("this")
    public byte[] extractBytes(int length) {
        checkArgument(size() >= length,
                "trying to extract too many bytes, current: " + size() + ", requested: " + length);

        ByteArray result = new ByteArrayImpl(length);

        // For removing empty buffers after the extraction:
        List<ByteArray> buffersToRemove = new ArrayList<>();

        long bytesRemaining = length;
        int indexBuffer = 0;
        while (bytesRemaining > 0) {
            ByteArray buffer = buffers.get(indexBuffer);

            int bytesToWriteLength = (int) ((buffer.size() >= bytesRemaining) ? bytesRemaining : (buffer.size()));
            byte[] bufferBytes = buffer.extract(bytesToWriteLength);
            result.add(bufferBytes);

            // We prepare for next iteration. if this buffer has been emptied, we store if for future cleaning...
            if (buffer.isEmpty()) buffersToRemove.add(buffer);
            bytesRemaining -= bytesToWriteLength;
            indexBuffer++;
        }
        // We remove those ByteArrays that are now empty after the extraction...
        buffersToRemove.forEach(b -> b.destroy());
        buffers.removeAll(buffersToRemove);

        return result.get();
    }

    /** Returns the number of bytes stored */
    public long size() {
        return buffers.stream().mapToLong(b -> b.size()).sum();
    }

    /**
     * Cleans the data. For the data stored in Memory, it
     * becomes eligible for the Garbage Collector, so it will be eventually cleaned as well.
     */
    public void clear() {
        buffers.forEach(b -> b.destroy());
        buffers.clear();
    }

    /**
     * It returns a piece of data from the builder, but it does NOT consume it. In order not to risk running
     * OutOfMemory, we check that the max size we are extracting is BYTE_ARRAY_SIZE.
     * @param length    length of the data
     */
    public byte[] get(int length) {
        checkArgument(length <= this.size(),
                " trying to extractReader too much data (not enough in the builder)");

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

    /**
     * Returns the Full Content of the Builder.
     */
    protected byte[] getFullContent() {
        ByteArray result =  new ByteArrayImpl((int) this.size());
        for (int i = 0; i < buffers.size(); i++) {
            ByteArray bufferItem = buffers.get(i);
            byte[] bufferItemContent = bufferItem.get();
            result.add(bufferItemContent);
        }
        return result.get();
    }

    @GuardedBy("this")
    public void updateMemoryConfig(ByteArrayMemoryConfiguration memoryConfig) {
        this.memoryConfig = memoryConfig;
    }
}
