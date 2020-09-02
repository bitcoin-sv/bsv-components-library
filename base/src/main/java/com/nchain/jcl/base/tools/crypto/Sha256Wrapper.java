/**
 * @author a.vilches@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2019-06-25 00:24
 */

package com.nchain.jcl.base.tools.crypto;

import com.nchain.jcl.base.tools.bytes.ByteTools;
import com.nchain.jcl.base.tools.bytes.HEX;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Arrays;

/**
 * A Sha256Hash just wraps a byte[] so that equals and hashcode work correctly, allowing it to be used as keys in a
 * map. It also checks that the length is correct and provides a bit more type safety.
 */
public class Sha256Wrapper implements Serializable, Comparable<Sha256Wrapper> {
    public static final int LENGTH = 32; // bytes
    public static final Sha256Wrapper ZERO_HASH = wrap(new byte[LENGTH]);

    private final byte[] bytes;

    /**
     * Private constructor for internal use only. Use any of the static methods to create a new instance.
     * @param rawHashBytes already calculated sha256
     */
    private Sha256Wrapper(byte[] rawHashBytes) {
        assert rawHashBytes.length == LENGTH;
        this.bytes = rawHashBytes;
    }

    /**
     * Creates a new instance that wraps the given hash value.
     *
     * @param rawHashBytes the raw hash bytes to wrap
     * @return a new instance
     * @throws IllegalArgumentException if the given array length is not exactly 32
     */
    public static Sha256Wrapper wrap(byte[] rawHashBytes) {
        return new Sha256Wrapper(rawHashBytes);
    }

    /**
     * Creates a new instance that wraps the given hash value (represented as a hex string).
     *
     * @param hexString a hash value represented as a hex string
     * @return a new instance
     * @throws IllegalArgumentException if the given string is not a valid
     *                                  hex string, or if it does not represent exactly 32 bytes
     */
    public static Sha256Wrapper wrap(String hexString) {
        return wrap(HEX.decode(hexString));
    }

    /**
     * Creates a new instance that wraps the given hash value, but with byte order reversed.
     *
     * @param rawHashBytes the raw hash bytes to wrap
     * @return a new instance
     * @throws IllegalArgumentException if the given array length is not exactly 32
     */
    public static Sha256Wrapper wrapReversed(byte[] rawHashBytes) {
        return wrap(ByteTools.reverseBytes(rawHashBytes));
    }

    /**
     * Creates a new instance containing the calculated (one-time) hash of the given bytes.
     *
     * @param contents the bytes on which the hash value is calculated
     * @return a new instance containing the calculated (one-time) hash
     */
    public static Sha256Wrapper of(byte[] contents) {
        return wrap(Sha256.hash(contents));
    }

    /**
     * Creates a new instance containing the hash of the calculated hash of the given bytes.
     *
     * @param contents the bytes on which the hash value is calculated
     * @return a new instance containing the calculated (two-time) hash
     */
    public static Sha256Wrapper twiceOf(byte[] contents) {
        return wrap(Sha256.hashTwice(contents));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return Arrays.equals(bytes, ((Sha256Wrapper) o).bytes);
    }

    /**
     * Returns the last four bytes of the wrapped hash. This should be unique enough to be a suitable hash code even for
     * blocks, where the goal is to try and get the first bytes to be zeros (i.e. the value as a big integer lower
     * than the target value).
     */
    @Override
    public int hashCode() {
        // Use the last 4 bytes, not the first 4 which are often zeros in Bitcoin.
        return fromBytes(bytes[LENGTH - 4], bytes[LENGTH - 3], bytes[LENGTH - 2], bytes[LENGTH - 1]);
    }

    private static int fromBytes(byte b1, byte b2, byte b3, byte b4) {
        return (b1 << 24) |
                (b2 & 0xFF) << 16 |
                (b3 & 0xFF) << 8 |
                (b4 & 0xFF);
    }


    /**
     * Returns a hexadecimal representation of the hash.
     * @return
     */
    @Override
    public String toString() {
        return HEX.encode(bytes);
    }

    /**
     * Returns the bytes interpreted as a positive integer.
     */
    public BigInteger toBigInteger() {
        return new BigInteger(1, bytes);
    }

    /**
     * Returns the internal byte array, without defensively copying. Therefore do NOT modify the returned array.
     */
    public byte[] getBytes() {
        return bytes;
    }

    /**
     * Returns a reversed copy of the internal byte array.
     */
    public byte[] getReversedBytes() {
        return ByteTools.reverseBytes(bytes);
    }

    @Override
    public int compareTo(final Sha256Wrapper other) {
        for (int i = LENGTH - 1; i >= 0; i--) {
            final int thisByte = this.bytes[i] & 0xff;
            final int otherByte = other.bytes[i] & 0xff;
            if (thisByte > otherByte)
                return 1;
            if (thisByte < otherByte)
                return -1;
        }
        return 0;
    }
}
