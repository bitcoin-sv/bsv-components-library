package com.nchain.jcl.protocol.unit.serialization

import com.nchain.jcl.protocol.config.ProtocolConfig
import com.nchain.jcl.protocol.config.provided.ProtocolBSVMainConfig
import com.nchain.jcl.protocol.messages.PongMsg
import com.nchain.jcl.protocol.messages.common.BitcoinMsg
import com.nchain.jcl.protocol.messages.common.BitcoinMsgBuilder
import com.nchain.jcl.protocol.serialization.PongMsgSerializer
import com.nchain.jcl.protocol.serialization.common.*
import com.nchain.jcl.tools.bytes.HEX
import com.nchain.jcl.tools.bytes.ByteArrayReader
import com.nchain.jcl.tools.bytes.ByteArrayWriter
import spock.lang.Specification
/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 30/07/2019 12:26
 *
 * Testing class for the Pong Message Serialization.
 * The test is taken the assumption that we have already a correct serialization version of this Message, obtained
 * from another source that we trust (in this case the Java BitcoinJ library). So we serialize/deserialize some
 * messages with out code and compare the results with that reference.
 */
class PongMsgSerializerSpec extends Specification {

    // These variables  have been used to generate the serialized version of the BODY of a Pong Message in
    // BitcoinJ, and its used here as a reference to compare our own Serializing implementation.
    private static final String REF_BODY_PONG_MSG = "440b89d1bafab71b"
    private static final long  REF_BODY_NONCE = 1997340640048384836

    // These variables have been used to generate the serialized version of a FULL Pong Message in BitcoinJ, including both
    // Header and Body.
    private static final String REF_PONG_MSG = "e3e1f3e8706f6e670000000000000000080000006fd6f567a2d34110b0761803"
    private static final long  REF_PONG_NONCE = 223058680113910690

    def "testing Pong Message Body Deserializing"() {
        given:
            ProtocolConfig config = new ProtocolBSVMainConfig()
            DeserializerContext context = DeserializerContext.builder()
                    .protocolconfig(config)
                    .build()
            PongMsg message = null
            ByteArrayReader byteReader = new ByteArrayReader(HEX.decode(REF_BODY_PONG_MSG))

        when:
            message = PongMsgSerializer.getInstance().deserialize(context, byteReader)

        then:
            message.getNonce() == REF_BODY_NONCE

    }

    def "testing Pong Message Body Serializing"() {
        given:
            ProtocolConfig config = new ProtocolBSVMainConfig()
            SerializerContext context = SerializerContext.builder()
                    .protocolconfig(config)
                    .build()
            PongMsg message = PongMsg.builder().nonce(REF_BODY_NONCE).build()
            ByteArrayWriter byteWriter = new ByteArrayWriter()
            String messageSerialized = null
        when:
            PongMsgSerializer.getInstance().serialize(context, message, byteWriter)
            messageSerialized = HEX.encode(byteWriter.reader().getFullContent())
            byteWriter.reader()
        then:
            messageSerialized.equals(REF_BODY_PONG_MSG)
    }

    def "testing Pong Message COMPLETE de-serializing"() {
        given:
            ProtocolConfig config = new ProtocolBSVMainConfig()
            DeserializerContext context = DeserializerContext.builder()
                .protocolconfig(config)
                .build()
            ByteArrayReader byteReader = new ByteArrayReader(HEX.decode(REF_PONG_MSG))
            BitcoinMsgSerializer bitcoinSerializer = new BitcoinMsgSerializerImpl()
        when:
            BitcoinMsg<PongMsg> message = bitcoinSerializer.deserialize(context, byteReader, PongMsg.MESSAGE_TYPE)
        then:
            message.getHeader().getMagic().equals(config.getMagicPackage())
            message.getHeader().getCommand().toUpperCase().equals(PongMsg.MESSAGE_TYPE.toUpperCase())
            message.getBody().nonce == REF_PONG_NONCE
    }

    def "testing Pong Message COMPLETE Serializing"() {
        given:
            ProtocolConfig config = new ProtocolBSVMainConfig()
            SerializerContext context = SerializerContext.builder()
                .protocolconfig(config)
                    .build()
            PongMsg pongBody = PongMsg.builder().nonce(REF_PONG_NONCE).build()
            BitcoinMsg<PongMsg> bitcoinPongMsg = new BitcoinMsgBuilder<>(config, pongBody).build()
            BitcoinMsgSerializer bitcoinSerializer = new BitcoinMsgSerializerImpl()
        when:
            byte[] pongMsgBytes = bitcoinSerializer.serialize(context, bitcoinPongMsg, PongMsg.MESSAGE_TYPE).getFullContent()
            String pongMsgDeserialzed = HEX.encode(pongMsgBytes)
        then:
            pongMsgDeserialzed.equals(REF_PONG_MSG)
    }
}
