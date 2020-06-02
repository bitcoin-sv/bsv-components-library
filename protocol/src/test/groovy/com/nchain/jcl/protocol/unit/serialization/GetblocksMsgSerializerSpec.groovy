package com.nchain.jcl.protocol.unit.serialization

import com.nchain.jcl.protocol.config.ProtocolConfig
import com.nchain.jcl.protocol.config.provided.ProtocolBSVMainConfig
import com.nchain.jcl.protocol.messages.BaseGetDataAndHeaderMsg
import com.nchain.jcl.protocol.messages.GetBlocksMsg
import com.nchain.jcl.protocol.messages.common.BitcoinMsg
import com.nchain.jcl.protocol.messages.common.BitcoinMsgBuilder
import com.nchain.jcl.protocol.serialization.GetblocksMsgSerializer
import com.nchain.jcl.protocol.serialization.common.*
import com.nchain.jcl.tools.bytes.HEX
import com.nchain.jcl.tools.bytes.ByteArrayReader
import com.nchain.jcl.tools.bytes.ByteArrayWriter
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
 */
class GetblocksMsgSerializerSpec extends Specification {
    private static final String REF_GETBLOCKS_MSG_BODY = "7d11010001a69d45e7abc3b8fc363d13b88aaa2f2ec62bf77b6881e8bd" +
            "7bd1012fd81d802ba99d45e7abc3b8fc363d13b88aaa2f2ec62bf77b6881e8bd7bd1012fd81d802b"
    private static final String REF_GETBLOCKS_MSG_FULL = "e3e1f3e8676574626c6f636b7300000045000000b666ac137d11010001" +
            "a69d45e7abc3b8fc363d13b88aaa2f2ec62bf77b6881e8bd7bd1012fd81d802ba99d45e7abc3b8fc363d13b88aaa2f2ec62bf77b6881e8bd7bd1012fd81d802b"


    def "testing getBlocksMessage BODY Serializing"() {
        given:
            ProtocolConfig config = new ProtocolBSVMainConfig()
            SerializerContext context  = SerializerContext.builder()
                    .protocolconfig(config)
                    .build()

            BaseGetDataAndHeaderMsg baseMsg = BaseGetDataAndHeaderMsgSerializerSpec.buildBaseMsg(config)
            GetBlocksMsg getBlocksMsg = GetBlocksMsg.builder().baseGetDataAndHeaderMsg(baseMsg).build()

            ByteArrayWriter byteWriter = new ByteArrayWriter()
            String messageSerialized
        when:
            GetblocksMsgSerializer.getInstance().serialize(context, getBlocksMsg, byteWriter)
            byte[] messageBytes = byteWriter.reader().getFullContent()
            messageSerialized = HEX.encode(messageBytes)
        then:
             messageSerialized.equals(REF_GETBLOCKS_MSG_BODY)
    }

    def "testing getBlocksMessage Message BODY De-Serializing"() {
        given:
            ProtocolConfig config = new ProtocolBSVMainConfig()
            DeserializerContext context = DeserializerContext.builder()
                    .protocolconfig(config)
                    .maxBytesToRead((long) (REF_GETBLOCKS_MSG_BODY.length()/2))
                    .build()
            GetBlocksMsg getBlocksMsg
            ByteArrayReader byteReader = new ByteArrayReader(HEX.decode(REF_GETBLOCKS_MSG_BODY))
        when:
            getBlocksMsg = GetblocksMsgSerializer.getInstance().deserialize(context, byteReader)
        then:
            getBlocksMsg.baseGetDataAndHeaderMsg.version.longValue() ==  Long.valueOf(70013).longValue()
            getBlocksMsg.baseGetDataAndHeaderMsg.hashCount.value == 1
            getBlocksMsg.baseGetDataAndHeaderMsg.blockLocatorHash.size() == 1
            getBlocksMsg.messageType == GetBlocksMsg.MESSAGE_TYPE

    }

    def "testing getBlocksMessage COMPLETE Serializing"() {
        given:
            ProtocolConfig config = new ProtocolBSVMainConfig()
            SerializerContext context = SerializerContext.builder()
                    .protocolconfig(config)
                    .build()
            BaseGetDataAndHeaderMsg baseMsg = BaseGetDataAndHeaderMsgSerializerSpec.buildBaseMsg(config)
            GetBlocksMsg getblockMsg  = GetBlocksMsg.builder().baseGetDataAndHeaderMsg(baseMsg).build()

         BitcoinMsg<GetBlocksMsg> getBlocksMsgBitcoinMsg = new BitcoinMsgBuilder<>(config, getblockMsg).build()
            BitcoinMsgSerializer serializer = BitcoinMsgSerializerImpl.getInstance()
        when:
            byte[] bytes = serializer.serialize(context, getBlocksMsgBitcoinMsg, GetBlocksMsg.MESSAGE_TYPE).getFullContent()
            String msgSerialized = HEX.encode(bytes)
        then:
            msgSerialized.equals(REF_GETBLOCKS_MSG_FULL)
    }

    def "testing getBlocksMessage COMPLETE De-serializing"() {
        given:
            ProtocolConfig config = new ProtocolBSVMainConfig()
            DeserializerContext context = DeserializerContext.builder()
                    .protocolconfig(config)
                    .maxBytesToRead((long) (REF_GETBLOCKS_MSG_FULL.length() / 2))
                    .build()
            ByteArrayReader byteReader = new ByteArrayReader(HEX.decode(REF_GETBLOCKS_MSG_FULL))
            BitcoinMsgSerializer bitcoinSerializer = BitcoinMsgSerializerImpl.getInstance()
        when:
            BitcoinMsg<GetBlocksMsg> getBlocksMsgBitcoinMsg = bitcoinSerializer.deserialize(context, byteReader, GetBlocksMsg.MESSAGE_TYPE)
        then:
            getBlocksMsgBitcoinMsg.getHeader().getMagic().equals(config.getMagicPackage())
            getBlocksMsgBitcoinMsg.getHeader().getCommand().equals(GetBlocksMsg.MESSAGE_TYPE)
    }

}
