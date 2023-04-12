package io.bitcoinsv.bsvcl.net.protocol.serialization

import io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig
import io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder
import io.bitcoinsv.bsvcl.net.protocol.messages.HashMsg
import io.bitcoinsv.bsvcl.net.protocol.messages.TxOutPointMsg
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext
import io.bitcoinsv.bsvcl.net.protocol.tools.ByteArrayArtificalStreamProducer
import io.bitcoinsv.bsvcl.common.bytes.ByteArrayReader
import io.bitcoinsv.bsvcl.common.bytes.ByteArrayWriter
import io.bitcoinsv.bitcoinjsv.core.Sha256Hash
import io.bitcoinsv.bitcoinjsv.core.Utils
import io.bitcoinsv.bitcoinjsv.params.MainNetParams
import io.bitcoinsv.bitcoinjsv.params.Net
import spock.lang.Specification

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 *
 * @date 24/09/2019
 * Testing class for the OutPoint Message Serialization.
 * The test is taken the assumption that we have already a correct serialization version of this Message, obtained
 * from another source that we trust (in this case the Java BitcoinJ library). So we serialize/deserialize some
 * messages with out code and compare the results with that reference.
 */
class TxOutPointMsgSerializerSpec extends Specification {
    // Whole TxOutpoint in Hex format
    public static final String REF_FULL_MSG = "bad09aa61d4fff3bba3fb8537dedd6db898996303ac2107060e430c16bb2208f01000000"
    // 'Hash' field in Hex format
    public static final byte[] REF_HASH = Sha256Hash.wrap("bad09aa61d4fff3bba3fb8537dedd6db898996303ac2107060e430c16bb2208f").getBytes();
    // 'index' field
    public static final int REF_INDEX = 1

   def "Testing HashMsg Deserializing"(int byteInterval, int delayMs) {
       given:
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            DeserializerContext context = DeserializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .build()
            TxOutPointMsgSerializer serializer = TxOutPointMsgSerializer.getInstance()
            TxOutPointMsg message
       when:
           ByteArrayReader byteReader = ByteArrayArtificalStreamProducer.stream(Utils.HEX.decode(REF_FULL_MSG), byteInterval, delayMs)
           message = serializer.deserialize(context, byteReader)
       then:
            message.getHash().hashBytes == REF_HASH
            message.index == REF_INDEX
            message.getMessageType() == TxOutPointMsg.MESSAGE_TYPE
       where:
           byteInterval | delayMs
               10       |    25
   }

   def "Testing HashMsg Serializing"() {
       given:
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            SerializerContext context = SerializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .build()
           HashMsg hash = HashMsg.builder().hash(REF_HASH).build();

           TxOutPointMsg message = TxOutPointMsg.builder().hash(hash).index(REF_INDEX).build()
           TxOutPointMsgSerializer serializer = TxOutPointMsgSerializer.getInstance()
           String messageSerializedBytes
       when:
            ByteArrayWriter byteWriter = new ByteArrayWriter()
            serializer.serialize(context, message, byteWriter)
            messageSerializedBytes =  Utils.HEX.encode(byteWriter.reader().getFullContent())
       then:
            messageSerializedBytes == REF_FULL_MSG
   }
}
