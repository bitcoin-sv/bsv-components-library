package com.nchain.jcl.tools.bytes;

import java.io.IOException;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2009-2010 Satoshi Nakamoto
 * Copyright (c) 2009-2016 The Bitcoin Core developers
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-04-04 13:08
 *
 * TODO: Temporary class, for Testing and checking performance.
 */
public class ByteArrayReaderOptimizedTemp extends ByteArrayReader {

    private int BUFFER_SIZE = 500_000;
    private ByteArrayImpl buffer = new ByteArrayImpl(BUFFER_SIZE, true);
    private int bytesConsumed = 0;

    public ByteArrayReaderOptimizedTemp(ByteArrayReader reader) {
        super(reader.builder);
    }

    private void refreshBuffer() throws IOException {
        // We extract from the buffer the bytes already consumed
        super.builder.extractBytes(bytesConsumed);
        // We extract from the Builder the bytes already in the buffer
        super.builder.extractBytes(buffer.dataSize);
        // We fill the rest of the buffer with content from the builder
        byte[] bytesToAddBuffer = super.builder.get((int) Math.min(buffer.remaining, super.builder.size()));
        buffer.add_bytes(bytesToAddBuffer, buffer.dataSize, bytesToAddBuffer.length);
        bytesConsumed = 0;
    }

    @Override
    public byte[] read(int length) {
        try {
            if (buffer.capacity >= length) {
                if (buffer.dataSize < length) refreshBuffer();
                bytesConsumed += length;
                return buffer.extract_bytes(length);
            } else {
                super.builder.extractBytes(bytesConsumed);
                byte[] result = super.read(length);
                buffer.extract_bytes(buffer.dataSize);
                bytesConsumed = 0;
                return result;
            }
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    @Override
    public byte[] readWithDanger(int length) {
        try {
            super.builder.extractBytes(bytesConsumed);
            byte[] result = super.readWithDanger(length);
            buffer.extract_bytes(buffer.dataSize);
            bytesConsumed = 0;
            return result;
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public byte[] getFullContentAndClose() {
        super.builder.extractBytes(bytesConsumed);
        byte[] result = builder.getFullContent();
        close();
        return result;
    }
}
