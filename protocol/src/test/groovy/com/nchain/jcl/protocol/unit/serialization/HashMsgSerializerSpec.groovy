package com.nchain.jcl.protocol.unit.serialization

import com.nchain.jcl.protocol.config.ProtocolConfig
import com.nchain.jcl.protocol.config.provided.ProtocolBSVMainConfig
import com.nchain.jcl.protocol.messages.HashMsg
import com.nchain.jcl.protocol.serialization.HashMsgSerializer
import com.nchain.jcl.protocol.serialization.common.DeserializerContext
import com.nchain.jcl.protocol.serialization.common.SerializerContext
import com.nchain.jcl.protocol.unit.tools.ByteArrayArtificalStreamProducer
import com.nchain.jcl.tools.crypto.Sha256Wrapper
import com.nchain.jcl.tools.bytes.ByteArrayReader
import com.nchain.jcl.tools.bytes.ByteArrayWriter
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
    public static final byte[] REF_HASH_BYTES = Sha256Wrapper.wrap("54657374696e672053657269616c697a6174696f6e206f6620486173684d7367").getBytes();

    def "Testing HashMsg Deserializing"(int byteInterval, int delayMs) {
        given:
            ProtocolConfig config = new ProtocolBSVMainConfig()
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
            ProtocolConfig config = new ProtocolBSVMainConfig()
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
