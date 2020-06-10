package com.nchain.jcl.protocol.unit.serialization

import com.nchain.jcl.protocol.config.ProtocolConfig
import com.nchain.jcl.protocol.config.provided.ProtocolBSVMainConfig
import com.nchain.jcl.protocol.messages.TxOutputMessage
import com.nchain.jcl.protocol.serialization.TxOutputMessageSerializer
import com.nchain.jcl.protocol.serialization.common.DeserializerContext
import com.nchain.jcl.protocol.serialization.common.SerializerContext
import com.nchain.jcl.protocol.unit.tools.ByteArrayArtificalStreamProducer
import com.nchain.jcl.tools.bytes.HEX
import com.nchain.jcl.tools.crypto.Sha256Wrapper
import com.nchain.jcl.tools.bytes.ByteArrayReader
import com.nchain.jcl.tools.bytes.ByteArrayWriter
import spock.lang.Specification
/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 01/10/2019
 *
 * Testing class for the TxOutput message Serialization.
 * The test is taken the assumption that we have already a correct serialization version of this Message, obtained
 * from another source that we trust (in this case the Java BitcoinJ library). So we serialize/deserialize some
 * messages with out code and compare the results with that reference.
 */
class TxOutputMessageSerializerSpec extends Specification {

    public static final String REF_MSG = "00f2052a01000000202b801dd82f01d17bbde881687bf72bc62e2faa8ab8133d36fcb8c3abe7459da6"

    public static final byte[] REF_BITES = Sha256Wrapper.wrap("2b801dd82f01d17bbde881687bf72bc62e2faa8ab8133d36fcb8c3abe7459da6").getBytes();
    //2b801dd82f01d17bbde881687bf72bc62e2faa8ab8133d36fcb8c3abe7459da6
    public static final long COIN_VALUE = 5000000000

    def "Testing TxOutputMessage Deserializing"(int byteInterval, int delayMs) {
        given:
            ProtocolConfig config = new ProtocolBSVMainConfig()
            DeserializerContext context = DeserializerContext.builder()
                    .protocolconfig(config)
                    .build()
            TxOutputMessageSerializer serializer = TxOutputMessageSerializer.getInstance()
            TxOutputMessage  message
            ByteArrayReader byteReader = ByteArrayArtificalStreamProducer.stream(HEX.decode(REF_MSG), byteInterval, delayMs)
        when:
            message = serializer.deserialize(context, byteReader)
        then:

            message.getMessageType() == TxOutputMessage.MESSAGE_TYPE
            message.txValue == COIN_VALUE
            message.pk_script == REF_BITES
        where:
            byteInterval | delayMs
                10       |    50

    }


    def "Testing TxOutputMessage Serializing"() {
        given:
            ProtocolConfig config = new ProtocolBSVMainConfig()
            SerializerContext context = SerializerContext.builder()
                    .protocolconfig(config)
                    .build()
            TxOutputMessageSerializer serializer = TxOutputMessageSerializer.getInstance()
            TxOutputMessage txOutputMessage = TxOutputMessage.builder().txValue(COIN_VALUE).pk_script(REF_BITES).build();

            String messageSerializedBytes
        when:
            ByteArrayWriter byteWriter = new ByteArrayWriter()
            serializer.serialize(context, txOutputMessage, byteWriter)
            messageSerializedBytes =  HEX.encode(byteWriter.reader().getFullContent())
        then:
            messageSerializedBytes == REF_MSG
    }

}
