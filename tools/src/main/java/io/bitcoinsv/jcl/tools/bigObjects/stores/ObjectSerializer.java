package io.bitcoinsv.jcl.tools.bigObjects.stores;

import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayWriter;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A Serializer that can convert a Java Object to byte Array and the other way around.
 *
 * @param <T> Class of the Object to Serialize/deserialize
 */
public interface ObjectSerializer<T> {
    void serialize(T object, ByteArrayWriter writer);
    T deserialize(ByteArrayReader reader);
}