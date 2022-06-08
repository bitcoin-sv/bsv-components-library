package io.bitcoinsv.jcl.net.unit.protocol.serialization

import io.bitcoinsv.jcl.net.protocol.messages.FeeFilterMsg
import io.bitcoinsv.jcl.net.protocol.serialization.FeeFilterMsgSerializer
import io.bitcoinsv.jcl.net.protocol.serialization.common.DeserializerContext
import io.bitcoinsv.jcl.net.protocol.serialization.common.SerializerContext
import io.bitcoinsv.jcl.net.unit.protocol.tools.ByteArrayArtificalStreamProducer
import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader
import io.bitcoinsv.jcl.tools.bytes.ByteArrayWriter
import spock.lang.Specification

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 05/08/2019 14:33
 *
 * Testing class for the FeeFilterMsg Message Serialization.
 * All the testing class for Serializer in this pacakge follow the same method: We take a serilized version of the
 * Message we are testing from a third-party source we trust: bitcoinJ in most cases, and then we compare our
 * serialization with the one produced by bitcoinJ.
 * In case of the Fee Message, bitcoinJ does NOT provide an implementation for that, so we are going to rely only
 * on our implementation, checking that the Serialization and Deserialization processes are consistent.
 */
class FeeFilterMsgSerializerSpec extends Specification {


    def "testing Serialization/Deserialization"(int byteInterval, int delayMs) {
        given:
        FeeFilterMsg msg = FeeFilterMsg.builder().fee(555).build()
        SerializerContext serializerContext = SerializerContext.builder().build();
        DeserializerContext deserializerContext = DeserializerContext.builder().build()
        when:

            // First we Serialize the Message:
            ByteArrayWriter writer = new ByteArrayWriter()
        FeeFilterMsgSerializer.getInstance().serialize(serializerContext, msg, writer)

            ByteArrayReader byteReader = ByteArrayArtificalStreamProducer.stream(writer.reader().getFullContent(), byteInterval, delayMs);

            // Now we deserialize it back and compare it to the initial version
            FeeFilterMsg newMsg = FeeFilterMsgSerializer.getInstance().deserialize(deserializerContext, byteReader);
        then:
            msg.getFee() == newMsg.getFee()
        where:
            byteInterval | delayMs
                10       |    15
    }
}
