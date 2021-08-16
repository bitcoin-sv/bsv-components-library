/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.protocol.serialization.common;

import io.bitcoinsv.jcl.net.protocol.messages.common.Message;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayWriter;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Operations to convert between a Message and a byte array
 */
public interface MessageSerializer<M extends Message> {


    /**
     * It takes a Byte Array, reads it and creates a Message with its content.
     *
     * @param context                   Serializer Context
     * @param byteReader                Wrapper for the Byte Array Source
     * @return                          A full Bitcoin-compliant Message
     * @return                          An instance of a Message
     */
    M deserialize(DeserializerContext context, ByteArrayReader byteReader);


    /**
     * It takes a Message, converts its information into a Byte Array and writes it to
     * the Output (ByteWriter)
     *
     * @param context                   Serializer Context
     * @param message                   Message to Serialize
     * @param byteWriter                Wrapper over an Outut Stream. It will sore the result of the Serialization, which can
     *                                  be retrieved later on by calling the method "byteWriter.getBytes()".
     * @return                          A full Bitcoin-compliant Message
     */
    void serialize(SerializerContext context, M message, ByteArrayWriter byteWriter);

}
