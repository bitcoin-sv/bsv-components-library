package com.nchain.jcl.protocol.unit.serialization

import com.nchain.jcl.protocol.config.ProtocolConfig
import com.nchain.jcl.protocol.config.provided.ProtocolBSVMainConfig
import com.nchain.jcl.protocol.messages.VarIntMsg
import com.nchain.jcl.protocol.serialization.VarIntMsgSerializer
import com.nchain.jcl.protocol.serialization.common.DeserializerContext
import com.nchain.jcl.protocol.serialization.common.SerializerContext
import com.nchain.jcl.tools.bytes.HEX
import com.nchain.jcl.tools.bytes.ByteArrayReader
import com.nchain.jcl.tools.bytes.ByteArrayWriter
import spock.lang.Specification
/**
 * Testing class for the VarIntMSg Message Serialization.
 * The test is taken the assumption that we have already a correct serialization version of this Message, obtained
 * from another source that we trust (in this case the Java BitcoinJ library). So we serialize/deserialize some
 * messages with out code and compare the results with that reference.
 */
class VarIntSerializationSpec extends Specification {

    // This is a VarInt  Serialized in HEX format, generated by a third party (bitcoinJ)
    // The following Serialized value for an Address has been produced using:
    // - value: 543

    private static final String REF_VARINT_MSG = "fd1f02"
    private static final int VAR_VALUE = 543

    def "Testing VarInt Deserializing"() {
        given:
            ProtocolConfig config = new ProtocolBSVMainConfig()
            DeserializerContext context = DeserializerContext.builder()
                    .protocolconfig(config)
                    .build()
            VarIntMsgSerializer serializer = VarIntMsgSerializer.getInstance()
            VarIntMsg message = null
        when:
            message = serializer.deserialize(context, new ByteArrayReader(HEX.decode(REF_VARINT_MSG)))
        then:
            message.getValue() == VAR_VALUE
    }

    def "Testing VarInt Serializing"() {
        given:
            ProtocolConfig config = new ProtocolBSVMainConfig()
            SerializerContext context = SerializerContext.builder()
                    .protocolconfig(config)
                    .build()
            VarIntMsg message = VarIntMsg.builder().value(VAR_VALUE).build()
            VarIntMsgSerializer serializer = VarIntMsgSerializer.getInstance()
            String messageBytesStr = null
        when:
            ByteArrayWriter byteWriter = new ByteArrayWriter()
            serializer.serialize(context, message, byteWriter)
            byte[] messageBytes = byteWriter.reader().getFullContent()
            messageBytesStr = HEX.encode(messageBytes)
        then:
            messageBytesStr.equals(REF_VARINT_MSG)
    }
}
