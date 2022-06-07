package io.bitcoinsv.jcl.tools.serialization;



import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayWriter;
import io.bitcoinsv.bitcoinjsv.core.Sha256Hash;
import io.bitcoinsv.bitcoinjsv.core.Utils;
import io.bitcoinsv.jcl.tools.bytes.IReader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * @date 2020-09-08
 *
 * Utility class to Serialize/Deserialize common data types:
 */
public abstract class BitcoinSerializerUtils {

    // Indicates how many bytes the number given will take
    public static int getVarIntSizeInBytes(long value) {
        // if negative, it's actually a very largeMsgs unsigned long value
        if (value < 0) return 9; // 1 marker + 8 data bytes
        if (value < 253) return 1; // 1 data byte
        if (value <= 0xFFFFL) return 3; // 1 marker + 2 data bytes
        if (value <= 0xFFFFFFFFL) return 5; // 1 marker + 4 data bytes
        return 9; // 1 marker + 8 data bytes
    }

    /** Deserialize the ByteArray into a number (length variable) */
    public static long deserializeVarIntWithoutExtraction(IReader reader, int offset) {
        long result = -1;
        int firstByte = 0xFF & reader.get(offset, 1)[0];

        // We calculate how to read the value from the byte Array.
        // the size in bytes used to store the value will be calculated automatically by the Builder later on:
        if (firstByte < 253){
            result = firstByte;
        } else if (firstByte == 253){
            result = (0xFF & reader.get(offset + 1, 1)[0]) | ((0xFF & reader.get(offset + 2, 1)[0]) << 8);
        } else if (firstByte == 254) {
            result = reader.getUint32(offset + 1);
        } else {
            result = reader.getInt64LE(offset + 1);
        }
        return result;
    }


    /** Deserialize the ByteArray into a number (length variable) */
    public static long deserializeVarInt(IReader reader) {
        long result = -1;
        int firstByte = 0xFF & reader.read();

        // We calculate how to read the value from the byte Array.
        // the size in bytes used to store the value will be calculated automatically by the Builder later on:
        if (firstByte < 253){
            result = firstByte;
        } else if (firstByte == 253){
            result = (0xFF & reader.read()) | ((0xFF & reader.read()) << 8);
        } else if (firstByte == 254) {
            result = reader.readUint32();
        } else {
            result = reader.readInt64LE();
        }
        return result;
    }

    private static byte[] getVarIntBytestoWrite(long value) {
        byte[] bytesToWrite;
        int sizeInBytes = getVarIntSizeInBytes(value);
        switch (sizeInBytes) {
            case 1: {
                bytesToWrite = new byte[]{(byte) value};
                break;
            }
            case 3: {
                bytesToWrite = new byte[]{(byte) 253, (byte) (value), (byte) (value >> 8)};
                break;
            }
            case 5: {
                bytesToWrite = new byte[5];
                bytesToWrite[0] = (byte) 254;
                Utils.uint32ToByteArrayLE(value, bytesToWrite, 1);
                break;
            }
            default: {
                bytesToWrite = new byte[9];
                bytesToWrite[0] = (byte) 255;
                Utils.uint64ToByteArrayLE(value, bytesToWrite, 1);
                break;
            }
        } // switch
        return bytesToWrite;
    }
    /** Serialize a value into a ByteArayWriter given */
    public static void serializeVarInt(long value, ByteArrayWriter writer) {
        byte[] bytesToWrite = getVarIntBytestoWrite(value);
        writer.write(bytesToWrite);
    }

    /** Serialize a value into a ByteArayWriter given */
    public static void serializeVarInt(long value, ByteArrayOutputStream bos) {
        byte[] bytesToWrite = getVarIntBytestoWrite(value);
        try { bos.write(bytesToWrite);} catch (IOException ioe) {throw new RuntimeException(ioe);};
    }

    /** Deserialize a Hash from a ByteArray */
    public static Sha256Hash deserializeHash(ByteArrayReader byteReader) {
        byte[] hashValue = byteReader.read(Sha256Hash.LENGTH);
        Sha256Hash result = Sha256Hash.wrapReversed(hashValue);
        return result;
    }

    /** Serialzes a Hash into a ByteArray */
    public static void serializeHash(Sha256Hash hash, ByteArrayWriter byteWriter) {
        byteWriter.write(hash.getReversedBytes());
    }

    /** Serializes a String into a ByteArray */
    public static void serializeVarStr(String value, ByteArrayWriter writer) {
        // First we serialize the length of the String
        BitcoinSerializerUtils.serializeVarInt(value.length(), writer);
        // Then we serialize the String itself:
        writer.writeStr(value, "UTF-8");
    }

    /** Deserializes a String from a Byte Array */
    public static String deserializeVarStr(ByteArrayReader reader) {
        // First we read the length of the String
        long length = BitcoinSerializerUtils.deserializeVarInt(reader);
        // we read the String itself
        String result = (length == 0) ? "" : reader.readStringNoTrim((int)length, "UTF-8");
        return result;
    }
}
