package com.nchain.jcl.tools.bytes;

import io.bitcoinj.core.Utils;

import java.io.UnsupportedEncodingException;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This class allows for adding data into a ByteArray, and also converts the data from useful representations, like
 * unsigned integers, etc.
 *
 * The result of all the data written can be accessed by the "reader()" method, which wraps up the content into a
 * ByteReader for consuming.
 *
 * NOTE: It's possible to call "reader()" to get a ByteReader, and keep using this writer to write data, while other
 * process might be reading data from it using the reader. You just need to make sure that the writer has written
 * enough data for the reader to read otherwise the reader will throw an exception if there is nothing to read.
 */
public class ByteArrayWriter {

    protected ByteArrayBuffer buffer;

    public ByteArrayWriter() {
        this.buffer = new ByteArrayBuffer();
    }

    public void write(byte data)            { buffer.addBytes(new byte[] {data}); }
    public void write(byte[] data)          { buffer.addBytes(data);}
    public void writeUint32LE(long value)   {
        byte[] out = new byte[4];
        Utils.uint32ToByteArrayLE(value, out, 0);
        buffer.addBytes(out);
    }
    public void writeUint64LE(long value)   {
        byte[] out = new byte[8];
        Utils.uint64ToByteArrayLE(value, out, 0);
        buffer.addBytes(out);
    }
    public void writeBoolean(boolean value) { buffer.addBytes(new byte[] {(byte) (value? 1 : 0)});}
    public void writeStr(String str)        { writeStr(str, str.length()); }
    public ByteArrayReader reader()         { return new ByteArrayReader(buffer);}
    public void close()                     { buffer.clear();}

    public void writeStr(String str, int length) {
        byte[] strBytes = str.getBytes();
        byte[] fieldToWrite = new byte[length];
        System.arraycopy(strBytes, 0, fieldToWrite,0, Math.min(str.length(), length));
        write(fieldToWrite);
    }

    public void writeStr(String str, String charset) {
        try {
            byte[] strBytes = str.getBytes(charset);
            write(strBytes);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
