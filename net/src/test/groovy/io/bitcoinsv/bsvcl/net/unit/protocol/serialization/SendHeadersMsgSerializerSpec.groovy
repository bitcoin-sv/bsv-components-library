package io.bitcoinsv.bsvcl.net.unit.protocol.serialization


import io.bitcoinsv.bsvcl.common.bytes.ByteArrayReader
import io.bitcoinsv.bsvcl.common.bytes.ByteArrayWriter
import io.bitcoinsv.bitcoinjsv.params.MainNetParams
import spock.lang.Specification

/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 20/08/2019
 *
 * Testing class for the SendHeadersMsg Serialization and Deserialization
 */
class SendHeadersMsgSerializerSpec extends Specification {

    def "testing SendHeadersMsg Serializing and Deserializing"() {
        given:
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolBasicConfig protocolBasicConfig = io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder.get(MainNetParams.get()).getBasicConfig()
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext serializerContext = io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext.builder()
                    .protocolBasicConfig(protocolBasicConfig)
                    .build()

            io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext deserializerContext = io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext.builder()
                    .protocolBasicConfig(protocolBasicConfig)
                    .build()

            io.bitcoinsv.bsvcl.net.protocol.messages.SendHeadersMsg sendHeadersMsg = io.bitcoinsv.bsvcl.net.protocol.messages.SendHeadersMsg.builder().build()

            ByteArrayWriter byteWriter = new ByteArrayWriter()
        when:
            io.bitcoinsv.bsvcl.net.protocol.serialization.SendHeadersMsgSerializer.getInstance().serialize(serializerContext, sendHeadersMsg, byteWriter)
            io.bitcoinsv.bsvcl.net.protocol.messages.SendHeadersMsg deserializedSendHeadersMsg = io.bitcoinsv.bsvcl.net.protocol.serialization.SendHeadersMsgSerializer.getInstance().deserialize(deserializerContext, byteWriter.reader())
        then:
            sendHeadersMsg.equals(deserializedSendHeadersMsg)
    }


    def "testing SendHeadersMsg COMPLETE Serializing and Deserializing"() {
        given:
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolBasicConfig protocolBasicConfig = io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder.get(MainNetParams.get()).getBasicConfig()
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext serializerContext = io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext.builder()
                    .protocolBasicConfig(protocolBasicConfig)
                    .build()

            io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext deserializerContext = io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext.builder()
                    .protocolBasicConfig(protocolBasicConfig)
                    .build()

            io.bitcoinsv.bsvcl.net.protocol.messages.SendHeadersMsg sendHeadersMsg = io.bitcoinsv.bsvcl.net.protocol.messages.SendHeadersMsg.builder().build()

            io.bitcoinsv.bsvcl.net.protocol.messages.HeaderMsg headerMsg = io.bitcoinsv.bsvcl.net.protocol.messages.HeaderMsg.builder()
                    .checksum(3806393949)
                    .command(io.bitcoinsv.bsvcl.net.protocol.messages.SendHeadersMsg.MESSAGE_TYPE)
                    .length((int)sendHeadersMsg.getLengthInBytes())
                    .magic(1)
                    .build();


            io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsg<io.bitcoinsv.bsvcl.net.protocol.messages.SendHeadersMsg> sendHeadersBitcoinMsg = new io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsg<io.bitcoinsv.bsvcl.net.protocol.messages.SendHeadersMsg>(headerMsg, sendHeadersMsg);

            io.bitcoinsv.bsvcl.net.protocol.serialization.common.BitcoinMsgSerializer serializer = io.bitcoinsv.bsvcl.net.protocol.serialization.common.BitcoinMsgSerializerImpl.getInstance()
        when:
            ByteArrayReader byteArrayReader = serializer.serialize(serializerContext, sendHeadersBitcoinMsg)
            io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsg<io.bitcoinsv.bsvcl.net.protocol.messages.SendHeadersMsg> deserializedSendHeadersBitcoinMsg = serializer.deserialize(deserializerContext, byteArrayReader)
        then:
            sendHeadersBitcoinMsg.equals(deserializedSendHeadersBitcoinMsg)

    }

}