package com.nchain.jcl.base.serialization;

import com.nchain.jcl.base.tools.bytes.ByteArrayReader;
import com.nchain.jcl.base.tools.bytes.ByteArrayWriter;
import com.nchain.jcl.base.tools.bytes.ByteTools;
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * @date 2020-09-08
 *
 * Utility class to Serialize/Deserialize common data types:
 */
public abstract class BitcoinSerializerUtils {

    // Indicates how many bytes the number given will take
    private static int getVarIntSizeInBytes(long value) {
        // if negative, it's actually a very largeMsgs unsigned long value
        if (value < 0) return 9; // 1 marker + 8 data bytes
        if (value < 253) return 1; // 1 data byte
        if (value <= 0xFFFFL) return 3; // 1 marker + 2 data bytes
        if (value <= 0xFFFFFFFFL) return 5; // 1 marker + 4 data bytes
        return 9; // 1 marker + 8 data bytes
    }

    /** Deserialize the ByteArray into a number (length variable) */
    public static long deserializeVarInt(ByteArrayReader byteReader) {
        long result = -1;
        int firstByte = 0xFF & byteReader.read();

        // We calculate how to read the value from the byte Array.
        // the size in bytes used to store the value will be calculated automatically by the Builder later on:
        if (firstByte < 253){
            result = firstByte;
        } else if (firstByte == 253){
            byteReader.waitForBytes(2);
            result = (0xFF & byteReader.read()) | ((0xFF & byteReader.read()) << 8);
        } else if (firstByte == 254) {
            byteReader.waitForBytes(4);
            result = byteReader.readUint32();
        } else {
            byteReader.waitForBytes(8);
            result = byteReader.readInt64LE();
        }
        return result;
    }

    /** Serialize a value into a ByteArayWriter given */
    public static void serializeVarInt(long value, ByteArrayWriter writer) {
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
                ByteTools.uint32ToByteArrayLE(value, bytesToWrite, 1);
                break;
            }
            default: {
                bytesToWrite = new byte[9];
                bytesToWrite[0] = (byte) 255;
                ByteTools.uint64ToByteArrayLE(value, bytesToWrite, 1);
                break;
            }
        } // switch

        writer.write(bytesToWrite);
    }

    /** Deserialize a Hash from a ByteArray */
    public static Sha256Wrapper deserializeHash(ByteArrayReader byteReader) {
        byte[] hashValue = byteReader.read(Sha256Wrapper.LENGTH);
        Sha256Wrapper result = Sha256Wrapper.wrapReversed(hashValue);
        return result;
    }

    /** Serialzes a Hash into a ByteArray */
    public static void serializeHash(Sha256Wrapper hash, ByteArrayWriter byteWriter) {
        byteWriter.write(hash.getReversedBytes());
    }
}
