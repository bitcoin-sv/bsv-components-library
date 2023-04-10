package io.bitcoinsv.bsvcl.net.unit.protocol.serialization


import io.bitcoinsv.bsvcl.tools.bytes.ByteArrayReader
import io.bitcoinsv.bsvcl.tools.bytes.ByteArrayWriter
import io.bitcoinsv.bitcoinjsv.core.Utils
import io.bitcoinsv.bitcoinjsv.params.MainNetParams
import io.bitcoinsv.bitcoinjsv.params.Net
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

    def "testing Pong Message Body Deserializing"(int byteInterval, int delayMs) {
        given:
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig config = io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext context = io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .build()
            io.bitcoinsv.bsvcl.net.protocol.messages.PongMsg message = null
            ByteArrayReader byteReader = new ByteArrayReader(Utils.HEX.decode(REF_BODY_PONG_MSG))
        when:
            message = io.bitcoinsv.bsvcl.net.protocol.serialization.PongMsgSerializer.getInstance().deserialize(context, byteReader)

        then:
            message.getNonce() == REF_BODY_NONCE
        where:
            byteInterval | delayMs
                10       |    25

    }

    def "testing Pong Message Body Serializing"() {
        given:
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig config = io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext context = io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .build()
            io.bitcoinsv.bsvcl.net.protocol.messages.PongMsg message = io.bitcoinsv.bsvcl.net.protocol.messages.PongMsg.builder().nonce(REF_BODY_NONCE).build()
            ByteArrayWriter byteWriter = new ByteArrayWriter()
            String messageSerialized = null
        when:
            io.bitcoinsv.bsvcl.net.protocol.serialization.PongMsgSerializer.getInstance().serialize(context, message, byteWriter)
            messageSerialized = Utils.HEX.encode(byteWriter.reader().getFullContent())
            byteWriter.reader()
        then:
            messageSerialized.equals(REF_BODY_PONG_MSG)
    }

    def "testing Pong Message COMPLETE de-serializing"(int byteInterval, int delayMs) {
        given:
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig config = io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext context = io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext.builder()
                .protocolBasicConfig(config.getBasicConfig())
                .build()
            ByteArrayReader byteReader = io.bitcoinsv.bsvcl.net.unit.protocol.tools.ByteArrayArtificalStreamProducer.stream(Utils.HEX.decode(REF_PONG_MSG), byteInterval, delayMs)
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.BitcoinMsgSerializer bitcoinSerializer = new io.bitcoinsv.bsvcl.net.protocol.serialization.common.BitcoinMsgSerializerImpl()
        when:
            io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsg<io.bitcoinsv.bsvcl.net.protocol.messages.PongMsg> message = bitcoinSerializer.deserialize(context, byteReader)
        then:
            message.getHeader().getMagic().equals(config.getBasicConfig().getMagicPackage())
            message.getHeader().getCommand().toUpperCase().equals(io.bitcoinsv.bsvcl.net.protocol.messages.PongMsg.MESSAGE_TYPE.toUpperCase())
            message.getBody().nonce == REF_PONG_NONCE
        where:
            byteInterval | delayMs
                10       |    25
    }

    def "testing Pong Message COMPLETE Serializing"() {
        given:
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig config = io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext context = io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .build()
            io.bitcoinsv.bsvcl.net.protocol.messages.PongMsg pongBody = io.bitcoinsv.bsvcl.net.protocol.messages.PongMsg.builder().nonce(REF_PONG_NONCE).build()
            io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsg<io.bitcoinsv.bsvcl.net.protocol.messages.PongMsg> bitcoinPongMsg = new io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsgBuilder<>(config.getBasicConfig(), pongBody).build()
        io.bitcoinsv.bsvcl.net.protocol.serialization.common.BitcoinMsgSerializer bitcoinSerializer = new io.bitcoinsv.bsvcl.net.protocol.serialization.common.BitcoinMsgSerializerImpl()
        when:
            byte[] pongMsgBytes = bitcoinSerializer.serialize(context, bitcoinPongMsg).getFullContent()
            String pongMsgDeserialzed = Utils.HEX.encode(pongMsgBytes)
        then:
            pongMsgDeserialzed.equals(REF_PONG_MSG)
    }
}
