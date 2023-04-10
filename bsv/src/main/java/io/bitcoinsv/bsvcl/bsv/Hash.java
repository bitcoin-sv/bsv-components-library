package io.bitcoinsv.bsvcl.bsv;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HexFormat;

public class Hash {
    public static final int LENGTH = 32;    // length of a hash in bytes
    private final byte[] value;

    public Hash(byte[] value) {
        if (value.length != LENGTH) {
            throw new IllegalArgumentException("Hash must be " + LENGTH + " bytes long");
        }
        this.value = value;
    }

    /**
     * Create a Hash from the string representation of the hash.
     * The string representation of a hash is reversed, so we need to account for that.
     * @param hashString the string representation of the hash
     * @return the Hash
     */
    public static Hash hashValue(final String hashString) {
        return new Hash(HexFormat.of().parseHex(new StringBuilder(hashString).reverse().toString()));
    }

    /**
     * Create a Hash from the hash encoded in bytes.
     * @param hashBytes the hash encoded in bytes
     * @return the Hash
     */
    public static Hash hashValue(final byte[] hashBytes) {
        return new Hash(hashBytes);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Hash hash) {
            return Arrays.equals(value, hash.value);
        } else {
            return super.equals(obj);
        }
    }

    /**
     * Uses the four first bytes of the hash. This should be unique enough to be a suitable hash code even for
     * blocks, where the goal is to try and get the last bytes to be zeros (i.e. the value as a big integer lower
     * than the target value).
     */
    @Override
    public int hashCode() {
        return ByteBuffer.wrap(value).getInt(0);
    }

    @Override
    public String toString() {
        return "Hash{value=" + asString() + '}';
    }

    public byte[] asBinary() {
        return value;
    }

    public String asString() {
        // string representation of a hash value is reversed
        return new StringBuilder(HexFormat.of().formatHex(value)).reverse().toString();
    }
}
