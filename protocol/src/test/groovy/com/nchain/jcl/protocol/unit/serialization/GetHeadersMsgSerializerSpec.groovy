package com.nchain.jcl.protocol.unit.serialization

import com.nchain.jcl.protocol.config.ProtocolConfig
import com.nchain.jcl.protocol.config.provided.ProtocolBSVMainConfig
import com.nchain.jcl.protocol.messages.BaseGetDataAndHeaderMsg
import com.nchain.jcl.protocol.messages.GetHeadersMsg
import com.nchain.jcl.protocol.messages.common.BitcoinMsg
import com.nchain.jcl.protocol.messages.common.BitcoinMsgBuilder
import com.nchain.jcl.protocol.serialization.GetHeadersMsgSerializer
import com.nchain.jcl.protocol.serialization.common.*
import com.nchain.jcl.protocol.unit.tools.ByteArrayArtificalStreamProducer
import com.nchain.jcl.tools.bytes.HEX
import com.nchain.jcl.tools.bytes.ByteArrayReader
import com.nchain.jcl.tools.bytes.ByteArrayWriter
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
*/
class GetHeadersMsgSerializerSpec extends Specification{
    private static final String REF_GETHEADERS_MSG_BODY = "7d11010001a69d45e7abc3b8fc363d13b88aaa2f2ec62bf77b6881e8bd" +
            "7bd1012fd81d802ba99d45e7abc3b8fc363d13b88aaa2f2ec62bf77b6881e8bd7bd1012fd81d802b"
    private static final String REF_GETHEADERS_MSG_FULL = "e3e1f3e867657468656164657273000045000000b666ac137d110100" +
            "01a69d45e7abc3b8fc363d13b88aaa2f2ec62bf77b6881e8bd7bd1012fd81d802ba99d45e7abc3b8fc363d13b88aaa2f2ec62bf77b6881e8bd7bd1012fd81d802b"


    def "testing getGetHeadersMessage BODY Serializing"() {
        given:
            ProtocolConfig config = new ProtocolBSVMainConfig()
            SerializerContext context  = SerializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .build()

            BaseGetDataAndHeaderMsg baseMsg = BaseGetDataAndHeaderMsgSerializerSpec.buildBaseMsg(config.getBasicConfig())

            GetHeadersMsg getHeadersMsg  =  GetHeadersMsg.builder()
                    .baseGetDataAndHeaderMsg(baseMsg)
                    .build()

            ByteArrayWriter byteWriter = new ByteArrayWriter()
        when:
            GetHeadersMsgSerializer.getInstance().serialize(context, getHeadersMsg, byteWriter)
            byte[] messageBytes = byteWriter.reader().getFullContent()
            String messageSerialized = HEX.encode(messageBytes)
        then:
            messageSerialized == REF_GETHEADERS_MSG_BODY

    }

    def "testing getGetHeadersMessage BODY De-Serializing"(int byteInterval, int delayMs) {
        given:
            ProtocolConfig config = new ProtocolBSVMainConfig()
            DeserializerContext context = DeserializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .maxBytesToRead((long) (REF_GETHEADERS_MSG_BODY.length()/2))
                    .build()
            GetHeadersMsg getHeadersMsg
            ByteArrayReader byteReader = ByteArrayArtificalStreamProducer.stream(HEX.decode(REF_GETHEADERS_MSG_BODY), byteInterval, delayMs);
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
            ProtocolConfig config = new ProtocolBSVMainConfig()
            SerializerContext context = SerializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .build()
            BaseGetDataAndHeaderMsg baseMsg = BaseGetDataAndHeaderMsgSerializerSpec.buildBaseMsg(config.getBasicConfig())
            GetHeadersMsg  getHeadersMsg =  GetHeadersMsg.builder().baseGetDataAndHeaderMsg(baseMsg).build();
            ByteArrayReader byteReader = new ByteArrayReader(HEX.decode(REF_GETHEADERS_MSG_BODY))


            BitcoinMsg<GetHeadersMsg> getHeadersMsgBitcoinMsg = new BitcoinMsgBuilder<>(config.getBasicConfig(), getHeadersMsg).build()
            BitcoinMsgSerializer serializer = BitcoinMsgSerializerImpl.getInstance()
        when:
            byte[] bytes = serializer.serialize(context, getHeadersMsgBitcoinMsg, GetHeadersMsg.MESSAGE_TYPE).getFullContent()
            String msgSerialized = HEX.encode(bytes)
        then:
            msgSerialized.equals(REF_GETHEADERS_MSG_FULL)
    }

    def "testing getGetHeadersMessage COMPLETE De-serializing"(int byteInterval, int delayMs) {
        given:
            ProtocolConfig config = new ProtocolBSVMainConfig()
            DeserializerContext context = DeserializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .maxBytesToRead((long) (REF_GETHEADERS_MSG_FULL.length() / 2))
                    .build()
            ByteArrayReader byteReader = ByteArrayArtificalStreamProducer.stream(HEX.decode(REF_GETHEADERS_MSG_FULL), byteInterval, delayMs);

            BitcoinMsgSerializer bitcoinSerializer = BitcoinMsgSerializerImpl.getInstance()
        when:
            BitcoinMsg<GetHeadersMsg> getHeaderMsg = bitcoinSerializer.deserialize(context, byteReader, GetHeadersMsg.MESSAGE_TYPE)
        then:
            getHeaderMsg.getHeader().getMagic().equals(config.getBasicConfig().getMagicPackage())
            getHeaderMsg.getHeader().getCommand().equals(GetHeadersMsg.MESSAGE_TYPE)
        where:
            byteInterval | delayMs
                10       |    15
    }
}
