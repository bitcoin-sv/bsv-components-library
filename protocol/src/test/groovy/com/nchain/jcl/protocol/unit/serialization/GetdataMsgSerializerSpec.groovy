package com.nchain.jcl.protocol.unit.serialization

import com.nchain.jcl.protocol.config.ProtocolConfig
import com.nchain.jcl.protocol.config.provided.ProtocolBSVMainConfig
import com.nchain.jcl.protocol.messages.GetdataMsg
import com.nchain.jcl.protocol.messages.HashMsg
import com.nchain.jcl.protocol.messages.InventoryVectorMsg
import com.nchain.jcl.protocol.messages.common.BitcoinMsg
import com.nchain.jcl.protocol.messages.common.BitcoinMsgBuilder
import com.nchain.jcl.protocol.serialization.GetdataMsgSerializer
import com.nchain.jcl.protocol.serialization.common.*
import com.nchain.jcl.protocol.unit.tools.ByteArrayArtificalStreamProducer
import com.nchain.jcl.tools.bytes.HEX
import com.nchain.jcl.tools.crypto.Sha256Wrapper
import com.nchain.jcl.tools.bytes.ByteArrayReader
import com.nchain.jcl.tools.bytes.ByteArrayWriter
import spock.lang.Specification
/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 27/08/2019
 *
 * Testing class for the GetdataMsg Serialization.
 * The test is taken the assumption that we have already a correct serialization version of this Message, obtained
 * from another source that we trust (in this case the Java BitcoinJ library). So we serialize/deserialize some
 * messages with out code and compare the results with that reference.
 */
class GetdataMsgSerializerSpec extends Specification {

    private static final String REF_GETDATA_MSG_BODY = "0101000000a69d45e7abc3b8fc363d13b88aaa2f2ec62bf77b6881e8bd7bd1012fd81d802b"

    public static final byte[] REF_INV_MSG_BITES = Sha256Wrapper.wrap("2b801dd82f01d17bbde881687bf72bc62e2faa8ab8133d36fcb8c3abe7459da6").getBytes()
    private static final HashMsg REF_HASH_MSG = HashMsg.builder().hash(REF_INV_MSG_BITES).build()

    private static final String REF_GETDATA_MSG_FULL = "e3e1f3e867657464617461000000000025000000e27152ce0101000000a69d45e7abc3b8fc363d13b88aaa2f2ec62bf77b6881e8bd7bd1012fd81d802b"

    def "testing getDataMessage  BODY De-Serializing"(int byteInterval, int delayMs) {
        given:
            ProtocolConfig config = new ProtocolBSVMainConfig()
            DeserializerContext context = DeserializerContext.builder()
                .protocolconfig(config)
                .maxBytesToRead((long) (REF_GETDATA_MSG_BODY.length()/2))
                .build()
            GetdataMsg inventoryMsg
            ByteArrayReader byteReader = ByteArrayArtificalStreamProducer.stream(HEX.decode(REF_GETDATA_MSG_BODY), byteInterval, delayMs);
        when:
            inventoryMsg = GetdataMsgSerializer.getInstance().deserialize(context, byteReader)
        then:
            inventoryMsg.getMessageType().equals(GetdataMsg.MESSAGE_TYPE)
            inventoryMsg.getCount().value.toString().equals("1")
        where:
            byteInterval | delayMs
                10       |    15
    }

    def "testing getDataMessage BODY Serializing"() {
        given:
            ProtocolConfig config = new ProtocolBSVMainConfig()
            SerializerContext context  = SerializerContext.builder()
                    .protocolconfig(config)
                    .build()
            InventoryVectorMsg inventoryVectorMsg  =  InventoryVectorMsg.builder()
                    .type(InventoryVectorMsg.VectorType.MSG_TX)
                    .hashMsg(REF_HASH_MSG)
                    .build()

            List<InventoryVectorMsg> msgList = new ArrayList<>();
            msgList.add(inventoryVectorMsg)
            GetdataMsg getdataMsg = GetdataMsg.builder().invVectorList(msgList).build();
            ByteArrayWriter byteWriter = new ByteArrayWriter()
            String messageSerialized
        when:
            GetdataMsgSerializer.getInstance().serialize(context, getdataMsg, byteWriter)
            byte[] messageBytes = byteWriter.reader().getFullContent()
            messageSerialized = HEX.encode(messageBytes)
        then:
            messageSerialized.equals(REF_GETDATA_MSG_BODY)
    }

    def "testing getDataMessage COMPLETE De-serializing"(int byteInterval, int delayMs) {
        given:
            ProtocolConfig config = new ProtocolBSVMainConfig()
            DeserializerContext context = DeserializerContext.builder()
                    .protocolconfig(config)
                    .maxBytesToRead((long) (REF_GETDATA_MSG_FULL.length() / 2))
                    .build()
            ByteArrayReader byteReader = ByteArrayArtificalStreamProducer.stream(HEX.decode(REF_GETDATA_MSG_FULL), byteInterval, delayMs);
            BitcoinMsgSerializer bitcoinSerializer = BitcoinMsgSerializerImpl.getInstance()
        when:
            BitcoinMsg<GetdataMsg> getDataMsg = bitcoinSerializer.deserialize(context, byteReader, GetdataMsg.MESSAGE_TYPE)
        then:
            getDataMsg.getHeader().getMagic().equals(config.getMagicPackage())
            getDataMsg.getHeader().getCommand().equals(GetdataMsg.MESSAGE_TYPE)
            List<InventoryVectorMsg> inventoryList = getDataMsg.getBody().getInvVectorList()
            InventoryVectorMsg inventoryVectorMsg = inventoryList.get(0)
            inventoryList.size() == 1
            inventoryVectorMsg.getType() == InventoryVectorMsg.VectorType.MSG_TX
        where:
            byteInterval | delayMs
                10       |    15
    }

    def "testing getData COMPLETE Serializing"() {
        given:
            ProtocolConfig config = new ProtocolBSVMainConfig()
            SerializerContext context = SerializerContext.builder()
                    .protocolconfig(config)
                    .build()
            InventoryVectorMsg inventoryVectorMsg  = InventoryVectorMsg.builder()
                    .type(InventoryVectorMsg.VectorType.MSG_TX)
                    .hashMsg(REF_HASH_MSG)
                    .build()
            List<InventoryVectorMsg> msgList = new ArrayList<>();
            msgList.add(inventoryVectorMsg);
            GetdataMsg getdataMsg = GetdataMsg.builder().invVectorList(msgList).build();

            BitcoinMsg<GetdataMsg> getdataMsgBitcoinMsg = new BitcoinMsgBuilder<>(config, getdataMsg).build()
            BitcoinMsgSerializer serializer = BitcoinMsgSerializerImpl.getInstance()
        when:
            byte[] bytes = serializer.serialize(context, getdataMsgBitcoinMsg, GetdataMsg.MESSAGE_TYPE).getFullContent()
            String serialized = HEX.encode(bytes)
        then:
             serialized.equals(REF_GETDATA_MSG_FULL)
    }
}
