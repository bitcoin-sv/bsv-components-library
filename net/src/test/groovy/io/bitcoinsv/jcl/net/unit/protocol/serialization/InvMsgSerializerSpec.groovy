package io.bitcoinsv.jcl.net.unit.protocol.serialization

import io.bitcoinsv.jcl.net.protocol.config.ProtocolConfigBuilder
import io.bitcoinsv.jcl.net.protocol.serialization.InventoryVectorMsgSerializer
import io.bitcoinsv.jcl.net.protocol.serialization.common.BitcoinMsgSerializer
import io.bitcoinsv.jcl.net.protocol.serialization.common.BitcoinMsgSerializerImpl
import io.bitcoinsv.jcl.net.protocol.serialization.common.DeserializerContext
import io.bitcoinsv.jcl.net.protocol.serialization.common.SerializerContext
import io.bitcoinsv.jcl.net.protocol.config.ProtocolConfig
import io.bitcoinsv.jcl.net.protocol.messages.InvMessage
import io.bitcoinsv.jcl.net.protocol.messages.InventoryVectorMsg
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsg
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsgBuilder
import io.bitcoinsv.jcl.net.protocol.serialization.InvMsgSerializer
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
 * @date 20/08/2019
 *
 * Testing class for the InvMessage Serialization.
 * The test is taken the assumption that we have already a correct serialization version of this Message, obtained
 * from another source that we trust (in this case the Java BitcoinJ library). So we serialize/deserialize some
 * messages with out code and compare the results with that reference.
 */
class InvMsgSerializerSpec extends Specification {

    // Body MSG in Hex format,
    public static final String REF_INV_MSG_BODY =  "01010000000ab288d8e32c31c81d8860149f1adf3c0355db67f6cf52cadf7e2a9a001810bb"
    public static final int REF_NUM_ITEMS = 1
    public static final String REF_INV_ITEM = "010000000ab288d8e32c31c81d8860149f1adf3c0355db67f6cf52cadf7e2a9a001810bb"

    // Full MSG, including Header:
    private static final String REF_INV_MSG_FULL = "e3e1f3e8696e7600000000000000000025000000f9e97b6301010000000ab288d8e32c31c81d8860149f1adf3c0355db67f6cf52cadf7e2a9a001810bb"


    def "testing invMessage BODY Serializing"() {
        given:
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            SerializerContext context  = SerializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .build()

            // We build the item by deserializing the content
            ByteArrayReader reader = new ByteArrayReader(Utils.HEX.decode(REF_INV_ITEM))
            InventoryVectorMsg inventoryVectorMsg  = InventoryVectorMsgSerializer.getInstance().deserialize(null, reader)

            InvMessage invMessage = InvMessage.builder()
                    .invVectorMsgList(Arrays.asList(inventoryVectorMsg))
                    .build()
            ByteArrayWriter byteWriter = new ByteArrayWriter()
            String messageSerialized = null
        when:
            InvMsgSerializer.getInstance().serialize(context, invMessage, byteWriter)
            byte[] messageBytes = byteWriter.reader()getFullContent()
            messageSerialized = Utils.HEX.encode(messageBytes)
        then:
            messageSerialized.equals(REF_INV_MSG_BODY)
    }

    def "testing invMessage BODY De-Serializing"(int byteInterval, int delayMs) {
        given:
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            DeserializerContext context = DeserializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .maxBytesToRead((long) (REF_INV_MSG_BODY.length()/2))
                    .build()
            InvMessage inventoryMsg = null
            ByteArrayReader byteReader = ByteArrayArtificalStreamProducer.stream(Utils.HEX.decode(REF_INV_MSG_BODY), byteInterval, delayMs);

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
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            SerializerContext context = SerializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .build()
            // We build the item by deserializing the content
            ByteArrayReader reader = new ByteArrayReader(Utils.HEX.decode(REF_INV_ITEM))
            InventoryVectorMsg inventoryVectorMsg  = InventoryVectorMsgSerializer.getInstance().deserialize(null, reader)

            InvMessage invMessage = InvMessage.builder()
                    .invVectorMsgList(Arrays.asList(inventoryVectorMsg))
                    .build()

            BitcoinMsg<InvMessage> inventoryBitcoinMsg = new BitcoinMsgBuilder<>(config.getBasicConfig(), invMessage).build()
            BitcoinMsgSerializer serializer = BitcoinMsgSerializerImpl.getInstance()
        when:
            byte[] bytes = serializer.serialize(context, inventoryBitcoinMsg, InvMessage.MESSAGE_TYPE).getFullContent()
            String invMsgSerialized = Utils.HEX.encode(bytes)
        then:
           invMsgSerialized.equals(REF_INV_MSG_FULL)
    }

    def "testing invMessage COMPLETE De-serializing"(int byteInterval, int delayMs) {
        given:
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            DeserializerContext context = DeserializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .maxBytesToRead((long) (REF_INV_MSG_FULL.length() / 2))
                    .build()
            ByteArrayReader byteReader = ByteArrayArtificalStreamProducer.stream(Utils.HEX.decode(REF_INV_MSG_FULL), byteInterval, delayMs);

            BitcoinMsgSerializer bitcoinSerializer = BitcoinMsgSerializerImpl.getInstance()
        when:
            BitcoinMsg<InvMessage> invBitcoinMsg = bitcoinSerializer.deserialize(context, byteReader, InvMessage.MESSAGE_TYPE)
        then:
            invBitcoinMsg.getHeader().getMagic().equals(config.getBasicConfig().getMagicPackage())
            invBitcoinMsg.getHeader().getCommand().equals(InvMessage.MESSAGE_TYPE)
            List<InventoryVectorMsg> inventoryList = invBitcoinMsg.getBody().getInvVectorList()
            inventoryList.size() == 1
        where:
            byteInterval | delayMs
                10       |    15
    }

  def "testing invMessage throwing Exception "() {
        given:
            ByteArrayReader reader = new ByteArrayReader(Utils.HEX.decode(REF_INV_ITEM))
            InventoryVectorMsg inventoryVectorMsg  = InventoryVectorMsgSerializer.getInstance().deserialize(null, reader)
            List<InventoryVectorMsg> msgList = new ArrayList<>();

            for(int i=0; i < InvMessage.MAX_ADDRESSES + 1; i++) {
                msgList.add(inventoryVectorMsg);
            }
        when:
            InvMessage.builder().invVectorMsgList(msgList).build();
        then:
            final java.lang.IllegalArgumentException ex = thrown()
            ex.message == 'Inv message too largeMsgs.'

    }


}
