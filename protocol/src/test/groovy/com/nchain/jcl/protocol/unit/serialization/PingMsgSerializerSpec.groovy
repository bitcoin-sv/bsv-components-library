package com.nchain.jcl.protocol.unit.serialization

import com.nchain.jcl.protocol.config.ProtocolConfig
import com.nchain.jcl.protocol.config.provided.ProtocolBSVMainConfig
import com.nchain.jcl.protocol.messages.PingMsg
import com.nchain.jcl.protocol.messages.common.BitcoinMsg
import com.nchain.jcl.protocol.messages.common.BitcoinMsgBuilder
import com.nchain.jcl.protocol.serialization.PingMsgSerializer
import com.nchain.jcl.protocol.serialization.common.*
import com.nchain.jcl.tools.bytes.HEX
import com.nchain.jcl.tools.bytes.ByteArrayReader
import com.nchain.jcl.tools.bytes.ByteArrayWriter
import spock.lang.Specification
/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 24/07/2019 10:12
 *
 * Testing class for the Ping Message Serialization.
 * The test is taken the assumption that we have already a correct serialization version of this Message, obtained
 * from another source that we trust (in this case the Java BitcoinJ library). So we serialize/deserialize some
 * messages with out code and compare the results with that reference.
 */
class PingMsgSerializerSpec extends Specification {

    // These variables have been used to generate the serialized version of a FULL Ping Message in BitcoinJ, including both
    // Header and Body.
    private static final String REF_PING_MSG = "e3e1f3e870696e6700000000000000000800000032ab095c3d9a9cb22d32b40b"
    private static final long  REF_PING_NONCE = 843354202076650045

    // These variables  have been used to generate the serialized version of the BODY of a Ping Message in
    // BitcoinJ, and its used here as a reference to compare our own Serializing implementation.
    private static final String REF_BODY_PING_MSG = "72be75ff2b9e1c3d"
    private static final long  REF_BODY_NONCE = 4403568447468191346

    def "testing Ping Message Body Deserializing"(int byteInterval, int delayMs) {
        given:
            ProtocolConfig config = new ProtocolBSVMainConfig()
            DeserializerContext context = DeserializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .build()
            PingMsg message = null
            ByteArrayReader byteReader = new ByteArrayReader(HEX.decode(REF_BODY_PING_MSG))

        when:
            message = PingMsgSerializer.getInstance().deserialize(context, byteReader)

        then:
            message.getNonce() == REF_BODY_NONCE
        where:
            byteInterval | delayMs
                10       |    25

    }

    def "testing Ping Message Body Serializing"() {
        given:
            ProtocolConfig config = new ProtocolBSVMainConfig()
            SerializerContext context = SerializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .build()
            PingMsg message = PingMsg.builder().nonce(REF_BODY_NONCE).build()
            ByteArrayWriter byteWriter = new ByteArrayWriter()
            String messageSerialized = null
        when:
            PingMsgSerializer.getInstance().serialize(context, message, byteWriter)
            messageSerialized = HEX.encode(byteWriter.reader().getFullContent())
        then:
            messageSerialized.equals(REF_BODY_PING_MSG)
    }

    def "testing Ping Message COMPLETE de-serializing"(int byteInterval, int delayMs) {
        given:
            ProtocolConfig config = new ProtocolBSVMainConfig()
            DeserializerContext context = DeserializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .build()
            ByteArrayReader byteReader = new ByteArrayReader(HEX.decode(REF_PING_MSG))
            BitcoinMsgSerializer bitcoinSerializer = new BitcoinMsgSerializerImpl()
        when:
            BitcoinMsg<PingMsg> message = bitcoinSerializer.deserialize(context, byteReader, PingMsg.MESSAGE_TYPE)
        then:
            message.getHeader().getMagic().equals(config.getBasicConfig().getMagicPackage())
            message.getHeader().getCommand().toUpperCase().equals(PingMsg.MESSAGE_TYPE.toUpperCase())
            message.getBody().nonce == REF_PING_NONCE
        where:
            byteInterval | delayMs
                10       |    25
    }

    def "testing Ping Message COMPLETE Serializing"() {
        given:
            ProtocolConfig config = new ProtocolBSVMainConfig()
            SerializerContext context = SerializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .build()
            PingMsg pingBodyMsg = PingMsg.builder().nonce(REF_PING_NONCE).build()
            BitcoinMsg<PingMsg> bitcoinPingMsg = new BitcoinMsgBuilder<>(config.getBasicConfig(), pingBodyMsg).build()
            BitcoinMsgSerializer bitcoinSerializer = new BitcoinMsgSerializerImpl()
        when:
            byte[] pingMsgBytes = bitcoinSerializer.serialize(context, bitcoinPingMsg, PingMsg.MESSAGE_TYPE).getFullContent()
            String pingnMsgDeserialzed = HEX.encode(pingMsgBytes)
        then:
            pingnMsgDeserialzed.equals(REF_PING_MSG)
    }
}
