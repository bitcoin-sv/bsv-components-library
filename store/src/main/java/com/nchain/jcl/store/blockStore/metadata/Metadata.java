package com.nchain.jcl.store.blockStore.metadata;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * @date 27/04/2021
 *
 * A Metadata class is an object that can be attached to a Block or Tx, and then be stored in JCL-Store, and retrieved
 * by using the Key for the Blocks or Tx it's been linked to.
 */
public interface Metadata {
    byte[] serialize();
    void load(byte[] data);
}
