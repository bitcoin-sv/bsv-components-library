package com.nchain.jcl.net.unit.protocol.serialization

import com.nchain.jcl.base.tools.bytes.ByteArrayReader
import com.nchain.jcl.base.tools.bytes.ByteArrayWriter
import com.nchain.jcl.base.tools.bytes.HEX
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper
import com.nchain.jcl.net.protocol.config.ProtocolConfig
import com.nchain.jcl.net.protocol.config.provided.ProtocolBSVMainConfig
import com.nchain.jcl.net.protocol.messages.*
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsgBuilder
import com.nchain.jcl.net.protocol.serialization.GetHeadersEnMsgSerializer
import com.nchain.jcl.net.protocol.serialization.common.BitcoinMsgSerializer
import com.nchain.jcl.net.protocol.serialization.common.BitcoinMsgSerializerImpl
import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext
import com.nchain.jcl.net.protocol.serialization.common.SerializerContext
import com.nchain.jcl.net.unit.protocol.tools.ByteArrayArtificalStreamProducer
import spock.lang.Specification
/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 */
class GetHeadersEnMsgSerializerSpec extends Specification {
    private static final String REF_GETHEADERSEN_MSG_BODY = "7d110100a69d45e7abc3b8fc363d13b88aaa2f2ec62bf77b6881e8b" +
            "d7bd1012fd81d802ba99d45e7abc3b8fc363d13b88aaa2f2ec62bf77b6881e8bd7bd1012fd81d802b"

    private static final String REF_GETHEADERSEN_MSG_FULL = "e3e1f3e867657468656164657273656e44000000079375ac7d110100a69d45e7abc3b8fc363d13b88aaa2f2ec62bf77b6881e8bd7bd1012fd81d802ba99d45e7abc3b8fc363d13b88aaa2f2ec62bf77b6881e8bd7bd1012fd81d802b";

    def "testing getGetHeadersenMessage BODY Serializing"() {
        given:
            ProtocolConfig config = new ProtocolBSVMainConfig()
            SerializerContext context  = SerializerContext.builder()
                .protocolBasicConfig(config.getBasicConfig())
                .build()

            byte[] locatorHash = Sha256Wrapper.wrap("2b801dd82f01d17bbde881687bf72bc62e2faa8ab8133d36fcb8c3abe7459da6").getBytes()
            HashMsg hashMsg = HashMsg.builder().hash(locatorHash).build()

            byte[] stopHash = Sha256Wrapper.wrap("2b801dd82f01d17bbde881687bf72bc62e2faa8ab8133d36fcb8c3abe7459da9").getBytes()
            HashMsg stopHashMsg = HashMsg.builder().hash(stopHash).build()

            GetHeadersEnMsg getHeadersEnMsg = GetHeadersEnMsg.builder()
                    .version(config.protocolVersion)
                    .blockLocatorHash(hashMsg)
                    .hashStop(stopHashMsg).build();

        ByteArrayWriter byteWriter = new ByteArrayWriter()
        when:
            GetHeadersEnMsgSerializer.getInstance().serialize(context, getHeadersEnMsg, byteWriter)
            byte[] messageBytes = byteWriter.reader().getFullContent()
            String messageSerialized = HEX.encode(messageBytes)
        then:
            messageSerialized == REF_GETHEADERSEN_MSG_BODY
    }


    def "testing getGetHeadersEnMessage BODY De-Serializing"(int byteInterval, int delayMs) {
        given:
            ProtocolConfig config = new ProtocolBSVMainConfig()
            DeserializerContext context = DeserializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .maxBytesToRead((long) (REF_GETHEADERSEN_MSG_BODY.length()/2))
                    .build()
            GetHeadersEnMsg  getHeadersEnMsg
            ByteArrayReader byteReader = ByteArrayArtificalStreamProducer.stream(HEX.decode(REF_GETHEADERSEN_MSG_BODY), byteInterval, delayMs);
            byte[] locatorHash = Sha256Wrapper.wrap("2b801dd82f01d17bbde881687bf72bc62e2faa8ab8133d36fcb8c3abe7459da6").getBytes()
            HashMsg hashMsg = HashMsg.builder().hash(locatorHash).build()
        when:
            getHeadersEnMsg = GetHeadersEnMsgSerializer.getInstance().deserialize(context, byteReader)
        then:
            getHeadersEnMsg.version.longValue() ==  Long.valueOf(70013).longValue()
            getHeadersEnMsg.blockLocatorHash.equals(hashMsg)
            getHeadersEnMsg.messageType == GetHeadersEnMsg.MESSAGE_TYPE
        where:
            byteInterval | delayMs
            10       |    15
    }

    def "testing getGetHeadersenMessage COMPLETE Serializing"() {
        given:
            ProtocolConfig config = new ProtocolBSVMainConfig()
            SerializerContext context = SerializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .build()
            GetHeadersEnMsg getHeadersEnMsg = buildGetHeadersEnMsg(config)


            BitcoinMsg<GetHeadersEnMsg> getHeadersMsgBitcoinMsg = new BitcoinMsgBuilder<>(config.getBasicConfig(), getHeadersEnMsg).build()
            BitcoinMsgSerializer serializer = BitcoinMsgSerializerImpl.getInstance()
        when:
            byte[] bytes = serializer.serialize(context, getHeadersMsgBitcoinMsg, GetHeadersEnMsg.MESSAGE_TYPE).getFullContent()
            String msgSerialized = HEX.encode(bytes)
        then:
            msgSerialized.equals(REF_GETHEADERSEN_MSG_FULL)
    }

    def "testing getGetHeadersEnMessage COMPLETE De-serializing"(int byteInterval, int delayMs) {
        given:
            ProtocolConfig config = new ProtocolBSVMainConfig()
            DeserializerContext context = DeserializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .maxBytesToRead((long) (REF_GETHEADERSEN_MSG_FULL.length() / 2))
                    .build()
            ByteArrayReader byteReader = ByteArrayArtificalStreamProducer.stream(HEX.decode(REF_GETHEADERSEN_MSG_FULL), byteInterval, delayMs);

            BitcoinMsgSerializer bitcoinSerializer = BitcoinMsgSerializerImpl.getInstance()
        when:
            BitcoinMsg<GetHeadersEnMsg> getHeadersEnMsg = bitcoinSerializer.deserialize(context, byteReader, GetHeadersEnMsg.MESSAGE_TYPE)
        then:
            getHeadersEnMsg.getHeader().getMagic().equals(config.getBasicConfig().getMagicPackage())
            getHeadersEnMsg.getHeader().getCommand().equals(GetHeadersEnMsg.MESSAGE_TYPE)
        where:
            byteInterval | delayMs
            10       |    15
    }

    private GetHeadersEnMsg buildGetHeadersEnMsg(ProtocolBSVMainConfig config) {
        byte[] locatorHash = Sha256Wrapper.wrap("2b801dd82f01d17bbde881687bf72bc62e2faa8ab8133d36fcb8c3abe7459da6").getBytes()
        HashMsg hashMsg = HashMsg.builder().hash(locatorHash).build()

        byte[] stopHash = Sha256Wrapper.wrap("2b801dd82f01d17bbde881687bf72bc62e2faa8ab8133d36fcb8c3abe7459da9").getBytes()
        HashMsg stopHashMsg = HashMsg.builder().hash(stopHash).build()

        GetHeadersEnMsg getHeadersEnMsg = GetHeadersEnMsg.builder()
                .version(config.protocolVersion)
                .blockLocatorHash(hashMsg)
                .hashStop(stopHashMsg).build();
        getHeadersEnMsg
    }
}