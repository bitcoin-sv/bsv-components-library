package com.nchain.jcl.base.tools.bytes;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.Arrays;

/**
 * @author a.vilches@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */
public class ByteTools {

    public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    /**
     * The regular {@link BigInteger#toByteArray()} method isn't quite what we often need: it appends a
     * leading zero to indicate that the number is positive and may need padding.
     *
     * @param b        the integer to format into a byte array
     * @param numBytes the desired size of the resulting byte array
     * @return numBytes byte long array.
     */
    public static byte[] bigIntegerToBytes(BigInteger b, int numBytes) {
        if (b == null) {
            return null;
        }
        byte[] bytes = new byte[numBytes];
        byte[] biBytes = b.toByteArray();
        int start = (biBytes.length == numBytes + 1) ? 1 : 0;
        int length = Math.min(biBytes.length, numBytes);
        System.arraycopy(biBytes, start, bytes, numBytes - length, length);
        return bytes;
    }

    // uint32 -> BE
    public static byte[] uint32ToByteArrayBE(long val) {
        byte[] out = new byte[4];
        uint32ToByteArrayBE(val, out, 0);
        return out;
    }

    public static void uint32ToByteArrayBE(long val, byte[] out, int offset) {
        out[offset + 0] = getUByte(val, 3);
        out[offset + 1] = getUByte(val, 2);
        out[offset + 2] = getUByte(val, 1);
        out[offset + 3] = getUByte(val, 0);
    }

    public static void uint32ToByteStreamBE(long val, OutputStream stream) throws IOException {
        stream.write((int) getUByte(val, 3));
        stream.write((int) getUByte(val, 2));
        stream.write((int) getUByte(val, 1));
        stream.write((int) getUByte(val, 0));
    }

    // uint32 -> LE
    public static byte[] uint32ToByteArrayLE(long val) {
        byte[] out = new byte[4];
        uint32ToByteArrayLE(val, out, 0);
        return out;
    }

    public static void uint32ToByteArrayLE(long val, byte[] out, int offset) {
        out[offset + 0] = getUByte(val, 0);
        out[offset + 1] = getUByte(val, 1);
        out[offset + 2] = getUByte(val, 2);
        out[offset + 3] = getUByte(val, 3);
    }

    public static void uint32ToByteStreamLE(long val, OutputStream stream) throws IOException {
        stream.write((int) getUByte(val, 0));
        stream.write((int) getUByte(val, 1));
        stream.write((int) getUByte(val, 2));
        stream.write((int) getUByte(val, 3));
    }

    // uint64 -> LE
    public static byte[] uint64ToByteArrayLE(long val) {
        byte[] out = new byte[8];
        uint64ToByteArrayLE(val, out, 0);
        return out;
    }

    public static void uint64ToByteArrayLE(long val, byte[] out, int offset) {
        out[offset + 0] = getUByte(val, 0);
        out[offset + 1] = getUByte(val, 1);
        out[offset + 2] = getUByte(val, 2);
        out[offset + 3] = getUByte(val, 3);
        out[offset + 4] = getUByte(val, 4);
        out[offset + 5] = getUByte(val, 5);
        out[offset + 6] = getUByte(val, 6);
        out[offset + 7] = getUByte(val, 7);
    }


    public static void uint64ToByteStreamLE(long val, OutputStream stream) throws IOException {
        stream.write((int) getUByte(val, 0));
        stream.write((int) getUByte(val, 1));
        stream.write((int) getUByte(val, 2));
        stream.write((int) getUByte(val, 3));
        stream.write((int) getUByte(val, 4));
        stream.write((int) getUByte(val, 5));
        stream.write((int) getUByte(val, 6));
        stream.write((int) getUByte(val, 7));
    }

    public static void uint64ToByteStreamLE(BigInteger val, OutputStream stream) throws IOException {
        byte[] bytes = val.toByteArray();
        if (bytes.length > 8) {
            throw new RuntimeException("Input too largeMsgs to encode into a uint64");
        }
        bytes = reverseBytes(bytes);
        stream.write(bytes);
        if (bytes.length < 8) {
            for (int i = 0; i < 8 - bytes.length; i++)
                stream.write(0);
        }
    }

    /**
     * Returns a copy of the given byte array in reverse order.
     */
    public static byte[] reverseBytes(byte[] bytes) {
        final int length = bytes.length;
        byte[] buffer = new byte[length];
        for (int i = 0; i < length; i++) {
            buffer[i] = bytes[length - 1 - i];
        }
        return buffer;
    }

    /**
     * Returns a copy of the given byte array with the bytes of each double-word (4 bytes) reversed.
     *
     * @param bytes      length must be divisible by 4.
     * @param trimLength trim output to this length.  If positive, must be divisible by 4.
     */
    public static byte[] reverseDwordBytes(byte[] bytes, int trimLength) {
        byte[] rev = new byte[trimLength >= 0 && bytes.length > trimLength ? trimLength : bytes.length];

        for (int i = 0; i < rev.length; i += 4) {
            System.arraycopy(bytes, i, rev, i, 4);
            for (int j = 0; j < 4; j++) {
                rev[i + j] = bytes[i + 3 - j];
            }
        }
        return rev;
    }

