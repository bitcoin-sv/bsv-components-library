package io.bitcoinsv.jcl.tools.bytes;

import io.bitcoinsv.bitcoinjsv.core.Utils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

/**
 * @author j.pomer@nchain.com
 * Copyright (c) 2018-2022 nChain Ltd
 * <p>
 * This class allows for reading data out of any InputStream, and automatically converts the data into useful
 * representations, like unsigned Integers, etc.
 */
public class InputStreamReader implements IReader, AutoCloseable {
    protected BufferedInputStream inputStream;

    public InputStreamReader(InputStream inputStream) {
        this.inputStream = new BufferedInputStream(inputStream);
    }

    @Override
    public byte[] read(int length) {
        try {
            return inputStream.readNBytes(length);
        } catch (IOException ignored) {
            return new byte[0];
        }
    }

    @Override
    public byte[] get(int length) {
        try {
            inputStream.mark(length);
            var result = inputStream.readNBytes(length);
            inputStream.reset();
            return result;
        } catch (IOException e) {
            throw new IllegalStateException("Couldn't read bytes!", e);
        }
    }

    @Override
    public byte[] get(long offset, int length) {
        inputStream.mark((int) offset + length);
        try {
            inputStream.readNBytes((int) offset);
            var result = inputStream.readNBytes(length);
            inputStream.reset();

            return result;
        } catch (IOException e) {
            throw new IllegalStateException("Couldn't read bytes!", e);
        }
    }

    @Override
    public long readUint32() {
        return Utils.readUint32(read(4), 0);
    }

    @Override
    public long readUint64() {
        return Utils.readUint64(read(8), 0);
    }

    @Override
    public byte read() {
        return read(1)[0];
    }

    @Override
    public int readUint16() {
        return Utils.readUint16(read(2), 0);
    }

    @Override
    public long readInt64LE() {
        return Utils.readInt64(read(8), 0);
    }

    @Override
    public long readInt48LE() {
        return Utils.readInt48(read(6), 0);
    }

    @Override
    public boolean readBoolean() {
        return (read() != 0);
    }

    @Override
    public long size() {
        try {
            return inputStream.available();
        } catch (IOException e) {
            throw new IllegalStateException("Couldn't read bytes!", e);
        }
    }

    @Override
    public boolean isEmpty() {
        try {
            return inputStream.available() == 0;
        } catch (IOException e) {
            throw new IllegalStateException("Couldn't read bytes!", e);
        }
    }

    @Override
    public void closeAndClear() {
        try {
            inputStream.close();
        } catch (IOException e) {
            throw new IllegalStateException("Couldn't read bytes!", e);
        }
    }

    @Override
    public byte[] getFullContent() {
        try {
            return inputStream.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Couldn't read bytes!", e);
        }
    }

    @Override
    public long getBytesReadCount() {
        return 0;
    }

    @Override
    public long getUint32(int offset) {
        return Utils.readUint32(get(offset, 4), 0);
    }

    @Override
    public long getInt64LE(int offset) {
        return Utils.readInt64(get(offset, 8), 0);
    }

    @Override
    public byte[] getFullContentAndClose() {
        try {
            byte[] result = inputStream.readAllBytes();
            closeAndClear();
            return result;
        } catch (IOException e) {
            throw new IllegalStateException("Couldn't read bytes!", e);
        }
    }

    @Override
    // Performs a Default "trim" on the String, removing spaces from beginning and end.
    public String readString(int length, String charset) {
        String result = readStringNoTrim(length, charset);
        return result.trim();
    }

    @Override
    public String readStringNoTrim(int length, String charset) {
        try {
            String result = new String(read(length), charset);
            return result;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public int available()  throws IOException {
        return inputStream.available();
    }

    @Override
    public void close() throws IOException {
        closeAndClear();
    }
}
