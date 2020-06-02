package com.nchain.jcl.tools.bytes;

import java.io.UnsupportedEncodingException;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-03-08 21:24
 *
 * This class allows for reading data out of a ByteArray, and automatically converts the data into useful
 * representations, like unsigned Integers, etc.
 *
 * IMPORTANT: The Reader CONSUMES the data as it reads them, so its not possible to use the same Reader twice.
 */
public class ByteArrayReader {

    public ByteArrayBuilder builder; // TODO :Move it back to protected
    protected long bytesReadCount = 0; // Number of bytes read....

    public ByteArrayReader(ByteArrayBuilder builder)    { this(builder, null); }
    public ByteArrayReader(ByteArrayWriter writer)      { this(writer.builder, null);}

    public ByteArrayReader(byte[] initialData) {
        this(new ByteArrayBuilder(), initialData);
    }

    public ByteArrayReader(ByteArrayBuilder builder, byte[] initialData) {
        this.builder = builder;
        if (initialData != null) this.builder.add(initialData);
    }

    public byte[] read(int length) {
        byte[] result = builder.extractBytes(length);
        bytesReadCount += length;
        return result;
    }

    public byte[] extract(int length)       { return builder.extractBytes(length); }
    public long readUint32()                { return ByteTools.readUint32(read(4)); }
    public byte read()                      { return read(1)[0]; }
    public long readInt64LE()               { return ByteTools.readInt64LE(read(8)); }
    public boolean readBoolean()            { return (read() != 0);}
    public long size()                      { return builder.size();}
    public boolean isEmpty()                { return builder.size() == 0;}
    public void close()                     { builder.clear();}
    public byte[] getFullContent()          { return builder.getFullContent();}
    public long getBytesReadCount()         { return bytesReadCount;}

    public byte[] getFullContentAndClose() {
        byte[] result = builder.getFullContent();
        close();
        return result;
    }

    public String readString(int length, String charset) {
        try {
            String result = new String(read(length), charset);
            return result.trim();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
