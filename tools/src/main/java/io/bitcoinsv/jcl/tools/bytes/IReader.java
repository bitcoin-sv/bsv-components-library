package io.bitcoinsv.jcl.tools.bytes;

public interface IReader {
    byte[] read(int length);

    byte[] get(int length);

    byte[] get(long offset, int length);

    long readUint32();

    long readUint64();

    byte read();

    int readUint16();

    long readInt64LE();

    long readInt48LE();

    boolean readBoolean();

    long size();

    boolean isEmpty();

    void closeAndClear();

    byte[] getFullContent();

    long getBytesReadCount();

    long getUint32(int offset);

    long getInt64LE(int offset);

    byte[] getFullContentAndClose();

    // Performs a Default "trim" on the String, removing spaces from beginning and end.
    String readString(int length, String charset);

    String readStringNoTrim(int length, String charset);
}
