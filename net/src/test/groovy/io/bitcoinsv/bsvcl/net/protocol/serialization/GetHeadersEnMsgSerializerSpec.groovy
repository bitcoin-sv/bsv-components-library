package io.bitcoinsv.bsvcl.net.protocol.serialization


import io.bitcoinsv.bsvcl.common.bytes.ByteArrayReader
import io.bitcoinsv.bsvcl.common.bytes.ByteArrayWriter
import io.bitcoinsv.bitcoinjsv.core.Sha256Hash
import io.bitcoinsv.bitcoinjsv.core.Utils
import io.bitcoinsv.bitcoinjsv.params.MainNetParams
import io.bitcoinsv.bitcoinjsv.params.Net
import io.bitcoinsv.bsvcl.net.protocol.tools.ByteArrayArtificalStreamProducer
import spock.lang.Specification
/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 *
 * NOTE: The Reference HEX Strings used have been generated using 70013 a the protocol Version
 */
class GetHeadersEnMsgSerializerSpec extends Specification {
    private static final String REF_GETHEADERSEN_MSG_BODY = "7d110100a69d45e7abc3b8fc363d13b88aaa2f2ec62bf77b6881e8b" +
            "d7bd1012fd81d802ba99d45e7abc3b8fc363d13b88aaa2f2ec62bf77b6881e8bd7bd1012fd81d802b"

    private static final String REF_GETHEADERSEN_MSG_FULL = "e3e1f3e867657468656164657273656e44000000079375ac7d110100a69d45e7abc3b8fc363d13b88aaa2f2ec62bf77b6881e8bd7bd1012fd81d802ba99d45e7abc3b8fc363d13b88aaa2f2ec62bf77b6881e8bd7bd1012fd81d802b";

    def "testing getGetHeadersenMessage BODY Serializing"() {
        given:
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig config = io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolBasicConfig basicConfig = config.getBasicConfig().toBuilder()
                .protocolVersion(io.bitcoinsv.bsvcl.net.protocol.config.ProtocolVersion.ENABLE_FEE_FILTER.getVersion())
                .build()
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext context  = io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext.builder()
                .protocolBasicConfig(basicConfig)
                .build()

            // locator Hash reversed (human-read format)
            byte[] locatorHash = Sha256Hash.wrapReversed(Utils.HEX.decode("2b801dd82f01d17bbde881687bf72bc62e2faa8ab8133d36fcb8c3abe7459da6")).getBytes()
        io.bitcoinsv.bsvcl.net.protocol.messages.HashMsg hashMsg = io.bitcoinsv.bsvcl.net.protocol.messages.HashMsg.builder().hash(locatorHash).build()

            // stop Hash reversed (human-read format)
            byte[] stopHash = Sha256Hash.wrapReversed(Utils.HEX.decode("2b801dd82f01d17bbde881687bf72bc62e2faa8ab8133d36fcb8c3abe7459da9")).getBytes()
            io.bitcoinsv.bsvcl.net.protocol.messages.HashMsg stopHashMsg = io.bitcoinsv.bsvcl.net.protocol.messages.HashMsg.builder().hash(stopHash).build()

        io.bitcoinsv.bsvcl.net.protocol.messages.GetHeadersEnMsg getHeadersEnMsg = io.bitcoinsv.bsvcl.net.protocol.messages.GetHeadersEnMsg.builder()
                    .version(basicConfig.protocolVersion)
                    .blockLocatorHash(hashMsg)
                    .hashStop(stopHashMsg).build();

            ByteArrayWriter byteWriter = new ByteArrayWriter()
        when:
            io.bitcoinsv.bsvcl.net.protocol.serialization.GetHeadersEnMsgSerializer.getInstance().serialize(context, getHeadersEnMsg, byteWriter)
            byte[] messageBytes = byteWriter.reader().getFullContent()
            String messageSerialized = Utils.HEX.encode(messageBytes)
        then:
            messageSerialized == REF_GETHEADERSEN_MSG_BODY
    }


    def "testing getGetHeadersEnMessage BODY De-Serializing"(int byteInterval, int delayMs) {
        given:
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig config = io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolBasicConfig basicConfig = config.getBasicConfig().toBuilder()
                .protocolVersion(io.bitcoinsv.bsvcl.net.protocol.config.ProtocolVersion.ENABLE_FEE_FILTER.getVersion())
                .build()
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext context = io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext.builder()
                    .protocolBasicConfig(basicConfig)
                    .maxBytesToRead((long) (REF_GETHEADERSEN_MSG_BODY.length()/2))
                    .build()
            io.bitcoinsv.bsvcl.net.protocol.messages.GetHeadersEnMsg getHeadersEnMsg
            ByteArrayReader byteReader = ByteArrayArtificalStreamProducer.stream(Utils.HEX.decode(REF_GETHEADERSEN_MSG_BODY), byteInterval, delayMs);
            // locator Hash reversed (human-read format)
            byte[] locatorHash = Sha256Hash.wrapReversed(Utils.HEX.decode("2b801dd82f01d17bbde881687bf72bc62e2faa8ab8133d36fcb8c3abe7459da6")).getBytes()
            io.bitcoinsv.bsvcl.net.protocol.messages.HashMsg hashMsg = io.bitcoinsv.bsvcl.net.protocol.messages.HashMsg.builder().hash(locatorHash).build()
        when:
            getHeadersEnMsg = io.bitcoinsv.bsvcl.net.protocol.serialization.GetHeadersEnMsgSerializer.getInstance().deserialize(context, byteReader)
        then:
            getHeadersEnMsg.version.longValue() ==  Long.valueOf(70013).longValue()
            getHeadersEnMsg.blockLocatorHash.equals(hashMsg)
            getHeadersEnMsg.messageType == io.bitcoinsv.bsvcl.net.protocol.messages.GetHeadersEnMsg.MESSAGE_TYPE
        where:
            byteInterval | delayMs
            10       |    15
    }

