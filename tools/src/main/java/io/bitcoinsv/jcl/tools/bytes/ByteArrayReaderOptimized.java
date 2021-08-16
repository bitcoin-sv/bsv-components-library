package io.bitcoinsv.jcl.tools.bytes;

import io.bitcoinj.core.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
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

    private static final Logger log = LoggerFactory.getLogger(ByteArrayReaderOptimized.class);
    private int BUFFER_SIZE = 1_000_000;
    //private int BUFFER_SIZE = 50;
    private byte[] buffer = new byte[BUFFER_SIZE];
    private int bufferDataSize = 0;
    private int bytesConsumed = 0;

    public ByteArrayReaderOptimized(ByteArray byteArray) {
        super(byteArray);
    }

    public ByteArrayReaderOptimized(ByteArrayReader byteArrayReader) {
        super(byteArrayReader.byteArray);
    }

    public ByteArrayReaderOptimized(ByteArrayWriter byteArrayWriter) {
        super(byteArrayWriter.buffer);
    }

    public ByteArrayReaderOptimized(byte[] initialData) {
        super(initialData);
    }

    public void refreshBuffer() throws IOException {
        // We extract from the buffer the bytes already consumed
        //log.trace("Before refreshing buffer. Buffer: " + bytesConsumed + " consumed, " + bufferDataSize + " Total. Builder size: " + byteArray.size());
        super.byteArray.extract(bytesConsumed);

        // We fill the buffer with content fom the byteArray
        byte[] bytesToAddBuffer = super.byteArray.get((int) Math.min(buffer.length, super.byteArray.size()));
        System.arraycopy(bytesToAddBuffer, 0, buffer, 0, bytesToAddBuffer.length);
        bufferDataSize = bytesToAddBuffer.length;
        bytesConsumed = 0;
        //log.trace("After refreshing buffer. Buffer: " + bytesConsumed + " consumed, " + bufferDataSize + " Total (" + bytesToAddBuffer.length + " bytes added from Builder. Builder size: " + byteArray.size());
    }

    private void resetBuffer() {
        bufferDataSize = 0;
        bytesConsumed = 0;
    }

    private void adjustBufferIfNeededForReading(int length) {
        try {
            //log.trace("checking if we can read " + length + " bytes...");
            if ((bufferDataSize - bytesConsumed) < length)
                refreshBuffer();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public long readUint32() {
        adjustBufferIfNeededForReading(4);
        long result= Utils.readUint32(buffer, bytesConsumed);
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
        long result = Utils.readInt64(buffer, bytesConsumed);
        bytesConsumed += 8;
        return result;
    }

    public boolean readBoolean()            { return (read() != 0); }
    public long size()                      { return byteArray.size() - bytesConsumed;}
    public boolean isEmpty()                { return size() == 0;}
    public void closeAndClear()             { byteArray.clear();}

    public byte[] getFullContent() {
        super.byteArray.extract(bytesConsumed);
        return byteArray.get();
    }

    @Override
    public byte[] read(int length) {
        byte[] result = new byte[length];
        if ((buffer.length) >= length) {
            adjustBufferIfNeededForReading(length);
            System.arraycopy(buffer, bytesConsumed, result, 0, length);
            bytesConsumed += length;
        } else {
            super.byteArray.extract(bytesConsumed);
            result = super.read(length);
            // WE reset the buffe rto Zero
            bufferDataSize = 0;
            bytesConsumed = 0;
            return result;
        }
        return result;
    }

    @Override
    public byte[] get(int length) {
        byte[] result = new byte[length];
        if ((buffer.length) >= length) {
            adjustBufferIfNeededForReading(length);
            System.arraycopy(buffer, bytesConsumed, result, 0, length);
        } else {
            result = super.get(length);
            resetBuffer();
            return result;
        }
        return result;
    }

    public byte[] getFullContentAndClose() {
        super.byteArray.extract(bytesConsumed);
        byte[] result = byteArray.get();
        closeAndClear();
        return result;
    }
}
