package io.bitcoinsv.bsvcl.net.unit.protocol.serialization


import io.bitcoinsv.bsvcl.tools.bytes.ByteArrayReader
import io.bitcoinsv.bsvcl.tools.bytes.ByteArrayWriter
import io.bitcoinsv.bitcoinjsv.core.Utils
import io.bitcoinsv.bitcoinjsv.params.MainNetParams
import io.bitcoinsv.bitcoinjsv.params.Net
import spock.lang.Specification

class SendCompactBlockMsgSpec extends Specification {
    public static final HIGH_BANDWIDTH_BYTES = "018dec1aa458091500"

    public static final LOW_BANDWIDTH_BYTES = "008dec1aa458091500"

    def "Testing SendCompactBlockMsg Deserialize"(int byteInterval, int delayMs) {
        given:
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig config = io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext context = io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext.builder()
                .protocolBasicConfig(config.getBasicConfig())
                .build()

            byte[] highBandwidthBytes = Utils.HEX.decode(HIGH_BANDWIDTH_BYTES)
            byte[] lowBandwidth_bytes = Utils.HEX.decode(LOW_BANDWIDTH_BYTES)

            io.bitcoinsv.bsvcl.net.protocol.serialization.SendCompactBlockMsgSerializer serializer = io.bitcoinsv.bsvcl.net.protocol.serialization.SendCompactBlockMsgSerializer.getInstance()

            io.bitcoinsv.bsvcl.net.protocol.messages.SendCompactBlockMsg highBandwidthMessage
            io.bitcoinsv.bsvcl.net.protocol.messages.SendCompactBlockMsg lowBandwidthMessage

        when:
            ByteArrayReader hbByteReader = io.bitcoinsv.bsvcl.net.unit.protocol.tools.ByteArrayArtificalStreamProducer.stream(highBandwidthBytes, byteInterval, delayMs)
            ByteArrayReader lbByteReader = io.bitcoinsv.bsvcl.net.unit.protocol.tools.ByteArrayArtificalStreamProducer.stream(lowBandwidth_bytes, byteInterval, delayMs)

            highBandwidthMessage = serializer.deserialize(context, hbByteReader)
            lowBandwidthMessage = serializer.deserialize(context, lbByteReader)

        then:
            highBandwidthMessage.getMessageType() == io.bitcoinsv.bsvcl.net.protocol.messages.SendCompactBlockMsg.MESSAGE_TYPE
            highBandwidthMessage.highBandwidthRelaying
            highBandwidthMessage.getVersion() == 5921250825923725

            lowBandwidthMessage.getMessageType() == io.bitcoinsv.bsvcl.net.protocol.messages.SendCompactBlockMsg.MESSAGE_TYPE
            !lowBandwidthMessage.highBandwidthRelaying
            lowBandwidthMessage.getVersion() == 5921250825923725

        where:
            byteInterval | delayMs
            10           | 5
    }

    def "Testing SendCompactBlockMsg Serializing"() {
        given:
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig config = io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext context = io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext.builder()
                .protocolBasicConfig(config.getBasicConfig())
                .build()

            io.bitcoinsv.bsvcl.net.protocol.serialization.SendCompactBlockMsgSerializer serializer = io.bitcoinsv.bsvcl.net.protocol.serialization.SendCompactBlockMsgSerializer.getInstance()

            io.bitcoinsv.bsvcl.net.protocol.messages.SendCompactBlockMsg highBandwidthMsg = io.bitcoinsv.bsvcl.net.protocol.messages.SendCompactBlockMsg.builder()
                .highBandwidthRelaying(true)
                .version(5921250825923725)
                .build()

            io.bitcoinsv.bsvcl.net.protocol.messages.SendCompactBlockMsg lowBandwidthMsg = io.bitcoinsv.bsvcl.net.protocol.messages.SendCompactBlockMsg.builder()
                .highBandwidthRelaying(false)
                .version(5921250825923725)
                .build()

        when:
            ByteArrayWriter hbByteWriter = new ByteArrayWriter()
            serializer.serialize(context, highBandwidthMsg, hbByteWriter)
            String hbBytes = Utils.HEX.encode(hbByteWriter.reader().getFullContent())
            hbByteWriter.reader()

            ByteArrayWriter lbByteWriter = new ByteArrayWriter()
            serializer.serialize(context, lowBandwidthMsg, lbByteWriter)
            String lbBytes = Utils.HEX.encode(lbByteWriter.reader().getFullContent())
            lbByteWriter.reader()

        then:
            hbBytes == HIGH_BANDWIDTH_BYTES
            lbBytes == LOW_BANDWIDTH_BYTES
    }
}
