package io.bitcoinsv.jcl.tools.bytes;

import io.bitcoinsv.bitcoinjsv.core.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;

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
public class ByteArrayReader implements IReader {

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

    @Override
    public byte[] read(int length) {
        byte[] result = byteArray.extract(length);
        bytesReadCount += length;
        return result;
    }

    @Override
    public byte[] get(int length)               { return byteArray.get(length); }
    @Override
    public byte[] get(long offset, int length)  { return byteArray.get(offset, length); }
    @Override
    public long readUint32()                    { return Utils.readUint32(read(4), 0); }
    @Override
    public long readInt32()                     { return Utils.readInt32(read(4), 0);}
    @Override
    public long readUint64()                    { return Utils.readUint64(read(8), 0); }
    @Override
    public byte read()                          { return read(1)[0]; }
    @Override
    public int readUint16()                     { return Utils.readUint16(read(2), 0); }
    @Override
    public long readInt64LE()                   { return Utils.readInt64(read(8), 0); }
    @Override
    public long readInt48LE()                   { return Utils.readInt48(read(6), 0); }
    @Override
    public boolean readBoolean()                { return (read() != 0); }
    @Override
    public long size()                          { return byteArray.size(); }
    @Override
    public boolean isEmpty()                    { return byteArray.size() == 0; }
    @Override
    public void closeAndClear()                 { byteArray.clear(); }
    @Override
    public byte[] getFullContent()              { return byteArray.get(); }
    @Override
    public long getBytesReadCount()             { return bytesReadCount; }
    @Override
    public long getUint32(int offset)                 { return Utils.readUint32(get(offset, 4), 0);}
    @Override
    public long getInt64LE(int offset)                 { return Utils.readInt64(get(offset, 8), 0); }

    @Override
    public byte[] getFullContentAndClose() {
        byte[] result = byteArray.get();
        closeAndClear();
        bytesReadCount += result.length;
        return result;
    }

    // Performs a Default "trim" on the String, removing spaces from beginning and end.
    @Override
    public String readString(int length, String charset) {
       String result = readStringNoTrim(length, charset);
       return result.trim();
    }

    @Override
    public String readStringNoTrim(int length, String charset) {
        try {
            return new String(read(length), charset);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
