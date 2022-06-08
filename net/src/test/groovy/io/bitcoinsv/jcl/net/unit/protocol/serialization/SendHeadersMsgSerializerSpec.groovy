package io.bitcoinsv.jcl.net.unit.protocol.serialization

import io.bitcoinsv.jcl.net.protocol.config.ProtocolBasicConfig
import io.bitcoinsv.jcl.net.protocol.config.ProtocolConfigBuilder
import io.bitcoinsv.jcl.net.protocol.messages.HeaderMsg
import io.bitcoinsv.jcl.net.protocol.messages.SendHeadersMsg
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsg
import io.bitcoinsv.jcl.net.protocol.serialization.SendHeadersMsgSerializer
import io.bitcoinsv.jcl.net.protocol.serialization.common.BitcoinMsgSerializer
import io.bitcoinsv.jcl.net.protocol.serialization.common.BitcoinMsgSerializerImpl
import io.bitcoinsv.jcl.net.protocol.serialization.common.DeserializerContext
import io.bitcoinsv.jcl.net.protocol.serialization.common.SerializerContext
import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader
import io.bitcoinsv.jcl.tools.bytes.ByteArrayWriter
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
            ProtocolBasicConfig protocolBasicConfig = ProtocolConfigBuilder.get(MainNetParams.get()).getBasicConfig()
            SerializerContext serializerContext = SerializerContext.builder()
                    .protocolBasicConfig(protocolBasicConfig)
                    .build()

            DeserializerContext deserializerContext = DeserializerContext.builder()
                    .protocolBasicConfig(protocolBasicConfig)
                    .build()

        SendHeadersMsg sendHeadersMsg = SendHeadersMsg.builder().build()

            ByteArrayWriter byteWriter = new ByteArrayWriter()
        when:
        SendHeadersMsgSerializer.getInstance().serialize(serializerContext, sendHeadersMsg, byteWriter)
            SendHeadersMsg deserializedSendHeadersMsg = SendHeadersMsgSerializer.getInstance().deserialize(deserializerContext, byteWriter.reader())
        then:
            sendHeadersMsg.equals(deserializedSendHeadersMsg)
    }


    def "testing SendHeadersMsg COMPLETE Serializing and Deserializing"() {
        given:
            ProtocolBasicConfig protocolBasicConfig = ProtocolConfigBuilder.get(MainNetParams.get()).getBasicConfig()
            SerializerContext serializerContext = SerializerContext.builder()
                    .protocolBasicConfig(protocolBasicConfig)
                    .build()

            DeserializerContext deserializerContext = DeserializerContext.builder()
                    .protocolBasicConfig(protocolBasicConfig)
                    .build()

            SendHeadersMsg sendHeadersMsg = SendHeadersMsg.builder().build()

        HeaderMsg headerMsg = HeaderMsg.builder()
                    .checksum(3806393949)
                    .command(SendHeadersMsg.MESSAGE_TYPE)
                    .length((int)sendHeadersMsg.getLengthInBytes())
                    .magic(1)
                    .build();


        BitcoinMsg<SendHeadersMsg> sendHeadersBitcoinMsg = new BitcoinMsg<SendHeadersMsg>(headerMsg, sendHeadersMsg);

        BitcoinMsgSerializer serializer = BitcoinMsgSerializerImpl.getInstance()
        when:
            ByteArrayReader byteArrayReader = serializer.serialize(serializerContext, sendHeadersBitcoinMsg)
            BitcoinMsg<SendHeadersMsg> deserializedSendHeadersBitcoinMsg = serializer.deserialize(deserializerContext, byteArrayReader)
        then:
            sendHeadersBitcoinMsg.equals(deserializedSendHeadersBitcoinMsg)

    }

}