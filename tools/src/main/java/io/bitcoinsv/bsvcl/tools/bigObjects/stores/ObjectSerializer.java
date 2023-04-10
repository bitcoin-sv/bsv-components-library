package io.bitcoinsv.bsvcl.tools.bigObjects.stores;

import io.bitcoinsv.bsvcl.tools.bytes.ByteArrayReader;
import io.bitcoinsv.bsvcl.tools.bytes.ByteArrayWriter;

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