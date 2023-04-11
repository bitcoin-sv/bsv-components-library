package io.bitcoinsv.bsvcl.net.unit.protocol.serialization


import io.bitcoinsv.bsvcl.common.bytes.ByteArrayReader
import io.bitcoinsv.bsvcl.common.bytes.ByteArrayWriter
import io.bitcoinsv.bitcoinjsv.core.Utils
import io.bitcoinsv.bitcoinjsv.params.MainNetParams
import io.bitcoinsv.bitcoinjsv.params.Net
import spock.lang.Specification

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 10/09/2019
 *
 * Testing class for the GetblocksMsgSerilaizer Serialization.
 * The test is taken the assumption that we have already a correct serialization version of this Message, obtained
 * from another source that we trust (in this case the Java BitcoinJ library). So we serialize/deserialize some
 * messages with out code and compare the results with that reference.
 *
 * NOTE: The Reference HEX Strings used have been generated using 70013 as the protocol Version
 */
class GetblocksMsgSerializerSpec extends Specification {
    private static final String REF_GETBLOCKS_MSG_BODY = "7d11010001a69d45e7abc3b8fc363d13b88aaa2f2ec62bf77b6881e8bd" +
            "7bd1012fd81d802ba99d45e7abc3b8fc363d13b88aaa2f2ec62bf77b6881e8bd7bd1012fd81d802b"
    private static final String REF_GETBLOCKS_MSG_FULL = "e3e1f3e8676574626c6f636b7300000045000000b666ac137d11010001" +
            "a69d45e7abc3b8fc363d13b88aaa2f2ec62bf77b6881e8bd7bd1012fd81d802ba99d45e7abc3b8fc363d13b88aaa2f2ec62bf77b6881e8bd7bd1012fd81d802b"


    def "testing getBlocksMessage BODY Serializing"() {
        given:
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig config = io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolBasicConfig basicConfig = config.getBasicConfig().toBuilder()
                .protocolVersion(io.bitcoinsv.bsvcl.net.protocol.config.ProtocolVersion.ENABLE_FEE_FILTER.getVersion())
                .build()
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext context  = io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext.builder()
                        .protocolBasicConfig(basicConfig)
                        .build()

            io.bitcoinsv.bsvcl.net.protocol.messages.BaseGetDataAndHeaderMsg baseMsg = BaseGetDataAndHeaderMsgSerializerSpec.buildBaseMsg(basicConfig)
            io.bitcoinsv.bsvcl.net.protocol.messages.GetBlocksMsg getBlocksMsg = io.bitcoinsv.bsvcl.net.protocol.messages.GetBlocksMsg.builder().baseGetDataAndHeaderMsg(baseMsg).build()

            ByteArrayWriter byteWriter = new ByteArrayWriter()
            String messageSerialized
        when:
            io.bitcoinsv.bsvcl.net.protocol.serialization.GetblocksMsgSerializer.getInstance().serialize(context, getBlocksMsg, byteWriter)
            byte[] messageBytes = byteWriter.reader().getFullContent()
            messageSerialized = Utils.HEX.encode(messageBytes)
        then:
             messageSerialized.equals(REF_GETBLOCKS_MSG_BODY)
    }

    def "testing getBlocksMessage Message BODY De-Serializing"(int byteInterval, int delayMs) {
        given:
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig config = io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolBasicConfig basicConfig = config.getBasicConfig().toBuilder()
                .protocolVersion(io.bitcoinsv.bsvcl.net.protocol.config.ProtocolVersion.ENABLE_FEE_FILTER.getVersion())
                .build()
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext context = io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext.builder()
                    .protocolBasicConfig(basicConfig)
                    .maxBytesToRead((long) (REF_GETBLOCKS_MSG_BODY.length()/2))
                    .build()
            io.bitcoinsv.bsvcl.net.protocol.messages.GetBlocksMsg getBlocksMsg
            ByteArrayReader byteReader = io.bitcoinsv.bsvcl.net.unit.protocol.tools.ByteArrayArtificalStreamProducer.stream(Utils.HEX.decode(REF_GETBLOCKS_MSG_BODY), byteInterval, delayMs);
        when:
            getBlocksMsg = io.bitcoinsv.bsvcl.net.protocol.serialization.GetblocksMsgSerializer.getInstance().deserialize(context, byteReader)
        then:
            getBlocksMsg.baseGetDataAndHeaderMsg.version.longValue() ==  Long.valueOf(70013).longValue()
            getBlocksMsg.baseGetDataAndHeaderMsg.hashCount.value == 1
            getBlocksMsg.baseGetDataAndHeaderMsg.blockLocatorHash.size() == 1
            getBlocksMsg.messageType == io.bitcoinsv.bsvcl.net.protocol.messages.GetBlocksMsg.MESSAGE_TYPE
        where:
            byteInterval | delayMs
                10       |    15

    }

