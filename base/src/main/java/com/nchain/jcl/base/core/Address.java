package com.nchain.jcl.base.core;

/*
 * Copyright 2011 Google Inc.
 * Copyright 2014 Giannis Dzegoutanis
 * Copyright 2015 Andreas Schildbach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.nchain.jcl.base.exception.AddressFormatException;
import com.nchain.jcl.base.exception.WrongNetworkException;
import java.util.Arrays;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * <p>A Bitcoin address looks like 1MsScoe2fTJoq4ZPdQgqyhgWeoNamYPevy and is derived from an elliptic curve public key
 * plus a set of network parameters. </p>
 *
 * <p>A standard address is built by taking the RIPE-MD160 hash of the public key bytes, with a version prefix and a
 * checksum suffix, then encoding it textually as base58. The version prefix is used to both denote the network for
 * which the address is valid, and also to indicate how the bytes inside the address
 * should be interpreted. Whilst almost all addresses today are hashes of public keys, another (currently unsupported
 * type) can contain a hash of a script instead.</p>
 *
 */
public class Address extends VersionedChecksummedBytes implements Addressable {
    /**
     * An address is a RIPEMD160 hash of a public key, therefore is always 160 bits or 20 bytes.
     */
    public static final int LENGTH = 20;

    /**
     * Construct an address from parameters, the address version, and the hash160 form. Example:<p>
     *
     * <pre>new Address(MainNetParams.get(), NetworkParameters.getAddressHeader(), Hex.decode("4a22c3c4cbb31e4d03b15550636762bda0baf85a"));</pre>
     */
    public Address(int version, byte[] hash160) throws WrongNetworkException {
        super(version, hash160);
        checkArgument(hash160.length == LENGTH, "Addresses are 160-bit hashes, so you must provide 20 bytes");
    }

    public Address(Addressable address) {
        this(address.getVersion(), address.getHash160());
    }

    public Address(String base58) {
        super(base58);
    }

    /**
     * Construct an address from its Base58 representation.
     * @param base58
     *            The textual form of the address, such as "17kzeh4N8g49GFvdDzSf8PjaPfyoD1MndL".
     * @throws AddressFormatException
     *             if the given base58 doesn't parse or the checksum is invalid
     * @throws WrongNetworkException
     *             if the given address is valid but for a different chain (eg testnet vs mainnet)
     */
    public static Address fromBase58(String base58) throws AddressFormatException {
        return new Address(base58);
    }


    /** The (big endian) 20 byte hash that is the core of a Bitcoin address. */
    @Override
    public byte[] getHash160() {
        return bytes;
    }

    /**
     * This implementation narrows the return type to <code>Address</code>.
     */
    @Override
    public Address clone() throws CloneNotSupportedException {
        return (Address) super.clone();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Addressable other = (Addressable) o;
        return this.getVersion() == other.getVersion() && Arrays.equals(this.getHash160(), other.getHash160());
    }

}
