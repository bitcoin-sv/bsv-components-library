package io.bitcoinsv.bsvcl.net.unit.protocol.serialization


import io.bitcoinsv.bitcoinjsv.params.MainNetParams
import io.bitcoinsv.bsvcl.net.protocol.config.ProtocolBasicConfig
import io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder
import io.bitcoinsv.bsvcl.net.protocol.messages.ByteStreamMsg
import io.bitcoinsv.bsvcl.net.protocol.serialization.ByteStreamMsgSerializer
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext
import io.bitcoinsv.bsvcl.tools.bytes.ByteArrayBuffer
import io.bitcoinsv.bsvcl.tools.bytes.ByteArrayWriter
import spock.lang.Specification

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 27/08/2019
 *
 * Testing class for the ByteStreamMsg Serialization. This is a very straight forward class that just copies the bytes from one array to another
 *
 */
class ByteStreamMsgSerializerSpec extends Specification {


    def "testing ByteStreamMsg BODY serialize/deserialize"() {
        given:
        ProtocolBasicConfig protocolBasicConfig = ProtocolConfigBuilder.get(MainNetParams.get()).getBasicConfig()
        SerializerContext serializerContext = SerializerContext.builder()
                .protocolBasicConfig(protocolBasicConfig)
                .build()

        DeserializerContext deserializerContext = DeserializerContext.builder()
                .protocolBasicConfig(protocolBasicConfig)
                .build()

        ByteArrayBuffer buffer = new ByteArrayBuffer();
        for(int i = 0; i < 1_000_000; i++){
            buffer.add("test".getBytes());
        }
        ByteStreamMsg byteStreamMsg = new ByteStreamMsg(buffer)

        ByteArrayWriter byteWriter = new ByteArrayWriter()
        when:
        ByteStreamMsgSerializer.getInstance().serialize(serializerContext, byteStreamMsg, byteWriter)
        ByteStreamMsg deserializedByteStreamMsg = ByteStreamMsgSerializer.getInstance().deserialize(deserializerContext, byteWriter.reader())
        then:
        byteStreamMsg == deserializedByteStreamMsg
    }
}