    def "testing getBlocksMessage COMPLETE Serializing"() {
        given:
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig config = io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolBasicConfig basicConfig = config.getBasicConfig().toBuilder()
                .protocolVersion(io.bitcoinsv.bsvcl.net.protocol.config.ProtocolVersion.ENABLE_FEE_FILTER.getVersion())
                .build()
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext context = io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext.builder()
                    .protocolBasicConfig(basicConfig)
                    .build()
            io.bitcoinsv.bsvcl.net.protocol.messages.BaseGetDataAndHeaderMsg baseMsg = BaseGetDataAndHeaderMsgSerializerSpec.buildBaseMsg(basicConfig)
            io.bitcoinsv.bsvcl.net.protocol.messages.GetBlocksMsg getblockMsg  = io.bitcoinsv.bsvcl.net.protocol.messages.GetBlocksMsg.builder().baseGetDataAndHeaderMsg(baseMsg).build()

         io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsg<io.bitcoinsv.bsvcl.net.protocol.messages.GetBlocksMsg> getBlocksMsgBitcoinMsg = new io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsgBuilder<>(config.getBasicConfig(), getblockMsg).build()
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.BitcoinMsgSerializer serializer = io.bitcoinsv.bsvcl.net.protocol.serialization.common.BitcoinMsgSerializerImpl.getInstance()
        when:
            byte[] bytes = serializer.serialize(context, getBlocksMsgBitcoinMsg).getFullContent()
            String msgSerialized = Utils.HEX.encode(bytes)
        then:
            msgSerialized.equals(REF_GETBLOCKS_MSG_FULL)
    }

    def "testing getBlocksMessage COMPLETE De-serializing"(int byteInterval, int delayMs) {
        given:
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig config = io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolBasicConfig basicConfig = config.getBasicConfig().toBuilder()
                .protocolVersion(io.bitcoinsv.bsvcl.net.protocol.config.ProtocolVersion.ENABLE_FEE_FILTER.getVersion())
                .build()
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext context = io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext.builder()
                    .protocolBasicConfig(basicConfig)
                    .maxBytesToRead((long) (REF_GETBLOCKS_MSG_FULL.length() / 2))
                    .build()
            ByteArrayReader byteReader = io.bitcoinsv.bsvcl.net.unit.protocol.tools.ByteArrayArtificalStreamProducer.stream(Utils.HEX.decode(REF_GETBLOCKS_MSG_FULL), byteInterval, delayMs);
        io.bitcoinsv.bsvcl.net.protocol.serialization.common.BitcoinMsgSerializer bitcoinSerializer = io.bitcoinsv.bsvcl.net.protocol.serialization.common.BitcoinMsgSerializerImpl.getInstance()
        when:
            io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsg<io.bitcoinsv.bsvcl.net.protocol.messages.GetBlocksMsg> getBlocksMsgBitcoinMsg = bitcoinSerializer.deserialize(context, byteReader)
        then:
            getBlocksMsgBitcoinMsg.getHeader().getMagic().equals(config.getBasicConfig().getMagicPackage())
            getBlocksMsgBitcoinMsg.getHeader().getCommand().equals(io.bitcoinsv.bsvcl.net.protocol.messages.GetBlocksMsg.MESSAGE_TYPE)
        where:
            byteInterval | delayMs
                10       |    15
    }

}
