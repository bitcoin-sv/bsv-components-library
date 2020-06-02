package com.nchain.jcl.tools.bytes;

import java.io.IOException;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-04-04 13:08
 *
 * This reader is an optimization over the regular ByteArrayReader.
 * The regular ByteArrayReader is fine for regular/medium amounts of data. But most of its operations involve
 * a call to System.arrayCopy(), which might be time-consuming when we need to invoke it several hundred thousands
 * or millions of times (like when we deserialize a Big block).
 *
 * So the point of this Optimized Reader is to reduce the calls to System.arrayCopy: In the regular Reader, all the
 * basic operations for reading an int, float, boolean, etc, involve the creation of a new Array as an intermediate
 * step in ordet ot come uip with the result.
 *
 * In this optimized implementation, we do not crete intermediate arrays for each basic operation. Instead, we use
 * a BUFFER big enough (1MB), which we fill with the info we need. Every time we need to extract an Int, long, etc, we
 * just go directly to the buffer and access directly the positions within the array we need (we keep tack of
 * an index, in order to know where to read next). When the Buffer as been read completeluy, we just need to refresh
 * it, moving content from the regular Builder we still have and into the Buffer. But since the buffer is big enough,
 * this refresh should happen not very frequently.
 */
public class ByteArrayReaderOptimized extends ByteArrayReader {

    private int BUFFER_SIZE = 1_000_000;
    //private int BUFFER_SIZE = 50;
    private byte[] buffer = new byte[BUFFER_SIZE];
    private int bufferDataSize = 0;
    private int bytesConsumed = 0;

    public ByteArrayReaderOptimized(ByteArrayReader reader) {
        super(reader.builder);
    }

    private void refreshBuffer() throws IOException {
        // We extract from the buffer the bytes already consumed
        super.builder.extractBytes(bytesConsumed);

        // We fill the buffer with content fom the builder
        byte[] bytesToAddBuffer = super.builder.get((int) Math.min(buffer.length, super.builder.size()));
        System.arraycopy(bytesToAddBuffer, 0, buffer, 0, bytesToAddBuffer.length);
        bufferDataSize = bytesToAddBuffer.length;
        bytesConsumed = 0;
    }

    private void resetBuffer() {
        bytesConsumed = 0;
        bufferDataSize = 0;
    }

    @Override
    public byte[] extract(int length) {
        resetBuffer();
        return builder.extractBytes(length);
    }

    private void adjustBufferIfNeededForReading(int length) {
        try {
            if ((bufferDataSize - bytesConsumed) < length)
                refreshBuffer();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public long readUint32() {
        adjustBufferIfNeededForReading(4);
        long result= ByteTools.readUint32(buffer, bytesConsumed);
        bytesConsumed += 4;
        return result;
    }

    public byte read() {
        adjustBufferIfNeededForReading(1);
        byte result = buffer[bytesConsumed];
        bytesConsumed+= 1;
        return result;
    }

    public long readInt64LE() {
        adjustBufferIfNeededForReading(8);
        long result = ByteTools.readInt64LE(buffer, bytesConsumed);
        bytesConsumed += 8;
        return result;
    }

    public boolean readBoolean()            { return (read() != 0); }
    public long size()                      { return builder.size() - bytesConsumed;}
    public boolean isEmpty()                { return size() == 0;}
    public void close()                     { builder.clear();}
    public byte[] getFullContent() {
        super.builder.extractBytes(bytesConsumed);
        return builder.getFullContent();
    }

    @Override
    public byte[] read(int length) {
        byte[] result = new byte[length];
        if ((buffer.length) >= length) {
            adjustBufferIfNeededForReading(length);
            System.arraycopy(buffer, bytesConsumed, result, 0, length);
            bytesConsumed += length;
        } else {
            super.builder.extractBytes(bytesConsumed);
            result = super.read(length);
            bufferDataSize = 0;
            bytesConsumed = 0;
            return result;
        }
        return result;
    }

    public byte[] getFullContentAndClose() {
        super.builder.extractBytes(bytesConsumed);
        byte[] result = builder.getFullContent();
        close();
        return result;
    }
}
