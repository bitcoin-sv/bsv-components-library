package com.nchain.jcl.net.protocol.serialization.common;


import com.nchain.jcl.base.tools.bytes.ByteArrayReader;
import com.nchain.jcl.net.protocol.messages.HeaderMsg;
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg;
import com.nchain.jcl.net.protocol.messages.common.Message;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2019-07-14
 *
 * A serializer for Bitcoin Messages.
 * Unlike the {@link MessageSerializer}, which serialize any knd of Message, this BitcoinMsgSerializer assumes
 * that we are working with Fully Bitcoin-compliant Messages, so they all have a Header and a Body.
 */
public interface BitcoinMsgSerializer {

    /**
     * It returns the Length of the Header.
     */
    int getHeaderLength();

    /**
     * It takes a Byte soruce and returns the Header contained in it. Other bytes in the ByteRader are not procesed.
     * The same ByteReader could be used later to keep reading data from it.
     *
     * @param context           Serializer Context
     * @param byteReader        Byte Source
     * @return                  The Header of the Bitcoin Message
     */
    HeaderMsg deserializeHeader(DeserializerContext context, ByteArrayReader byteReader);


    /**
     * It takes a Byte Source and returns the Bitcoin Message contained in it. It assumes that the Byte source contains
     * a full valid Bitcoin Message, including Header + Body.
     *
     * @param context                   Serializer Context
     * @param byteReader                Byte Source
     * @param command                   Type of the message (as it's stored in the "command" field in the Header)
     * @return                          A full Bitcoin-compliant Message
     */
    <M extends Message> BitcoinMsg<M> deserialize(DeserializerContext context, ByteArrayReader byteReader,
                                                  String command);


    /**
     * It takes a full valid Bitcoin Message (including Header + Body), serializes it content and returns it as
     * a Byte Array
     *
     * @param context                   Serializer Context
     * @param bitcoinMessage            Message to deserialize
     * @param command                   Type of the message (as it's stored in the "command" field in the Header)
     * @return                          The content of "bitcoinMessage" serialized
     */
    <M extends Message> ByteArrayReader serialize(SerializerContext context, BitcoinMsg<M> bitcoinMessage,
                                                  String command);

    /**
     * It sends a signal to this Serializer so it stops immediately, throwing a RuntimeException. The time when the
     * Serializer really stops after receiving this signal, depends on the implementation.
     */
    default void kill() {}
    /** Indicates if this Serializer is killable or not */
    default boolean isKillable() { return false; }

}
