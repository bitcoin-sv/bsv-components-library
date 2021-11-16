package com.nchain.jcl.tools.bytes;

import io.bitcoinj.core.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.time.Duration;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * <p>
 * This class allows for reading data out of a ByteArray, and automatically converts the data into useful
 * representations, like unsigned Integers, etc.
 * <p>
 * The data is read from the beginning of the ByteArray (from the left), and once its read the data is consumed
 * (you can also use the "get()" methoid, which reads but NOT consume the data).
 */
public class ByteArrayReader {

    private static final Logger log = LoggerFactory.getLogger(ByteArrayReader.class);

    protected ByteArray byteArray;
    protected long bytesReadCount = 0; // Number of bytes read....

    public ByteArrayReader(ByteArrayReader reader) {
        this(reader.byteArray);
    }

    public ByteArrayReader(ByteArrayWriter writer) {
        this(writer.buffer);
    }

    public ByteArrayReader(byte[] initialData) {
        this(new ByteArrayBuffer(initialData));
    }

    public ByteArrayReader(ByteArray byteArray) {
        this.byteArray = byteArray;
    }

    public byte[] read(int length) {
        byte[] result = byteArray.extract(length);
        bytesReadCount += length;
        return result;
    }

    public byte[] get(int length)               { return byteArray.get(length); }
    public byte[] get(long offset, int length)  { return byteArray.get(offset, length); }
    public long readUint32()                    { return Utils.readUint32(read(4), 0); }
    public long readUint64()                    { return Utils.readUint64(read(8), 0); }
    public byte read()                          { return read(1)[0]; }
    public int readUint16()                     { return Utils.readUint16(read(2), 0); }
    public long readInt64LE()                   { return Utils.readInt64(read(8), 0); }
    public long readInt48LE()                   { return Utils.readInt48(read(6), 0); }
    public boolean readBoolean()                { return (read() != 0); }
    public long size()                          { return byteArray.size(); }
    public boolean isEmpty()                    { return byteArray.size() == 0; }
    public void closeAndClear()                 { byteArray.clear(); }
    public byte[] getFullContent()              { return byteArray.get(); }
    public long getBytesReadCount()             { return bytesReadCount; }
    public long getUint32(int offset)                 { return Utils.readUint32(get(offset, 4), 0);}
    public long getInt64LE(int offset)                 { return Utils.readInt64(get(offset, 8), 0); }

    public byte[] getFullContentAndClose() {
        byte[] result = byteArray.get();
        closeAndClear();
        bytesReadCount += result.length;
        return result;
    }

    // Performs a Default "trim" on the String, removing spaces from beginning and end.
    public String readString(int length, String charset) {
       String result = readStringNoTrim(length, charset);
       return result.trim();
    }

    public String readStringNoTrim(int length, String charset) {
        try {
            String result = new String(read(length), charset);
            return result;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
