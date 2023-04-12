package io.bitcoinsv.bsvcl.net.protocol.serialization


import io.bitcoinsv.bsvcl.common.bytes.ByteArrayReader
import io.bitcoinsv.bsvcl.common.bytes.ByteArrayWriter
import io.bitcoinsv.bitcoinjsv.core.Utils
import io.bitcoinsv.bitcoinjsv.params.MainNetParams
import io.bitcoinsv.bitcoinjsv.params.Net
import io.bitcoinsv.bsvcl.net.protocol.tools.ByteArrayArtificalStreamProducer
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
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig config = io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolBasicConfig basicConfig = config.getBasicConfig().toBuilder()
                .protocolVersion(io.bitcoinsv.bsvcl.net.protocol.config.ProtocolVersion.ENABLE_FEE_FILTER.getVersion())
                .build()
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext context  = io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext.builder()
                    .protocolBasicConfig(basicConfig)
                    .build()

            io.bitcoinsv.bsvcl.net.protocol.messages.BaseGetDataAndHeaderMsg baseMsg = BaseGetDataAndHeaderMsgSerializerSpec.buildBaseMsg(basicConfig)

            io.bitcoinsv.bsvcl.net.protocol.messages.GetHeadersMsg getHeadersMsg  =  io.bitcoinsv.bsvcl.net.protocol.messages.GetHeadersMsg.builder()
                    .baseGetDataAndHeaderMsg(baseMsg)
                    .build()

            ByteArrayWriter byteWriter = new ByteArrayWriter()
        when:
            io.bitcoinsv.bsvcl.net.protocol.serialization.GetHeadersMsgSerializer.getInstance().serialize(context, getHeadersMsg, byteWriter)
            byte[] messageBytes = byteWriter.reader().getFullContent()
            String messageSerialized = Utils.HEX.encode(messageBytes)
        then:
            messageSerialized == REF_GETHEADERS_MSG_BODY

    }

    def "testing getGetHeadersMessage BODY De-Serializing"(int byteInterval, int delayMs) {
        given:
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig config = io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolBasicConfig basicConfig = config.getBasicConfig().toBuilder()
                .protocolVersion(io.bitcoinsv.bsvcl.net.protocol.config.ProtocolVersion.ENABLE_FEE_FILTER.getVersion())
                .build()
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext context = io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext.builder()
                    .protocolBasicConfig(basicConfig)
                    .maxBytesToRead((long) (REF_GETHEADERS_MSG_BODY.length()/2))
                    .build()
            io.bitcoinsv.bsvcl.net.protocol.messages.GetHeadersMsg getHeadersMsg
            ByteArrayReader byteReader = ByteArrayArtificalStreamProducer.stream(Utils.HEX.decode(REF_GETHEADERS_MSG_BODY), byteInterval, delayMs);
        when:
            getHeadersMsg = io.bitcoinsv.bsvcl.net.protocol.serialization.GetHeadersMsgSerializer.getInstance().deserialize(context, byteReader)
        then:
            getHeadersMsg.baseGetDataAndHeaderMsg.version.longValue() ==  Long.valueOf(70013).longValue()
            getHeadersMsg.baseGetDataAndHeaderMsg.hashCount.value == 1
            getHeadersMsg.baseGetDataAndHeaderMsg.blockLocatorHash.size() == 1
            getHeadersMsg.messageType == io.bitcoinsv.bsvcl.net.protocol.messages.GetHeadersMsg.MESSAGE_TYPE
        where:
            byteInterval | delayMs
                10       |    15
    }

    def "testing getGetHeadersMessage COMPLETE Serializing"() {
        given:
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig config = io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolBasicConfig basicConfig = config.getBasicConfig().toBuilder()
                .protocolVersion(io.bitcoinsv.bsvcl.net.protocol.config.ProtocolVersion.ENABLE_FEE_FILTER.getVersion())
                .build()
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext context = io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext.builder()
                    .protocolBasicConfig(basicConfig)
                    .build()
            io.bitcoinsv.bsvcl.net.protocol.messages.BaseGetDataAndHeaderMsg baseMsg = BaseGetDataAndHeaderMsgSerializerSpec.buildBaseMsg(basicConfig)
            io.bitcoinsv.bsvcl.net.protocol.messages.GetHeadersMsg getHeadersMsg =  io.bitcoinsv.bsvcl.net.protocol.messages.GetHeadersMsg.builder().baseGetDataAndHeaderMsg(baseMsg).build();
            ByteArrayReader byteReader = new ByteArrayReader(Utils.HEX.decode(REF_GETHEADERS_MSG_BODY))


            io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsg<io.bitcoinsv.bsvcl.net.protocol.messages.GetHeadersMsg> getHeadersMsgBitcoinMsg = new io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsgBuilder<>(config.getBasicConfig(), getHeadersMsg).build()
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.BitcoinMsgSerializer serializer = io.bitcoinsv.bsvcl.net.protocol.serialization.common.BitcoinMsgSerializerImpl.getInstance()
        when:
            byte[] bytes = serializer.serialize(context, getHeadersMsgBitcoinMsg).getFullContent()
            String msgSerialized = Utils.HEX.encode(bytes)
        then:
            msgSerialized.equals(REF_GETHEADERS_MSG_FULL)
    }

    def "testing getGetHeadersMessage COMPLETE De-serializing"(int byteInterval, int delayMs) {
        given:
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig config = io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolBasicConfig basicConfig = config.getBasicConfig().toBuilder()
                .protocolVersion(io.bitcoinsv.bsvcl.net.protocol.config.ProtocolVersion.ENABLE_FEE_FILTER.getVersion())
                .build()
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext context = io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext.builder()
                    .protocolBasicConfig(basicConfig)
                    .maxBytesToRead((long) (REF_GETHEADERS_MSG_FULL.length() / 2))
                    .build()
            ByteArrayReader byteReader = ByteArrayArtificalStreamProducer.stream(Utils.HEX.decode(REF_GETHEADERS_MSG_FULL), byteInterval, delayMs);

            io.bitcoinsv.bsvcl.net.protocol.serialization.common.BitcoinMsgSerializer bitcoinSerializer = io.bitcoinsv.bsvcl.net.protocol.serialization.common.BitcoinMsgSerializerImpl.getInstance()
        when:
            io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsg<io.bitcoinsv.bsvcl.net.protocol.messages.GetHeadersMsg> getHeaderMsg = bitcoinSerializer.deserialize(context, byteReader)
        then:
            getHeaderMsg.getHeader().getMagic().equals(config.getBasicConfig().getMagicPackage())
            getHeaderMsg.getHeader().getCommand().equals(io.bitcoinsv.bsvcl.net.protocol.messages.GetHeadersMsg.MESSAGE_TYPE)
        where:
            byteInterval | delayMs
                10       |    15
    }
}
