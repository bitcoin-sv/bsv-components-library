package io.bitcoinsv.bsvcl.tools.bytes;

import static com.google.common.base.Preconditions.*;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * @date 23/07/2021
 *
 * Implementation of ByteArray backed by a regular/static byte array.
 * THIS IMPLEMENTATION HAS BEEN CREATED FOR PERFORMANCE REASONS ONLY. IT ALLOWS FOR READING DATA BUT IT DOES NOT
 * ALLOW TO ADD OR EXTRACT DATA FROM IT.
 *
 */
public class ByteArrayStatic implements ByteArray {

    private static final byte[] EMPTY_ARRAY = new byte[0];
    private byte[] content;
    private int index = 0;

    /** Constructor */
    public ByteArrayStatic(byte[] content) {
        this.content = content;
    }

    @Override public long capacity()        { return content.length;}
    @Override public long size()            { return content.length - index;}
    @Override public long available()       { return 0;}
    @Override public boolean isEmpty()      { return (size() > 0);}
    @Override public void init()            {}
    @Override public void clear()           { content = EMPTY_ARRAY;}
    @Override public byte[] get(int length) { return get(0, length);}
    @Override public byte[] get()           { return content;}

    @Override public byte[] get(long offset, int length) {
        checkArgument(offset > 0 && ((offset + length) < content.length), "Trying to get too much bytes");
        byte[] result = new byte[length];
        System.arraycopy(this.content, (int) offset, result, 0, length);
        return result;
    }

    // This implementation is immutable, so adding/extracting bytes is not allowed
    @Override public void add(byte[] data)                          { throw new UnsupportedOperationException();}
    @Override public void add(byte[] data, int offset, int length)  { throw new UnsupportedOperationException(); }
    @Override public byte[] extract(int length)                     { throw new UnsupportedOperationException(); }
    @Override public void discard(int length)                       { throw new UnsupportedOperationException(); }
    @Override public void extractInto(int length, byte[] array, int writeOffset) { throw new UnsupportedOperationException(); }
}
