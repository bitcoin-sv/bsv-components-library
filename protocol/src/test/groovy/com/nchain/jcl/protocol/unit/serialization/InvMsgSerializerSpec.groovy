package com.nchain.jcl.protocol.unit.serialization

import com.nchain.jcl.protocol.config.ProtocolConfig
import com.nchain.jcl.protocol.config.provided.ProtocolBSVMainConfig
import com.nchain.jcl.protocol.messages.HashMsg
import com.nchain.jcl.protocol.messages.InvMessage
import com.nchain.jcl.protocol.messages.InventoryVectorMsg
import com.nchain.jcl.protocol.messages.VarIntMsg
import com.nchain.jcl.protocol.messages.common.BitcoinMsg
import com.nchain.jcl.protocol.messages.common.BitcoinMsgBuilder
import com.nchain.jcl.protocol.serialization.InvMsgSerializer
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
 * @date 20/08/2019
 *
 * Testing class for the InvMessage Serialization.
 * The test is taken the assumption that we have already a correct serialization version of this Message, obtained
 * from another source that we trust (in this case the Java BitcoinJ library). So we serialize/deserialize some
 * messages with out code and compare the results with that reference.
 */
class InvMsgSerializerSpec extends Specification {

    public static final byte[] REF_INV_MSG_BITES =  Sha256Wrapper.wrap("2b801dd82f01d17bbde881687bf72bc62e2faa8ab8133d36fcb8c3abe7459da6").getBytes()
    private static final InventoryVectorMsg.VectorType REF_INV_VEC_TYPE = InventoryVectorMsg.VectorType.MSG_TX

    private static final String REF_INV_MSG_BODY = "0101000000a69d45e7abc3b8fc363d13b88aaa2f2ec62bf77b6881e8bd7bd1012fd81d802b"
    private static final String REF_INV_MSG_FULL = "e3e1f3e8696e7600000000000000000025000000e27152ce0101000000a69d45e7abc3b8fc363d13b88aaa2f2ec62bf77b6881e8bd7bd1012fd81d802b"
    private static final HashMsg REF_HASH_MSG = HashMsg.builder().hash(REF_INV_MSG_BITES).build()

    def "testing invMessage BODY Serializing"() {
        given:
            ProtocolConfig config = new ProtocolBSVMainConfig()
            SerializerContext context  = SerializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .build()
            InventoryVectorMsg inventoryVectorMsg  = InventoryVectorMsg.builder()
                    .type(REF_INV_VEC_TYPE)
                    .hashMsg(REF_HASH_MSG)
                    .build()
            List<InventoryVectorMsg> msgList = new ArrayList<>();
            msgList.add(inventoryVectorMsg);
            VarIntMsg count = VarIntMsg.builder().value(msgList.size()).build();
            InvMessage invMessage = InvMessage.builder().invVectorMsgList(msgList).build();
            ByteArrayWriter byteWriter = new ByteArrayWriter()
            String messageSerialized = null
        when:
            InvMsgSerializer.getInstance().serialize(context, invMessage, byteWriter)
            byte[] messageBytes = byteWriter.reader()getFullContent()
            messageSerialized = HEX.encode(messageBytes)
        then:
            messageSerialized.equals(REF_INV_MSG_BODY)
    }

    def "testing invMessage BODY De-Serializing"(int byteInterval, int delayMs) {
        given:
            ProtocolConfig config = new ProtocolBSVMainConfig()
            DeserializerContext context = DeserializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .maxBytesToRead((long) (REF_INV_MSG_BODY.length()/2))
                    .build()
            InvMessage inventoryMsg = null
            ByteArrayReader byteReader = ByteArrayArtificalStreamProducer.stream(HEX.decode(REF_INV_MSG_BODY), byteInterval, delayMs);

        when:
            inventoryMsg = InvMsgSerializer.getInstance().deserialize(context, byteReader)
        then:
            inventoryMsg.getMessageType().equals("inv")
            inventoryMsg.getCount().value.toString().equals("1")
        where:
            byteInterval | delayMs
                10       |    15
    }

    def "testing invMessage COMPLETE Serializing"() {
        given:
            ProtocolConfig config = new ProtocolBSVMainConfig()
            SerializerContext context = SerializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .build()
            InventoryVectorMsg inventoryVectorMsg  = InventoryVectorMsg.builder()
                    .type(REF_INV_VEC_TYPE)
                    .hashMsg(REF_HASH_MSG)
                    .build()
            List<InventoryVectorMsg> msgList = new ArrayList<>();
            msgList.add(inventoryVectorMsg);
            VarIntMsg count = VarIntMsg.builder().value(msgList.size()).build();
            InvMessage invMessage = InvMessage.builder().invVectorMsgList(msgList).build();

            BitcoinMsg<InvMessage> inventoryBitcoinMsg = new BitcoinMsgBuilder<>(config.getBasicConfig(), invMessage).build()
            BitcoinMsgSerializer serializer = BitcoinMsgSerializerImpl.getInstance()
        when:
            byte[] bytes = serializer.serialize(context, inventoryBitcoinMsg, InvMessage.MESSAGE_TYPE).getFullContent()
            String invMsgSerialized = HEX.encode(bytes)
        then:
           invMsgSerialized.equals(REF_INV_MSG_FULL)
    }

    def "testing invMessage COMPLETE De-serializing"(int byteInterval, int delayMs) {
        given:
            ProtocolConfig config = new ProtocolBSVMainConfig()
            DeserializerContext context = DeserializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .maxBytesToRead((long) (REF_INV_MSG_FULL.length() / 2))
                    .build()
            ByteArrayReader byteReader = ByteArrayArtificalStreamProducer.stream(HEX.decode(REF_INV_MSG_FULL), byteInterval, delayMs);

            BitcoinMsgSerializer bitcoinSerializer = BitcoinMsgSerializerImpl.getInstance()
        when:
            BitcoinMsg<InvMessage> invBitcoinMsg = bitcoinSerializer.deserialize(context, byteReader, InvMessage.MESSAGE_TYPE)
        then:
            invBitcoinMsg.getHeader().getMagic().equals(config.getBasicConfig().getMagicPackage())
            invBitcoinMsg.getHeader().getCommand().equals(InvMessage.MESSAGE_TYPE)
            List<InventoryVectorMsg> inventoryList = invBitcoinMsg.getBody().getInvVectorList()
            InventoryVectorMsg inventoryVectorMsg = inventoryList.get(0)
            inventoryList.size() == 1
            inventoryVectorMsg.getType() == REF_INV_VEC_TYPE
        where:
            byteInterval | delayMs
                10       |    15
    }

  def "testing invMessage throwing Exception "() {
        given:
            InventoryVectorMsg inventoryVectorMsg  = InventoryVectorMsg.builder()
                    .type(REF_INV_VEC_TYPE)
                    .hashMsg(REF_HASH_MSG)
                    .build()
            List<InventoryVectorMsg> msgList = new ArrayList<>();

            for(int i=0; i < InvMessage.MAX_ADDRESSES + 1; i++) {
                msgList.add(inventoryVectorMsg);
            }
        when:
            InvMessage invMessage = InvMessage.builder().invVectorMsgList(msgList).build();
        then:
            final java.lang.IllegalArgumentException ex = thrown()
            ex.message == 'Inv message too largeMsgs.'

    }


}
