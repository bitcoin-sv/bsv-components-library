package com.nchain.jcl.net.unit.protocol.serialization

import com.nchain.jcl.net.protocol.config.ProtocolBasicConfig
import com.nchain.jcl.net.protocol.config.ProtocolConfig
import com.nchain.jcl.net.protocol.config.ProtocolConfigBuilder
import com.nchain.jcl.net.protocol.config.ProtocolVersion
import com.nchain.jcl.net.protocol.config.provided.ProtocolBSVMainConfig
import com.nchain.jcl.net.protocol.messages.BaseGetDataAndHeaderMsg
import com.nchain.jcl.net.protocol.messages.HashMsg
import com.nchain.jcl.net.protocol.messages.VarIntMsg
import com.nchain.jcl.net.protocol.serialization.BaseGetDataAndHeaderMsgSerializer
import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext
import com.nchain.jcl.net.protocol.serialization.common.SerializerContext
import com.nchain.jcl.net.unit.protocol.tools.ByteArrayArtificalStreamProducer
import com.nchain.jcl.tools.bytes.ByteArrayReader
import com.nchain.jcl.tools.bytes.ByteArrayWriter
import io.bitcoinsv.bitcoinjsv.core.Sha256Hash
import io.bitcoinsv.bitcoinjsv.core.Utils
import io.bitcoinsv.bitcoinjsv.params.MainNetParams
import io.bitcoinsv.bitcoinjsv.params.Net
import spock.lang.Specification

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 *
 * NOTE: The Reference HEX Strings used have been generated using 70013 a the protocol Version
 *
 * Testing class for the BaseGetDataAndHeaderMsgSerializer Serialization.
 */
class BaseGetDataAndHeaderMsgSerializerSpec extends Specification {
    private static final String REF_MSG_BODY = "7d11010001a69d45e7abc3b8fc363d13b88aaa2f2ec62bf77b6881e8bd" +
            "7bd1012fd81d802ba99d45e7abc3b8fc363d13b88aaa2f2ec62bf77b6881e8bd7bd1012fd81d802b"

    def "testing BaseGetDataAndHeaderMsg Deserializing"() {
        given:
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            ProtocolBasicConfig basicConfig = config.getBasicConfig().toBuilder()
                .protocolVersion(ProtocolVersion.ENABLE_FEE_FILTER.getVersion())
                .build()
            SerializerContext context = SerializerContext.builder()
                    .protocolBasicConfig(basicConfig)
                    .build()

            BaseGetDataAndHeaderMsg baseMsg = buildBaseMsg(basicConfig)
            ByteArrayWriter byteWriter = new ByteArrayWriter()
            byte[] messageBytes
            String messageSerialized
        when:
            BaseGetDataAndHeaderMsgSerializer.getInstance().serialize(context, baseMsg, byteWriter)
            messageBytes = byteWriter.reader().getFullContent()
            messageSerialized = Utils.HEX.encode(messageBytes)
        then:
            messageSerialized.equals(REF_MSG_BODY)
    }

    def "testing BaseGetDataAndHeaderMsg   Serializing"(int byteInterval, int delayMs) {
        given:

            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            ProtocolBasicConfig basicConfig = config.getBasicConfig().toBuilder()
                .protocolVersion(ProtocolVersion.ENABLE_FEE_FILTER.getVersion())
                .build()
            DeserializerContext context = DeserializerContext.builder()
                    .protocolBasicConfig(basicConfig)
                    .maxBytesToRead((long)(REF_MSG_BODY.length()/2))
                    .build()

            ByteArrayReader byteReader = ByteArrayArtificalStreamProducer.stream(Utils.HEX.decode(REF_MSG_BODY), byteInterval, delayMs);
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
        // locator Hash reversed (human-read format)
        byte[] locatorHash = Sha256Hash.wrap("2b801dd82f01d17bbde881687bf72bc62e2faa8ab8133d36fcb8c3abe7459da6").getBytes()
        HashMsg hashMsg = HashMsg.builder().hash(Utils.reverseBytes(locatorHash)).build()
        locators.add(hashMsg);
        // stop Hash reversed (human-read format)
        byte[] stopHash = Sha256Hash.wrap("2b801dd82f01d17bbde881687bf72bc62e2faa8ab8133d36fcb8c3abe7459da9").getBytes()
        HashMsg stopHashMsg = HashMsg.builder().hash(Utils.reverseBytes(stopHash)).build()
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
