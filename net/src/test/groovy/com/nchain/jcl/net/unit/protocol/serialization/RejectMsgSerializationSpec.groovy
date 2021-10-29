package com.nchain.jcl.net.unit.protocol.serialization

import com.nchain.jcl.net.protocol.config.ProtocolConfig
import com.nchain.jcl.net.protocol.config.ProtocolConfigBuilder
import com.nchain.jcl.net.protocol.config.provided.ProtocolBSVMainConfig
import com.nchain.jcl.net.protocol.messages.RejectMsg
import com.nchain.jcl.net.protocol.messages.VarStrMsg
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsgBuilder
import com.nchain.jcl.net.protocol.serialization.RejectMsgSerializer
import com.nchain.jcl.net.protocol.serialization.common.BitcoinMsgSerializer
import com.nchain.jcl.net.protocol.serialization.common.BitcoinMsgSerializerImpl
import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext
import com.nchain.jcl.net.protocol.serialization.common.SerializerContext
import com.nchain.jcl.net.unit.protocol.tools.ByteArrayArtificalStreamProducer
import com.nchain.jcl.tools.bytes.ByteArrayReader
import com.nchain.jcl.tools.bytes.ByteArrayWriter
import io.bitcoinj.core.Utils
import io.bitcoinj.params.MainNetParams
import io.bitcoinj.params.Net
import spock.lang.Specification

/**
 * Testing class for the RejectMsg Message Serialization.
 * The test is taken the assumption that we have already a correct serialization version of this Message, obtained
 * from another source that we trust (in this case the Java BitcoinJ library). So we serialize/deserialize some
 * messages with out code and compare the results with that reference.
 */
// TODO: Still pending to test those scenarios where a HASH is stored in the "data" field. At this moment we haven't
// reached that functionality yet.
class RejectMsgSerializationSpec extends Specification {

    // This is the REJECT MSG used as a reference, to compare to our own Serialization.
    // This message has been generated by bitcoinJ using the following values:
    // - Main network
    // - message: "Testing"
    // - ccode: INVALID
    // - reason: "Dummy Reason"
    // - data: empty (null)

    private static final String REF_REJECT_MSG = "e3e1f3e872656a656374000000000000160000008298ed110754657374696e67100c44756d6d7920526561736f6e"
    private static final String REF_REJECT_BODY_MSG = "0754657374696e67100c44756d6d7920526561736f6e"
    private static final String REF_MESSAGE = "Testing"
    private static final RejectMsg.RejectCode REF_CCODE = RejectMsg.RejectCode.INVALID
    private static final String REF_REASON = "Dummy Reason"

    def "testing Reject Message BODY Serializing"() {
        given:
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            SerializerContext context = SerializerContext.builder()
                .protocolBasicConfig(config.getBasicConfig())
                .build()
            RejectMsg rejectMessage = RejectMsg.builder()
                .message(VarStrMsg.builder().str(REF_MESSAGE).build())
                .ccode(REF_CCODE)
                .reason(VarStrMsg.builder().str(REF_REASON).build())
                .build()
            ByteArrayWriter byteWriter = new ByteArrayWriter()
            String messageSerialized = null
        when:
            RejectMsgSerializer.getInstance().serialize(context, rejectMessage, byteWriter)
            byte[] messageBytes = byteWriter.reader().getFullContent()
            byteWriter.reader()
            messageSerialized = Utils.HEX.encode(messageBytes)
        then:
            messageSerialized.equals(REF_REJECT_BODY_MSG)
    }

    def "testing Reject Message BODY De-Serializing"(int byteInterval, int delayMs) {
        given:
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            DeserializerContext context = DeserializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .maxBytesToRead((long) (REF_REJECT_BODY_MSG.length() / 2))
                    .build()
            RejectMsg rejectMsg = null
            ByteArrayReader byteReader = ByteArrayArtificalStreamProducer.stream(Utils.HEX.decode(REF_REJECT_BODY_MSG), byteInterval, delayMs)
        when:
            rejectMsg = RejectMsgSerializer.getInstance().deserialize(context, byteReader)
        then:
            rejectMsg.getMessage().getStr().equals(REF_MESSAGE)
            rejectMsg.getCcode().equals(REF_CCODE)
            rejectMsg.getReason().getStr().equals(REF_REASON)
            (rejectMsg.getData() == null || rejectMsg.getData().length == 0)
        where:
            byteInterval | delayMs
                10       |    25
    }

    def "testing Reject Message COMPLETE Serializing"() {
        given:
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            SerializerContext context = SerializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .build()
            RejectMsg rejectMsg = RejectMsg.builder()
                    .message(VarStrMsg.builder().str(REF_MESSAGE).build())
                    .ccode(REF_CCODE)
                    .reason(VarStrMsg.builder().str(REF_REASON).build())
                    .build()
            BitcoinMsg<RejectMsg> rejectBitcoinMsg = new BitcoinMsgBuilder<>(config.getBasicConfig(), rejectMsg).build()
            BitcoinMsgSerializer serializer = BitcoinMsgSerializerImpl.getInstance()
        when:
            byte[] bytes = serializer.serialize(context, rejectBitcoinMsg, RejectMsg.MESSAGE_TYPE).getFullContent()
            String rejectMsgSerialized = Utils.HEX.encode(bytes)
        then:
            rejectMsgSerialized.equals(REF_REJECT_MSG)
    }

    def "testing Reject Message COMPLETE De-serializing"(int byteInterval, int delayMs) {
        given:
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            DeserializerContext context = DeserializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .maxBytesToRead((long) (REF_REJECT_BODY_MSG.length() / 2))
                    .build()
            ByteArrayReader byteReader = ByteArrayArtificalStreamProducer.stream(Utils.HEX.decode(REF_REJECT_MSG), byteInterval, delayMs)
            BitcoinMsgSerializer bitcoinSerializer = BitcoinMsgSerializerImpl.getInstance()
        when:
            BitcoinMsg<RejectMsg> rejectBitcoinMsg = bitcoinSerializer.deserialize(context, byteReader, RejectMsg.MESSAGE_TYPE)
        then:
            rejectBitcoinMsg.getHeader().getMagic().equals(config.getBasicConfig().getMagicPackage())
            rejectBitcoinMsg.getHeader().getCommand().equals(RejectMsg.MESSAGE_TYPE)
            rejectBitcoinMsg.getBody().getMessage().getStr().equals(REF_MESSAGE)
            rejectBitcoinMsg.getBody().getCcode().equals(REF_CCODE)
            rejectBitcoinMsg.getBody().getReason().getStr().equals(REF_REASON)
            ((rejectBitcoinMsg.getBody().getData() == null) || (rejectBitcoinMsg.getBody().getData().length == 0))
        where:
            byteInterval | delayMs
                10       |    25
    }
}