package com.nchain.jcl.tools.bytes;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An abstraction over a regular Byte Array but with limited functionality. It allows to "add" data at
 * the end (right), extract data from the beginning (left) and other utility functions.
 *
 * Different implementations may store the data differently.
 *
 */
public interface ByteArray {
    long capacity();
    long size();
    long available();
    boolean isEmpty();
    void add(byte[] data);
    void add(byte[] data, int offset, int length);
    byte[] get(long offset, int length);
    byte[] get(int length);
    byte[] get();
    byte[] extract(int length);
    void extractInto(int length, byte[] array, int writeOffset);
    void init();    // initialization operations (if needed)
    void clear(); // cleaning operations. No data available after this
}
