package io.bitcoinsv.bsvcl.net.protocol.serialization


import io.bitcoinsv.bsvcl.common.bytes.ByteArrayReader
import io.bitcoinsv.bsvcl.common.bytes.ByteArrayWriter
import io.bitcoinsv.bitcoinjsv.core.Utils
import io.bitcoinsv.bitcoinjsv.params.MainNetParams
import io.bitcoinsv.bitcoinjsv.params.Net
import io.bitcoinsv.bsvcl.net.protocol.tools.ByteArrayArtificalStreamProducer
import spock.lang.Specification

/**
 * Testing class for the VatStrMSg Message Serialization.
 * The test is taken the assumption that we have already a correct serialization version of this Message, obtained
 * from another source that we trust (in this case the Java BitcoinJ library). So we serialize/deserialize some
 * messages with out code and compare the results with that reference.
 */
class VarStrMsgSerializationSpec extends Specification {

    private static final String VARSTR_MSG_VARINT = "1f"
    private static final String VARSTR_MSG_VARSTR = "Testing Serialization of VarStr"

    def "testing De-serializing VarStr"(int byteInterval, int delayMs) {
        given:
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig config = io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext context = io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .build()
            io.bitcoinsv.bsvcl.net.protocol.serialization.VarStrMsgSerializer serializer = io.bitcoinsv.bsvcl.net.protocol.serialization.VarStrMsgSerializer.getinstance()
            io.bitcoinsv.bsvcl.net.protocol.messages.VarStrMsg message

        when:
            // We convert the String in a ByteArray, Careful here, since the "VarInt" part is in HEX format,
            // but the "VarStr" part is in Char Format:
            byte[] varIntBytes = Utils.HEX.decode(VARSTR_MSG_VARINT)
            byte[] varStrBytes = VARSTR_MSG_VARSTR.getBytes()
            byte[] varStrWhole = new byte[varIntBytes.length + varStrBytes.length]
            System.arraycopy(varIntBytes, 0, varStrWhole, 0, varIntBytes.length)
            System.arraycopy(varStrBytes, 0, varStrWhole, varIntBytes.length, varStrBytes.length)
            ByteArrayReader byteReader = ByteArrayArtificalStreamProducer.stream(varStrWhole, byteInterval, delayMs)
            message = serializer.deserialize(context, byteReader)
        then:
            message.getStr().equals("Testing Serialization of VarStr")
        where:
            byteInterval | delayMs
                10       |    15
    }

    def "testing Serializing VarStr"(int byteInterval, int delayMs) {
        given:
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig config = io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext contextS = io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .build()
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext contextD = io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext.builder().protocolBasicConfig(config.getBasicConfig()).build()
            io.bitcoinsv.bsvcl.net.protocol.serialization.VarStrMsgSerializer serializer = io.bitcoinsv.bsvcl.net.protocol.serialization.VarStrMsgSerializer.getinstance()
            io.bitcoinsv.bsvcl.net.protocol.messages.VarStrMsg message = io.bitcoinsv.bsvcl.net.protocol.messages.VarStrMsg.builder().str(VARSTR_MSG_VARSTR).build()
            String serialized
        when:
            ByteArrayWriter byteWriter = new ByteArrayWriter()
            serializer.serialize(contextS, message, byteWriter)
            byte[] serializedBytes = byteWriter.reader().getFullContent()
            // We convert it to String. But we cannot do it directly since the "varInt" part and the "varStr" are
            // encoded differently. So we need to Deserialize it:
            ByteArrayReader byteReader = ByteArrayArtificalStreamProducer.stream(serializedBytes, byteInterval, delayMs)
            io.bitcoinsv.bsvcl.net.protocol.messages.VarStrMsg message2 = serializer.deserialize(contextD, byteReader)
            serialized = message2.getStr()
        then:
            serialized.equals(VARSTR_MSG_VARSTR)
        where:
            byteInterval | delayMs
                10       |    50
    }
}
