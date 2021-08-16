/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.unit.protocol.serialization


import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader
import io.bitcoinsv.jcl.tools.bytes.ByteArrayWriter
import io.bitcoinj.core.Utils
import io.bitcoinj.params.MainNetParams
import io.bitcoinj.params.Net
import io.bitcoinsv.jcl.net.protocol.config.ProtocolConfig
import io.bitcoinsv.jcl.net.protocol.config.ProtocolConfigBuilder
import io.bitcoinsv.jcl.net.protocol.messages.SendCompactBlockMsg
import io.bitcoinsv.jcl.net.protocol.serialization.SendCompactBlockMsgSerializer
import io.bitcoinsv.jcl.net.protocol.serialization.common.DeserializerContext
import io.bitcoinsv.jcl.net.protocol.serialization.common.SerializerContext
import io.bitcoinsv.jcl.net.unit.protocol.tools.ByteArrayArtificalStreamProducer
import spock.lang.Specification

class SendCompactBlockMsgSpec extends Specification {
    public static final HIGH_BANDWIDTH_BYTES = "018dec1aa458091500"

    public static final LOW_BANDWIDTH_BYTES = "008dec1aa458091500"

    def "Testing SendCompactBlockMsg Deserialize"(int byteInterval, int delayMs) {
        given:
        ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
        DeserializerContext context = DeserializerContext.builder()
                .protocolBasicConfig(config.getBasicConfig())
                .build()

            byte[] highBandwidthBytes = Utils.HEX.decode(HIGH_BANDWIDTH_BYTES)
            byte[] lowBandwidth_bytes = Utils.HEX.decode(LOW_BANDWIDTH_BYTES)

        SendCompactBlockMsgSerializer serializer = SendCompactBlockMsgSerializer.getInstance()

        SendCompactBlockMsg highBandwidthMessage
            SendCompactBlockMsg lowBandwidthMessage

        when:
            ByteArrayReader hbByteReader = ByteArrayArtificalStreamProducer.stream(highBandwidthBytes, byteInterval, delayMs)
            ByteArrayReader lbByteReader = ByteArrayArtificalStreamProducer.stream(lowBandwidth_bytes, byteInterval, delayMs)

            highBandwidthMessage = serializer.deserialize(context, hbByteReader)
            lowBandwidthMessage = serializer.deserialize(context, lbByteReader)

        then:
            highBandwidthMessage.getMessageType() == SendCompactBlockMsg.MESSAGE_TYPE
            highBandwidthMessage.highBandwidthRelaying
            highBandwidthMessage.getVersion() == 5921250825923725

            lowBandwidthMessage.getMessageType() == SendCompactBlockMsg.MESSAGE_TYPE
            !lowBandwidthMessage.highBandwidthRelaying
            lowBandwidthMessage.getVersion() == 5921250825923725

        where:
            byteInterval | delayMs
            10           | 5
    }

    def "Testing SendCompactBlockMsg Serializing"() {
        given:
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
        SerializerContext context = SerializerContext.builder()
                .protocolBasicConfig(config.getBasicConfig())
                .build()

            SendCompactBlockMsgSerializer serializer = SendCompactBlockMsgSerializer.getInstance()

            SendCompactBlockMsg highBandwidthMsg = SendCompactBlockMsg.builder()
                .highBandwidthRelaying(true)
                .version(5921250825923725)
                .build()

            SendCompactBlockMsg lowBandwidthMsg = SendCompactBlockMsg.builder()
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
