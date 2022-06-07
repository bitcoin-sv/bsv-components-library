package io.bitcoinsv.jcl.net.unit.protocol.serialization

import io.bitcoinsv.bitcoinjsv.core.Sha256Hash
import io.bitcoinsv.bitcoinjsv.core.Utils
import io.bitcoinsv.bitcoinjsv.params.MainNetParams
import io.bitcoinsv.bitcoinjsv.params.Net
import io.bitcoinsv.jcl.net.protocol.config.ProtocolBasicConfig
import io.bitcoinsv.jcl.net.protocol.config.ProtocolConfig
import io.bitcoinsv.jcl.net.protocol.config.ProtocolConfigBuilder
import io.bitcoinsv.jcl.net.protocol.messages.ByteStreamMsg
import io.bitcoinsv.jcl.net.protocol.messages.GetdataMsg
import io.bitcoinsv.jcl.net.protocol.messages.HashMsg
import io.bitcoinsv.jcl.net.protocol.messages.InventoryVectorMsg
import io.bitcoinsv.jcl.net.protocol.messages.MemPoolMsg
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsg
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsgBuilder
import io.bitcoinsv.jcl.net.protocol.serialization.ByteStreamMsgSerializer
import io.bitcoinsv.jcl.net.protocol.serialization.GetdataMsgSerializer
import io.bitcoinsv.jcl.net.protocol.serialization.MemPoolMsgSerializer
import io.bitcoinsv.jcl.net.protocol.serialization.common.BitcoinMsgSerializer
import io.bitcoinsv.jcl.net.protocol.serialization.common.BitcoinMsgSerializerImpl
import io.bitcoinsv.jcl.net.protocol.serialization.common.DeserializerContext
import io.bitcoinsv.jcl.net.protocol.serialization.common.SerializerContext
import io.bitcoinsv.jcl.net.unit.protocol.tools.ByteArrayArtificalStreamProducer
import io.bitcoinsv.jcl.tools.bytes.ByteArrayBuffer
import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader
import io.bitcoinsv.jcl.tools.bytes.ByteArrayWriter
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
