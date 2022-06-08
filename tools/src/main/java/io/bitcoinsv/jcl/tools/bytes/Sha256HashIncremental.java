package io.bitcoinsv.jcl.tools.bytes;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An utility class to run a Sha-256 Hash on a large dataset, feeding it with incremental pieces of data.
 *
 * NOTE: It might be worth it to merge this class with Sha256Hash in bitcoinJ. The only reason not to do it at this
 * moment is due to possible performance drawbacks: Sha256Hash class is alrady being used as key and stored in Maps
 * in other projects and multiple ocassions, so adding more logic to that class might impact on memory usage.
 * >>> TO CHECK
 */
public class Sha256HashIncremental {

    private MessageDigest digest;

    /** Constructor */
    public Sha256HashIncremental() {
        try {
            this.digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /** Adds more data to hash */
    public Sha256HashIncremental add(byte[] bytes) {
        this.digest.update(bytes);
        return this;
    }

    /** Returns the hash of all the data fed so far */
    public byte[] hash() {
        return digest.digest();
    }

    /** Retruns the double-hash of all the data fed so far */
    public byte[] hashTwice() {
        return digest.digest(digest.digest());
    }

}