    /**
     * Parse 4 bytes from the byte array (starting at the offset) as unsigned 32-bit integer in little endian format.
     */
    public static long readUint32(byte[] bytes) {
        return readUint32(bytes, 0);
    }

    public static long readUint32(byte[] bytes, int offset) {
        return moveUByteTo(bytes[offset + 0], 0) |
                moveUByteTo(bytes[offset + 1], 1) |
                moveUByteTo(bytes[offset + 2], 2) |
                moveUByteTo(bytes[offset + 3], 3);
    }

    /**
     * Parse 8 bytes from the byte array (starting at the offset) as signed 64-bit integer in little endian format.
     */
    public static long readInt64LE(byte[] bytes) {
        return readInt64LE(bytes, 0);
    }

    public static long readInt64LE(byte[] bytes, int offset) {
        return moveUByteTo(bytes[offset + 0], 0) |
                moveUByteTo(bytes[offset + 1], 1) |
                moveUByteTo(bytes[offset + 2], 2) |
                moveUByteTo(bytes[offset + 3], 3) |
                moveUByteTo(bytes[offset + 4], 4) |
                moveUByteTo(bytes[offset + 5], 5) |
                moveUByteTo(bytes[offset + 6], 6) |
                moveUByteTo(bytes[offset + 7], 7);
    }

    /**
     * Parse 4 bytes from the byte array (starting at the offset) as unsigned 32-bit integer in big endian format.
     */
    public static long readUint32BE(byte[] bytes) {
        return readUint32BE(bytes, 0);
    }

    public static long readUint32BE(byte[] bytes, int offset) {
        return moveUByteTo(bytes[offset + 0], 3) |
                moveUByteTo(bytes[offset + 1], 2) |
                moveUByteTo(bytes[offset + 2], 1) |
                moveUByteTo(bytes[offset + 3], 0);
    }


    public static byte[] copyOf(byte[] in) {
        return copyOf(in, 0, in.length);
    }

    public static byte[] copyOf(byte[] in, int length) {
        return copyOf(in , 0, length);
    }

    public static byte[] copyOf(byte[] in, int cursor, int length) {
        byte[] out = new byte[length];
        System.arraycopy(in, cursor, out, 0, Math.min(length, in.length - cursor));
        return out;
    }

    /**
     * Creates a copy of bytes and appends b to the end of it
     */
    public static byte[] appendByte(byte[] bytes, byte b) {
        byte[] result = Arrays.copyOf(bytes, bytes.length + 1);
        result[result.length - 1] = b;
        return result;
    }

    private static long moveUByteTo(byte val, int position) {
        return (val & 0xffL) << (position * 8);
    }

    private static byte getUByte(long val, int position) {
        assert position < 8; // java long types has 8 bytes
        return (byte) (val >> (position * 8) & 0xFF);
    }

    /**
     * <p>The "compact" format is a representation of a whole number N using an unsigned 32 bit number similar to a
     * floating point format. The most significant 8 bits are the unsigned exponent of base 256. This exponent can
     * be thought of as "number of bytes of N". The lower 23 bits are the mantissa. Bit number 24 (0x800000) represents
     * the sign of N. Therefore, N = (-1^sign) * mantissa * 256^(exponent-3).</p>
     *
     * <p>Satoshi's original implementation used BN_bn2mpi() and BN_mpi2bn(). MPI uses the most significant bit of the
     * first byte as sign. Thus 0x1234560000 is compact 0x05123456 and 0xc0de000000 is compact 0x0600c0de. Compact
     * 0x05c0de00 would be -0x40de000000.</p>
     *
     * <p>Bitcoin only uses this "compact" format for encoding difficulty targets, which are unsigned 256bit quantities.
     * Thus, all the complexities of the sign bit and using base 256 are probably an implementation accident.</p>
     */
    public static BigInteger decodeCompactBits(long compact) {
        int size = ((int) (compact >> 24)) & 0xFF;
        byte[] bytes = new byte[4 + size];
        bytes[3] = (byte) size;
        if (size >= 1) bytes[4] = (byte) ((compact >> 16) & 0xFF);
        if (size >= 2) bytes[5] = (byte) ((compact >> 8) & 0xFF);
        if (size >= 3) bytes[6] = (byte) (compact & 0xFF);
        return decodeMPI(bytes, true);
    }

