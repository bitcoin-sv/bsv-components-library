package com.nchain.jcl.tools.bytes;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-03-03 15:51
 *
 * An abstraction over a regular Byte Array but with limited functionality. It allows to "add" data at
 * the end (right), extractReader data from the beginning (left) and random access.
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
    byte[] get(int offset, int length);
    byte[] get();
    byte[] extract(int length);
    void init();    // initialization operations
    void destroy(); // cleaning operations. No data available after this
}
