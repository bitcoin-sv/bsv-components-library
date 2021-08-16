/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.store.blockStore.metadata;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * @date 27/04/2021
 *
 * A Metadata class is an object that can be attached to a Block or Tx, and then be stored in JCL-Store, and retrieved
 * by using the Key for the Blocks or Tx it's been linked to.
 */
public interface Metadata {
    /** Serializes the content of the Class into a Array of Bytes */
    byte[] serialize();
    /** It parses the data fromm a Byte Array, deserializes it and loads/populates the content of the Class */
    void load(byte[] data);
}
