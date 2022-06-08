package io.bitcoinsv.jcl.net.unit.protocol.serialization

import io.bitcoinsv.jcl.net.protocol.config.ProtocolConfig
import io.bitcoinsv.jcl.net.protocol.config.ProtocolConfigBuilder
import io.bitcoinsv.jcl.net.protocol.messages.HashMsg
import io.bitcoinsv.jcl.net.protocol.serialization.HashMsgSerializer
import io.bitcoinsv.jcl.net.protocol.serialization.common.DeserializerContext
import io.bitcoinsv.jcl.net.protocol.serialization.common.SerializerContext
import io.bitcoinsv.jcl.net.unit.protocol.tools.ByteArrayArtificalStreamProducer
import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader
import io.bitcoinsv.jcl.tools.bytes.ByteArrayWriter
import io.bitcoinsv.bitcoinjsv.core.Sha256Hash
import io.bitcoinsv.bitcoinjsv.params.MainNetParams
import io.bitcoinsv.bitcoinjsv.params.Net
import spock.lang.Specification

/**
 * @author m.jose@nchain.com
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 18/09/2019
 *
 *
 * Testing class for the Hash Message Serialization.
 * The test is taken the assumption that we have already a correct serialization version of this Message, obtained
 * from another source that we trust (in this case the Java BitcoinJ library). So we serialize/deserialize some
 * messages with out code and compare the results with that reference.
 */
class HashMsgSerializerSpec extends Specification {

    // Hash Content: a String containing a Test Message.
    // This is the bytes we are using as the content of the Hash.
    public static final byte[] REF_HASH_CONTENT_BYTES = "Testing Serialization of HashMsg".getBytes();

    // Serialized version of the Hash (generated by bitcoinJ)
    public static final byte[] REF_HASH_BYTES = Sha256Hash.wrap("54657374696e672053657269616c697a6174696f6e206f6620486173684d7367").getBytes();

    def "Testing HashMsg Deserializing"(int byteInterval, int delayMs) {
        given:
        ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
        DeserializerContext context = DeserializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .build()
        HashMsgSerializer serializer = HashMsgSerializer.getInstance()
        HashMsg message
        ByteArrayReader byteReader = ByteArrayArtificalStreamProducer.stream(REF_HASH_BYTES, byteInterval, delayMs);
        when:

             message = serializer.deserialize(context, byteReader)
        then:
             message.getHashBytes() == REF_HASH_CONTENT_BYTES
             message.messageType == HashMsg.MESSAGE_TYPE
        where:
            byteInterval | delayMs
                10       |    15
    }

    def "Testing HashMsg Serializing"() {
        given:
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
        SerializerContext context = SerializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .build()
            HashMsg message = HashMsg.builder().hash(REF_HASH_CONTENT_BYTES).build()
            HashMsgSerializer serializer = HashMsgSerializer.getInstance()
            byte[] messageSerializedBytes
        when:
            ByteArrayWriter byteWriter = new ByteArrayWriter()
            serializer.serialize(context, message, byteWriter)
            messageSerializedBytes = byteWriter.reader().getFullContent()
        then:
            messageSerializedBytes == REF_HASH_BYTES
    }
}
