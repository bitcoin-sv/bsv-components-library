package com.nchain.jcl.base.tools.bytes;

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

    protected ByteArrayBuilder builder;

    public ByteArrayWriter() {
        this.builder = new ByteArrayBuilder();
    }


    public void write(byte data)            { builder.add(new byte[] {data}); }
    public void write(byte[] data)          { builder.add(data);}
    public void writeUint32LE(long value)   { builder.add(ByteTools.uint32ToByteArrayLE(value)); }
    public void writeUint64LE(long value)   { builder.add(ByteTools.uint64ToByteArrayLE(value)); }
    public void writeBoolean(boolean value) { builder.add(new byte[] {(byte) (value? 1 : 0)});}
    public void writeStr(String str)        { writeStr(str, str.length()); }
    public ByteArrayReader reader()         { return new ByteArrayReader(builder);}
    public void close()                     { builder.clear();}

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