    /**
     * MPI encoded numbers are produced by the OpenSSL BN_bn2mpi function. They consist of
     * a 4 byte big endian length field, followed by the stated number of bytes representing
     * the number in big endian format (with a sign bit).
     * @param hasLength can be set to false if the given array is missing the 4 byte length field
     */
    public static BigInteger decodeMPI(byte[] mpi, boolean hasLength) {
        byte[] buf;
        if (hasLength) {
            int length = (int) readUint32BE(mpi, 0);
            buf = new byte[length];
            System.arraycopy(mpi, 4, buf, 0, length);
        } else
            buf = mpi;
        if (buf.length == 0)
            return BigInteger.ZERO;
        boolean isNegative = (buf[0] & 0x80) == 0x80;
        if (isNegative)
            buf[0] &= 0x7f;
        BigInteger result = new BigInteger(buf);
        return isNegative ? result.negate() : result;
    }

    /**
     * MPI encoded numbers are produced by the OpenSSL BN_bn2mpi function. They consist of
     * a 4 byte big endian length field, followed by the stated number of bytes representing
     * the number in big endian format (with a sign bit).
     * @param includeLength indicates whether the 4 byte length field should be included
     */
    public static byte[] encodeMPI(BigInteger value, boolean includeLength) {
        if (value.equals(BigInteger.ZERO)) {
            if (!includeLength)
                return new byte[] {};
            else
                return new byte[] {0x00, 0x00, 0x00, 0x00};
        }
        boolean isNegative = value.signum() < 0;
        if (isNegative)
            value = value.negate();
        byte[] array = value.toByteArray();
        int length = array.length;
        if ((array[0] & 0x80) == 0x80)
            length++;
        if (includeLength) {
            byte[] result = new byte[length + 4];
            System.arraycopy(array, 0, result, length - array.length + 3, array.length);
            uint32ToByteArrayBE(length, result, 0);
            if (isNegative)
                result[4] |= 0x80;
            return result;
        } else {
            byte[] result;
            if (length != array.length) {
                result = new byte[length];
                System.arraycopy(array, 0, result, 1, array.length);
            }else
                result = array;
            if (isNegative)
                result[0] |= 0x80;
            return result;
        }
    }

    /**
     * Returns a minimally encoded encoded version of the data. That is, a version will pass the check
     * in checkMinimallyEncodedLE(byte[] bytesLE).
     *
     * If the data is already minimally encoded the original byte array will be returned.
     *
     * inspired by: https://reviews.bitcoinabc.org/D1219
     *
     * @param dataLE
     * @return
     */
    public static byte[] minimallyEncodeLE(byte[] dataLE) {

        if (dataLE.length == 0) {
            return dataLE;
        }

        // If the last byte is not 0x00 or 0x80, we are minimally encoded.
        int last = dataLE[dataLE.length - 1];
        if ((last & 0x7f) != 0) {
            return dataLE;
        }

        // If the script is one byte long, then we have a zero, which encodes as an
        // empty array.
        if (dataLE.length == 1) {
            return EMPTY_BYTE_ARRAY;
        }

        // If the next byte has it sign bit set, then we are minimaly encoded.
        if ((dataLE[dataLE.length - 2] & 0x80) != 0) {
            return dataLE;
        }

        //we might modify the array so clone it
        dataLE = dataLE.clone();

        // We are not minimally encoded, we need to figure out how much to trim.
        // we are using i - 1 indexes here as we want to ignore the last byte (first byte in BE)
        for (int i = dataLE.length - 1; i > 0; i--) {
            // We found a non zero byte, time to encode.
            if (dataLE[i - 1] != 0) {
                if ((dataLE[i - 1] & 0x80) != 0) {
                    // We found a byte with it's sign bit set so we need one more
                    // byte.
                    dataLE[i++] = (byte) last;
                } else {
                    // the sign bit is clear, we can use it.
                    // add the sign bit from the last byte
                    dataLE[i - 1] |= last;
                }

                return Arrays.copyOf(dataLE, i);
            }
        }

        // If we the whole thing is zeros, then we have a zero.
        return EMPTY_BYTE_ARRAY;
    }


    /**
     * checks that LE encoded number is minimally represented.  That is that there are no leading zero bytes except in
     * the case: if there's more than one byte and the most significant bit of the second-most-significant-byte is set it
     * would conflict with the sign bit.
     * @param bytesLE
     * @return
     */
    public static boolean checkMinimallyEncodedLE(byte[] bytesLE, int maxNumSize) {

        if (bytesLE.length > maxNumSize) {
            return false;
        }

        if (bytesLE.length > 0) {
            // Check that the number is encoded with the minimum possible number
            // of bytes.
            //
            // If the most-significant-byte - excluding the sign bit - is zero
            // then we're not minimal. Note how this test also rejects the
            // negative-zero encoding, 0x80.
            if ((bytesLE[bytesLE.length - 1] & 0x7f) == 0) {
                // One exception: if there's more than one byte and the most
                // significant bit of the second-most-significant-byte is set it
                // would conflict with the sign bit. An example of this case is
                // +-255, which encode to 0xff00 and 0xff80 respectively.
                // (big-endian).
                if (bytesLE.length <= 1 || (bytesLE[bytesLE.length - 2] & 0x80) == 0) {
                    return false;
                }
            }
        }

        return true;
    }

}
