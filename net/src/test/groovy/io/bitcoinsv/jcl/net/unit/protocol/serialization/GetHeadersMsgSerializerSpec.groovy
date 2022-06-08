package io.bitcoinsv.jcl.net.unit.protocol.serialization

import io.bitcoinsv.jcl.net.protocol.config.ProtocolBasicConfig
import io.bitcoinsv.jcl.net.protocol.config.ProtocolConfigBuilder
import io.bitcoinsv.jcl.net.protocol.config.ProtocolVersion
import io.bitcoinsv.jcl.net.protocol.serialization.common.BitcoinMsgSerializer
import io.bitcoinsv.jcl.net.protocol.serialization.common.BitcoinMsgSerializerImpl
import io.bitcoinsv.jcl.net.protocol.serialization.common.DeserializerContext
import io.bitcoinsv.jcl.net.protocol.serialization.common.SerializerContext
import io.bitcoinsv.jcl.net.protocol.config.ProtocolConfig
import io.bitcoinsv.jcl.net.protocol.messages.BaseGetDataAndHeaderMsg
import io.bitcoinsv.jcl.net.protocol.messages.GetHeadersMsg
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsg
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsgBuilder
import io.bitcoinsv.jcl.net.protocol.serialization.GetHeadersMsgSerializer
import io.bitcoinsv.jcl.net.unit.protocol.tools.ByteArrayArtificalStreamProducer
import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader
import io.bitcoinsv.jcl.tools.bytes.ByteArrayWriter
import io.bitcoinsv.bitcoinjsv.core.Utils
import io.bitcoinsv.bitcoinjsv.params.MainNetParams
import io.bitcoinsv.bitcoinjsv.params.Net
import spock.lang.Specification

/**
* @author m.jose@nchain.com

* Copyright (c) 2018-2019 Bitcoin Association
* Distributed under the Open BSV software license, see the accompanying file LICENSE.
*
* @date 12/09/2019
*
* Testing class for the GetblocksMsgSerilaizer Serialization.
* The test is taken the assumption that we have already a correct serialization version of this Message, obtained
* from another source that we trust (in this case the Java BitcoinJ library). So we serialize/deserialize some
* messages with out code and compare the results with that reference.
*
* NOTE: The Reference HEX Strings used have been generated using 70013 as the protocol Version
*/
class GetHeadersMsgSerializerSpec extends Specification{
    private static final String REF_GETHEADERS_MSG_BODY = "7d11010001a69d45e7abc3b8fc363d13b88aaa2f2ec62bf77b6881e8bd" +
            "7bd1012fd81d802ba99d45e7abc3b8fc363d13b88aaa2f2ec62bf77b6881e8bd7bd1012fd81d802b"
    private static final String REF_GETHEADERS_MSG_FULL = "e3e1f3e867657468656164657273000045000000b666ac137d110100" +
            "01a69d45e7abc3b8fc363d13b88aaa2f2ec62bf77b6881e8bd7bd1012fd81d802ba99d45e7abc3b8fc363d13b88aaa2f2ec62bf77b6881e8bd7bd1012fd81d802b"


    def "testing getGetHeadersMessage BODY Serializing"() {
        given:
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            ProtocolBasicConfig basicConfig = config.getBasicConfig().toBuilder()
                .protocolVersion(ProtocolVersion.ENABLE_FEE_FILTER.getVersion())
                .build()
            SerializerContext context  = SerializerContext.builder()
                    .protocolBasicConfig(basicConfig)
                    .build()

            BaseGetDataAndHeaderMsg baseMsg = BaseGetDataAndHeaderMsgSerializerSpec.buildBaseMsg(basicConfig)

        GetHeadersMsg getHeadersMsg  =  GetHeadersMsg.builder()
                    .baseGetDataAndHeaderMsg(baseMsg)
                    .build()

            ByteArrayWriter byteWriter = new ByteArrayWriter()
        when:
        GetHeadersMsgSerializer.getInstance().serialize(context, getHeadersMsg, byteWriter)
            byte[] messageBytes = byteWriter.reader().getFullContent()
            String messageSerialized = Utils.HEX.encode(messageBytes)
        then:
            messageSerialized == REF_GETHEADERS_MSG_BODY

    }

