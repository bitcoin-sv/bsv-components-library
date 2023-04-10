package io.bitcoinsv.bsvcl.net.protocol.serialization.common;


import io.bitcoinsv.bsvcl.net.protocol.config.ProtocolBasicConfig;
import io.bitcoinsv.bsvcl.net.protocol.messages.HeaderMsg;
import io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsg;
import io.bitcoinsv.bsvcl.net.protocol.messages.common.BodyMessage;
import io.bitcoinsv.bsvcl.tools.bytes.ByteArrayReader;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A serializer for Bitcoin Messages.
 * Unlike the {@link MessageSerializer}, which serialize any knd of Message, this BitcoinMsgSerializer assumes
 * that we are working with Fully Bitcoin-compliant Messages, so they all have a Header and a Body.
 */
public interface BitcoinMsgSerializer {

    /**
     * It takes a Byte source and returns the Header contained in it. Other bytes in the ByteRader are not procesed.
     * The same ByteReader could be used later to keep reading data from it.
     *
     * @param context           Serializer Context
     * @param byteReader        Byte Source
     * @return                  The Header of the Bitcoin Message
     */
    HeaderMsg deserializeHeader(DeserializerContext context, ByteArrayReader byteReader);

    /**
     * It takes a Byte source and returns the Header contained in it. Other bytes in the ByteRader are not procesed.
     * The same ByteReader could be used later to keep reading data from it.
     *
     * @param context           Serializer Context
     * @param headerMsg         HeaderMsg related (previously received))to the body we are deserializing
     * @param byteReader        Byte Source
     * @return                  The Header of the Bitcoin Message
     */
    <M extends BodyMessage> M deserializeBody(DeserializerContext context, HeaderMsg headerMsg, ByteArrayReader byteReader);


    /**
     * It calculates the checksum of the payload provided. The 'numByes' specifies the maximum number of bytes to use
     */
    long calculateChecksum(ByteArrayReader byteReader, long numBytes);

    /**
     * It calculates the checksum of Message, considering the Protocol Configuration given
     */
    <M extends BodyMessage> long calculateChecksum(ProtocolBasicConfig protocolBasicConfig, M bodyMessage);

    /**
     * It takes a Byte Source and returns the Bitcoin Message contained in it. It assumes that the Byte source contains
     * a full valid Bitcoin Message, including Header + Body.
     *
     * @param context                   Serializer Context
     * @param byteReader                Byte Source
     * @return                          A full Bitcoin-compliant Message
     */
    <M extends BodyMessage> BitcoinMsg<M> deserialize(DeserializerContext context, ByteArrayReader byteReader);


    /**
     * It takes a full valid Bitcoin Message (including Header + Body), serializes it content and returns it as
     * a Byte Array
     *
     * @param context                   Serializer Context
     * @param bitcoinMessage            Message to deserialize
     * @return                          The content of "bitcoinMessage" serialized
     */
    <M extends BodyMessage> ByteArrayReader serialize(SerializerContext context, BitcoinMsg<M> bitcoinMessage);

}
