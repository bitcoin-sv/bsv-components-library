package io.bitcoinsv.bsvcl.net.unit.protocol.serialization

import io.bitcoinsv.bsvcl.net.protocol.config.ProtocolBasicConfig
import io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder
import io.bitcoinsv.bsvcl.net.protocol.messages.HeaderMsg
import io.bitcoinsv.bsvcl.net.protocol.messages.MemPoolMsg
import io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsg
import io.bitcoinsv.bsvcl.net.protocol.serialization.MemPoolMsgSerializer
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.BitcoinMsgSerializer
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.BitcoinMsgSerializerImpl
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext
import io.bitcoinsv.bsvcl.common.bytes.ByteArrayReader
import io.bitcoinsv.bsvcl.common.bytes.ByteArrayWriter
import io.bitcoinsv.bitcoinjsv.params.MainNetParams
import spock.lang.Specification

/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 20/08/2019
 *
 * Testing class for the MemPoolMsg Serialization and deserialization.
 */
class MemPoolMsgSerializerSpec extends Specification {

    def "testing MemPoolMsg Serializing and Deserializing"() {
        given:
            ProtocolBasicConfig protocolBasicConfig = ProtocolConfigBuilder.get(MainNetParams.get()).getBasicConfig()
            SerializerContext serializerContext = SerializerContext.builder()
                    .protocolBasicConfig(protocolBasicConfig)
                    .build()

            DeserializerContext deserializerContext = DeserializerContext.builder()
                    .protocolBasicConfig(protocolBasicConfig)
                    .build()

            MemPoolMsg memPoolMsg = MemPoolMsg.builder().build()

            ByteArrayWriter byteWriter = new ByteArrayWriter()
        when:
            MemPoolMsgSerializer.getInstance().serialize(serializerContext, memPoolMsg, byteWriter)
            MemPoolMsg deserializedMemPoolMsg = MemPoolMsgSerializer.getInstance().deserialize(deserializerContext, byteWriter.reader())
        then:
            memPoolMsg.equals(deserializedMemPoolMsg)
    }


    def "testing MemPoolMsg COMPLETE Serializing and Deserializing"() {
        given:
            ProtocolBasicConfig protocolBasicConfig = ProtocolConfigBuilder.get(MainNetParams.get()).getBasicConfig()
            SerializerContext serializerContext = SerializerContext.builder()
                    .protocolBasicConfig(protocolBasicConfig)
                    .build()

            DeserializerContext deserializerContext = DeserializerContext.builder()
                    .protocolBasicConfig(protocolBasicConfig)
                    .build()

            MemPoolMsg memPoolMsg = MemPoolMsg.builder().build()

            HeaderMsg headerMsg = HeaderMsg.builder()
                    .checksum(3806393949)
                    .command(MemPoolMsg.MESSAGE_TYPE)
                    .length((int)memPoolMsg.getLengthInBytes())
                    .magic(1)
                    .build();


            BitcoinMsg<MemPoolMsg> memPoolBitcoinMsg = new BitcoinMsg<MemPoolMsg>(headerMsg, memPoolMsg);

            BitcoinMsgSerializer serializer = BitcoinMsgSerializerImpl.getInstance()
        when:
            ByteArrayReader byteArrayReader = serializer.serialize(serializerContext, memPoolBitcoinMsg)
            BitcoinMsg<MemPoolMsg> deserializedMemPoolBitcoinMsg = serializer.deserialize(deserializerContext, byteArrayReader)
        then:
            memPoolBitcoinMsg.equals(deserializedMemPoolBitcoinMsg)

    }

}