package com.nchain.jcl.net.unit.protocol.serialization

import com.nchain.jcl.net.protocol.config.ProtocolConfig
import com.nchain.jcl.net.protocol.config.provided.ProtocolBSVMainConfig
import com.nchain.jcl.net.protocol.messages.HashMsg
import com.nchain.jcl.net.protocol.messages.TxInputMsg
import com.nchain.jcl.net.protocol.messages.TxOutPointMsg
import com.nchain.jcl.net.protocol.serialization.TxInputMsgSerializer
import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext
import com.nchain.jcl.net.protocol.serialization.common.SerializerContext
import com.nchain.jcl.net.unit.protocol.tools.ByteArrayArtificalStreamProducer
import com.nchain.jcl.tools.bytes.ByteArrayReader
import com.nchain.jcl.tools.bytes.ByteArrayReaderOptimized
import com.nchain.jcl.tools.bytes.ByteArrayWriter
import io.bitcoinj.core.Sha256Hash
import io.bitcoinj.core.Utils
import spock.lang.Specification

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 26/09/2019
 *
 * Testing class for the TxInput message Serialization.
 * The test is taken the assumption that we have already a correct serialization version of this Message, obtained
 * from another source that we trust (in this case the Java BitcoinJ library). So we serialize/deserialize some
 * messages with out code and compare the results with that reference.
 */
class TxInputMsgSerializerSpec extends Specification {

    public static final String REF_MSG = "a69d45e7abc3b8fc363d13b88aaa2f2ec62bf77b6881e8bd7bd1012fd81d802b00000000010000000100"

    public static final byte[] REF_BITES = Sha256Hash.wrap("2b801dd82f01d17bbde881687bf72bc62e2faa8ab8133d36fcb8c3abe7459da6").getBytes();
    public static final long SEQUENCE = 65536

    def "Testing TxInputMessage Deserializing"(int byteInterval, int delayMs) {
        given:
            ProtocolConfig config = new ProtocolBSVMainConfig()
            DeserializerContext context = DeserializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .build()
            TxInputMsgSerializer serializer = TxInputMsgSerializer.getInstance()
            TxInputMsg message
        when:
            ByteArrayReader byteReader = ByteArrayArtificalStreamProducer.stream(Utils.HEX.decode(REF_MSG), byteInterval, delayMs)
           message = serializer.deserialize(context, new ByteArrayReaderOptimized(byteReader))
        then:
           message.getSequence()  == SEQUENCE
           message.getMessageType() == TxInputMsg.MESSAGE_TYPE
        where:
            byteInterval | delayMs
                10       |    25
    }


    def "Testing TxInputMessage Serializing"() {
        given:
            ProtocolConfig config = new ProtocolBSVMainConfig()
            SerializerContext context = SerializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .build()
            TxInputMsgSerializer serializer = TxInputMsgSerializer.getInstance()
            HashMsg hash = HashMsg.builder().hash(REF_BITES).build();

            TxOutPointMsg txOutPointMessage =  TxOutPointMsg.builder().hash(hash).index(0).build()
            TxInputMsg txInputMessage = TxInputMsg.builder().pre_outpoint(txOutPointMessage)
                        .sequence(SEQUENCE)
                        .signature_script(new byte[1]).build()
            String messageSerializedBytes
        when:
            ByteArrayWriter byteWriter = new ByteArrayWriter()
            serializer.serialize(context, txInputMessage, byteWriter)
            messageSerializedBytes =  Utils.HEX.encode(byteWriter.reader().getFullContent())
        then:
             messageSerializedBytes == REF_MSG
    }
}
