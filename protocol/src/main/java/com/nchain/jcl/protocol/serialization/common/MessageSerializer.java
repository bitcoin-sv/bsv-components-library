package com.nchain.jcl.protocol.serialization.common;


import com.nchain.jcl.protocol.messages.common.Message;
import com.nchain.jcl.tools.bytes.ByteArrayReader;
import com.nchain.jcl.tools.bytes.ByteArrayWriter;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2019-07-14
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


    /**
     * It sends a signal to this Serializer so it stops immediately, throwing a RuntimeException. The time when the
     * Serializer really stops after receiving this signal, depends on the implementation.
     */
    default void kill() {}

    /** Indicates if this Serializer is killable or not */
    default boolean isKillable() { return false; }
}