    def "testing getGetHeadersMessage BODY De-Serializing"(int byteInterval, int delayMs) {
        given:
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            ProtocolBasicConfig basicConfig = config.getBasicConfig().toBuilder()
                .protocolVersion(ProtocolVersion.ENABLE_FEE_FILTER.getVersion())
                .build()
            DeserializerContext context = DeserializerContext.builder()
                    .protocolBasicConfig(basicConfig)
                    .maxBytesToRead((long) (REF_GETHEADERS_MSG_BODY.length()/2))
                    .build()
            GetHeadersMsg getHeadersMsg
            ByteArrayReader byteReader = ByteArrayArtificalStreamProducer.stream(Utils.HEX.decode(REF_GETHEADERS_MSG_BODY), byteInterval, delayMs);
        when:
            getHeadersMsg = GetHeadersMsgSerializer.getInstance().deserialize(context, byteReader)
        then:
            getHeadersMsg.baseGetDataAndHeaderMsg.version.longValue() ==  Long.valueOf(70013).longValue()
            getHeadersMsg.baseGetDataAndHeaderMsg.hashCount.value == 1
            getHeadersMsg.baseGetDataAndHeaderMsg.blockLocatorHash.size() == 1
            getHeadersMsg.messageType == GetHeadersMsg.MESSAGE_TYPE
        where:
            byteInterval | delayMs
                10       |    15
    }

    def "testing getGetHeadersMessage COMPLETE Serializing"() {
        given:
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            ProtocolBasicConfig basicConfig = config.getBasicConfig().toBuilder()
                .protocolVersion(ProtocolVersion.ENABLE_FEE_FILTER.getVersion())
                .build()
            SerializerContext context = SerializerContext.builder()
                    .protocolBasicConfig(basicConfig)
                    .build()
            BaseGetDataAndHeaderMsg baseMsg = BaseGetDataAndHeaderMsgSerializerSpec.buildBaseMsg(basicConfig)
            GetHeadersMsg  getHeadersMsg =  GetHeadersMsg.builder().baseGetDataAndHeaderMsg(baseMsg).build();
            ByteArrayReader byteReader = new ByteArrayReader(Utils.HEX.decode(REF_GETHEADERS_MSG_BODY))


        BitcoinMsg<GetHeadersMsg> getHeadersMsgBitcoinMsg = new BitcoinMsgBuilder<>(config.getBasicConfig(), getHeadersMsg).build()
        BitcoinMsgSerializer serializer = BitcoinMsgSerializerImpl.getInstance()
        when:
            byte[] bytes = serializer.serialize(context, getHeadersMsgBitcoinMsg).getFullContent()
            String msgSerialized = Utils.HEX.encode(bytes)
        then:
            msgSerialized.equals(REF_GETHEADERS_MSG_FULL)
    }

    def "testing getGetHeadersMessage COMPLETE De-serializing"(int byteInterval, int delayMs) {
        given:
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            ProtocolBasicConfig basicConfig = config.getBasicConfig().toBuilder()
                .protocolVersion(ProtocolVersion.ENABLE_FEE_FILTER.getVersion())
                .build()
            DeserializerContext context = DeserializerContext.builder()
                    .protocolBasicConfig(basicConfig)
                    .maxBytesToRead((long) (REF_GETHEADERS_MSG_FULL.length() / 2))
                    .build()
            ByteArrayReader byteReader = ByteArrayArtificalStreamProducer.stream(Utils.HEX.decode(REF_GETHEADERS_MSG_FULL), byteInterval, delayMs);

            BitcoinMsgSerializer bitcoinSerializer = BitcoinMsgSerializerImpl.getInstance()
        when:
            BitcoinMsg<GetHeadersMsg> getHeaderMsg = bitcoinSerializer.deserialize(context, byteReader)
        then:
            getHeaderMsg.getHeader().getMagic().equals(config.getBasicConfig().getMagicPackage())
            getHeaderMsg.getHeader().getCommand().equals(GetHeadersMsg.MESSAGE_TYPE)
        where:
            byteInterval | delayMs
                10       |    15
    }
}
