package com.nchain.jcl.base.serialization;

import com.nchain.jcl.base.domain.api.BitcoinObject;
import com.nchain.jcl.base.tools.bytes.ByteArrayReader;
import com.nchain.jcl.base.tools.bytes.ByteArrayWriter;


/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A Bitcoin Serializer provides operations to serialze/Deserialize a BitcoinObject.
 * This class uses the byteArrayReader and ByteArrayWriter classes, which are abstractions/wrappers over
 * ByteArrays that are used for reading/writting.
 *
 * @T Class (Extending BitcoinObject) to Serialize/Deserialize
 * @see ByteArrayReader
 * @see ByteArrayWriter
 */
public interface BitcoinSerializer<T extends BitcoinObject> {

    // Methods to convert a ByteArray into a Java Object
    T deserialize(ByteArrayReader byteReader);
    default T deserialize(byte[] bytes) { return deserialize(new ByteArrayReader(bytes)); }

    // Methods to convert a Java object into a Byte Array
    void serialize(T object, ByteArrayWriter byteWriter);
    default byte[] serialize(T object) {
        ByteArrayWriter byteWriter = new ByteArrayWriter();
        serialize(object, byteWriter);
        return byteWriter.reader().getFullContentAndClose();
    }
}
