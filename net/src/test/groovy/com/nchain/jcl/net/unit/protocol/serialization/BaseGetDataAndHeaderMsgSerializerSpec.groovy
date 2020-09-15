package com.nchain.jcl.net.unit.protocol.serialization

import com.nchain.jcl.net.protocol.config.ProtocolBasicConfig
import com.nchain.jcl.net.protocol.config.ProtocolConfig
import com.nchain.jcl.net.protocol.config.provided.ProtocolBSVMainConfig
import com.nchain.jcl.net.protocol.messages.BaseGetDataAndHeaderMsg
import com.nchain.jcl.net.protocol.messages.HashMsg
import com.nchain.jcl.net.protocol.messages.VarIntMsg
import com.nchain.jcl.net.protocol.serialization.BaseGetDataAndHeaderMsgSerializer
import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext
import com.nchain.jcl.net.protocol.serialization.common.SerializerContext
import com.nchain.jcl.net.unit.protocol.tools.ByteArrayArtificalStreamProducer
import com.nchain.jcl.base.tools.bytes.ByteArrayReader
import com.nchain.jcl.base.tools.bytes.ByteArrayWriter
import com.nchain.jcl.base.tools.bytes.HEX
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper
import spock.lang.Specification

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 *
 * @date 17/09/2019
 *
 * * Testing class for the BaseGetDataAndHeaderMsgSerializer Serialization.
 */
class BaseGetDataAndHeaderMsgSerializerSpec extends Specification {
    private static final String REF_MSG_BODY = "7d11010001a69d45e7abc3b8fc363d13b88aaa2f2ec62bf77b6881e8bd" +
            "7bd1012fd81d802ba99d45e7abc3b8fc363d13b88aaa2f2ec62bf77b6881e8bd7bd1012fd81d802b"
    def "testing BaseGetDataAndHeaderMsg Serializing"() {
        given:
            ProtocolConfig config = new ProtocolBSVMainConfig()
            SerializerContext context = SerializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .build()

            BaseGetDataAndHeaderMsg baseMsg = buildBaseMsg(config.getBasicConfig())
            ByteArrayWriter byteWriter = new ByteArrayWriter()
            byte[] messageBytes
            String messageSerialized
        when:
            BaseGetDataAndHeaderMsgSerializer.getInstance().serialize(context, baseMsg, byteWriter)
            messageBytes = byteWriter.reader().getFullContent()
            messageSerialized = HEX.encode(messageBytes)
        then:
            messageSerialized.equals(REF_MSG_BODY)
    }

    def "testing BaseGetDataAndHeaderMsg   De-Serializing"(int byteInterval, int delayMs) {
        given:

            ProtocolConfig config = new ProtocolBSVMainConfig()
            DeserializerContext context = DeserializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .maxBytesToRead((long)(REF_MSG_BODY.length()/2))
                    .build()

            ByteArrayReader byteReader = ByteArrayArtificalStreamProducer.stream(HEX.decode(REF_MSG_BODY), byteInterval, delayMs);
            BaseGetDataAndHeaderMsg baseMsg
        when:
             baseMsg = BaseGetDataAndHeaderMsgSerializer.getInstance().deserialize(context, byteReader)
        then:
             baseMsg.getVersion().longValue() == Long.valueOf(70013).longValue()
             baseMsg.getHashCount().getValue() == Long.valueOf(1).longValue()
             baseMsg.getBlockLocatorHash().size() == 1
        where:
            byteInterval | delayMs
                10       |    15

    }

    public static BaseGetDataAndHeaderMsg buildBaseMsg(ProtocolBasicConfig config) {
        List<HashMsg> locators = new ArrayList<Byte[]>();
        byte[] locatorHash = Sha256Wrapper.wrap("2b801dd82f01d17bbde881687bf72bc62e2faa8ab8133d36fcb8c3abe7459da6").getBytes()
        HashMsg hashMsg = HashMsg.builder().hash(locatorHash).build()
        locators.add(hashMsg);

        byte[] stopHash = Sha256Wrapper.wrap("2b801dd82f01d17bbde881687bf72bc62e2faa8ab8133d36fcb8c3abe7459da9").getBytes()
        HashMsg stopHashMsg = HashMsg.builder().hash(stopHash).build()
        VarIntMsg hashCount = VarIntMsg.builder().value(locators.size()).build();
        BaseGetDataAndHeaderMsg  baseMsg =  BaseGetDataAndHeaderMsg.builder()
                .version(config.protocolVersion)
                .hashCount(hashCount)
                .blockLocatorHash(locators)
                .hashStop(stopHashMsg)
                .build();

        return baseMsg;
    }


}