    def "testing getGetHeadersenMessage COMPLETE Serializing"() {
        given:
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig config = io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolBasicConfig basicConfig = config.getBasicConfig().toBuilder()
                .protocolVersion(io.bitcoinsv.bsvcl.net.protocol.config.ProtocolVersion.ENABLE_FEE_FILTER.getVersion())
                .build()
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext context = io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext.builder()
                    .protocolBasicConfig(basicConfig)
                    .build()
            io.bitcoinsv.bsvcl.net.protocol.messages.GetHeadersEnMsg getHeadersEnMsg = buildGetHeadersEnMsg(config)


            io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsg<io.bitcoinsv.bsvcl.net.protocol.messages.GetHeadersEnMsg> getHeadersMsgBitcoinMsg = new io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsgBuilder<>(config.getBasicConfig(), getHeadersEnMsg).build()
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.BitcoinMsgSerializer serializer = io.bitcoinsv.bsvcl.net.protocol.serialization.common.BitcoinMsgSerializerImpl.getInstance()
        when:
            byte[] bytes = serializer.serialize(context, getHeadersMsgBitcoinMsg).getFullContent()
            String msgSerialized = Utils.HEX.encode(bytes)
        then:
            msgSerialized.equals(REF_GETHEADERSEN_MSG_FULL)
    }

    def "testing getGetHeadersEnMessage COMPLETE De-serializing"(int byteInterval, int delayMs) {
        given:
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig config = io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolBasicConfig basicConfig = config.getBasicConfig().toBuilder()
                .protocolVersion(io.bitcoinsv.bsvcl.net.protocol.config.ProtocolVersion.ENABLE_FEE_FILTER.getVersion())
                .build()
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext context = io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext.builder()
                    .protocolBasicConfig(basicConfig)
                    .maxBytesToRead((long) (REF_GETHEADERSEN_MSG_FULL.length() / 2))
                    .build()
            ByteArrayReader byteReader = ByteArrayArtificalStreamProducer.stream(Utils.HEX.decode(REF_GETHEADERSEN_MSG_FULL), byteInterval, delayMs);

            io.bitcoinsv.bsvcl.net.protocol.serialization.common.BitcoinMsgSerializer bitcoinSerializer = io.bitcoinsv.bsvcl.net.protocol.serialization.common.BitcoinMsgSerializerImpl.getInstance()
        when:
            io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsg<io.bitcoinsv.bsvcl.net.protocol.messages.GetHeadersEnMsg> getHeadersEnMsg = bitcoinSerializer.deserialize(context, byteReader)
        then:
            getHeadersEnMsg.getHeader().getMagic().equals(config.getBasicConfig().getMagicPackage())
            getHeadersEnMsg.getHeader().getCommand().equals(io.bitcoinsv.bsvcl.net.protocol.messages.GetHeadersEnMsg.MESSAGE_TYPE)
        where:
            byteInterval | delayMs
            10       |    15
    }

    private io.bitcoinsv.bsvcl.net.protocol.messages.GetHeadersEnMsg buildGetHeadersEnMsg(io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig config) {
        // We make sure we use the same Protocol Version:
        io.bitcoinsv.bsvcl.net.protocol.config.ProtocolBasicConfig basicConfig = config.getBasicConfig().toBuilder()
                .protocolVersion(io.bitcoinsv.bsvcl.net.protocol.config.ProtocolVersion.ENABLE_FEE_FILTER.getVersion())
                .build()
        // locator Hash reversed (human-read format)
        byte[] locatorHash = Sha256Hash.wrapReversed(Utils.HEX.decode("2b801dd82f01d17bbde881687bf72bc62e2faa8ab8133d36fcb8c3abe7459da6")).getBytes()
        io.bitcoinsv.bsvcl.net.protocol.messages.HashMsg hashMsg = io.bitcoinsv.bsvcl.net.protocol.messages.HashMsg.builder().hash(locatorHash).build()
        // stop Hash reversed (human-read format)
        byte[] stopHash = Sha256Hash.wrapReversed(Utils.HEX.decode("2b801dd82f01d17bbde881687bf72bc62e2faa8ab8133d36fcb8c3abe7459da9")).getBytes()
        io.bitcoinsv.bsvcl.net.protocol.messages.HashMsg stopHashMsg = io.bitcoinsv.bsvcl.net.protocol.messages.HashMsg.builder().hash(stopHash).build()

        io.bitcoinsv.bsvcl.net.protocol.messages.GetHeadersEnMsg getHeadersEnMsg = io.bitcoinsv.bsvcl.net.protocol.messages.GetHeadersEnMsg.builder()
                .version(basicConfig.protocolVersion)
                .blockLocatorHash(hashMsg)
                .hashStop(stopHashMsg).build();
        getHeadersEnMsg
    }
}
