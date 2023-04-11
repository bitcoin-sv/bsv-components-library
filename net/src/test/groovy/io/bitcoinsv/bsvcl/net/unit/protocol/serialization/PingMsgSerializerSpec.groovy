package io.bitcoinsv.bsvcl.net.unit.protocol.serialization


import io.bitcoinsv.bsvcl.common.bytes.ByteArrayReader
import io.bitcoinsv.bsvcl.common.bytes.ByteArrayWriter
import io.bitcoinsv.bitcoinjsv.core.Utils
import io.bitcoinsv.bitcoinjsv.params.MainNetParams
import io.bitcoinsv.bitcoinjsv.params.Net
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
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig config = io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext context = io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .build()
            io.bitcoinsv.bsvcl.net.protocol.messages.PingMsg message = null
            ByteArrayReader byteReader = new ByteArrayReader(Utils.HEX.decode(REF_BODY_PING_MSG))

        when:
            message = io.bitcoinsv.bsvcl.net.protocol.serialization.PingMsgSerializer.getInstance().deserialize(context, byteReader)

        then:
            message.getNonce() == REF_BODY_NONCE
        where:
            byteInterval | delayMs
                10       |    25

    }

    def "testing Ping Message Body Serializing"() {
        given:
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig config = io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext context = io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .build()
            io.bitcoinsv.bsvcl.net.protocol.messages.PingMsg message = io.bitcoinsv.bsvcl.net.protocol.messages.PingMsg.builder().nonce(REF_BODY_NONCE).build()
            ByteArrayWriter byteWriter = new ByteArrayWriter()
            String messageSerialized = null
        when:
            io.bitcoinsv.bsvcl.net.protocol.serialization.PingMsgSerializer.getInstance().serialize(context, message, byteWriter)
            messageSerialized = Utils.HEX.encode(byteWriter.reader().getFullContent())
        then:
            messageSerialized.equals(REF_BODY_PING_MSG)
    }

    def "testing Ping Message COMPLETE de-serializing"(int byteInterval, int delayMs) {
        given:
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig config = io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext context = io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .build()
            ByteArrayReader byteReader = new ByteArrayReader(Utils.HEX.decode(REF_PING_MSG))
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.BitcoinMsgSerializer bitcoinSerializer = new io.bitcoinsv.bsvcl.net.protocol.serialization.common.BitcoinMsgSerializerImpl()
        when:
            io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsg<io.bitcoinsv.bsvcl.net.protocol.messages.PingMsg> message = bitcoinSerializer.deserialize(context, byteReader)
        then:
            message.getHeader().getMagic().equals(config.getBasicConfig().getMagicPackage())
            message.getHeader().getCommand().toUpperCase().equals(io.bitcoinsv.bsvcl.net.protocol.messages.PingMsg.MESSAGE_TYPE.toUpperCase())
            message.getBody().nonce == REF_PING_NONCE
        where:
            byteInterval | delayMs
                10       |    25
    }

    def "testing Ping Message COMPLETE Serializing"() {
        given:
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig config = io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext context = io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .build()
            io.bitcoinsv.bsvcl.net.protocol.messages.PingMsg pingBodyMsg = io.bitcoinsv.bsvcl.net.protocol.messages.PingMsg.builder().nonce(REF_PING_NONCE).build()
            io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsg<io.bitcoinsv.bsvcl.net.protocol.messages.PingMsg> bitcoinPingMsg = new io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsgBuilder<>(config.getBasicConfig(), pingBodyMsg).build()
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.BitcoinMsgSerializer bitcoinSerializer = new io.bitcoinsv.bsvcl.net.protocol.serialization.common.BitcoinMsgSerializerImpl()
        when:
            byte[] pingMsgBytes = bitcoinSerializer.serialize(context, bitcoinPingMsg).getFullContent()
            String pingnMsgDeserialzed = Utils.HEX.encode(pingMsgBytes)
        then:
            pingnMsgDeserialzed.equals(REF_PING_MSG)
    }
}
