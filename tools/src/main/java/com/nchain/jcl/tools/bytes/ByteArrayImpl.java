package com.nchain.jcl.tools.bytes;


import lombok.Getter;
import lombok.Setter;

import javax.annotation.concurrent.GuardedBy;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-03-07 22:19
 *
 * This class is the implementation for a NIO ByteArray.
 *
 */
public class ByteArrayImpl implements ByteArray {

    @Getter @Setter protected ByteBuffer buffer;
    protected int capacity;
    protected int dataSize;
    protected int remaining;


    public ByteArrayImpl(int capacity) {
        this.capacity = capacity;
        this.buffer = ByteBuffer.allocateDirect(capacity);
        this.dataSize = buffer.position();
        this.remaining = buffer.remaining();
    }

    @GuardedBy("this")
    @Override
    public void add(byte[] data, int offset, int length) {
        checkArgument(length >= 0, "'length' must be >= 0");
        checkArgument((offset >= 0) && ((length == 0) || (length > 0 && offset < data.length)),
                " 'offset' has a wrong value, must be in the range[" + 0 + "," + data.length + "] "
                        + "but got " + offset + " instead");
        checkArgument((capacity() - size()) >= length,
                "Not enough capacity left in the buffer, capacity left: "
                        + (capacity() - size()) + " , requested: " + length + " bytes");
        try {
            add_bytes(data, offset, length);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    @GuardedBy("this")
    @Override
    public byte[] get(int offset, int length) {
        checkArgument(offset >= 0, "wrong value for 'pos'");
        checkArgument(length >= 0, "'length' must be >= zero");
        checkArgument(size() >= (offset + length),
                "not enough data in the buffer: actual data: " + size() + " bytes,"
                        + " , requested: " + length + " bytes");
        try {
            return get_bytes(offset, length);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    @GuardedBy("this")
    @Override
    public byte[] extract(int length) {
        checkArgument(length >= 0, "'length' must be >= 0");
        checkArgument(size() >= length,
                "not enough data in the buffer: actual data: " + size() + " bytes,"
                        + " , requested: " + length + " bytes");
        try {
            return extract_bytes(length);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    @Override
    public void init() {

    }


    @GuardedBy("this")
    @Override
    public void destroy() {
        try {
            destroy_content();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    @GuardedBy("this")
    @Override
    public void add(byte[] data) {
        this.add(data, 0 ,data.length);
    }

    @GuardedBy("this")
    @Override
    public byte[] get() {
        return get(0, (int) size());
    }


    public void add_bytes(byte[] data, int offset, int length) throws IOException {
        buffer.position(dataSize).limit(capacity).put(data, offset, length);
        remaining = buffer.remaining();
        dataSize = buffer.position();
    }

    public byte[] get_bytes(int offset, int length) throws IOException {
        byte[] result = new byte[length];
        buffer.position(offset).limit(dataSize).get(result, 0, length);
        return result;
    }

    public byte[] extract_bytes(int length) throws IOException {
        byte[] result = get(0, length);
        buffer.compact();
        remaining += length;
        dataSize -= length;
        return result;
    }

    public void destroy_content() throws IOException {
        buffer = null;
    }

    public long size()         { return dataSize; }

    public long available()    { return remaining; }

    public long capacity()     { return capacity; }

    public boolean isEmpty()  { return dataSize == 0; }
}
