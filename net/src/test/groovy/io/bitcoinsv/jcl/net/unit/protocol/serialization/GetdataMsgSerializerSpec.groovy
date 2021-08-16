package io.bitcoinsv.jcl.net.unit.protocol.serialization


import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader
import io.bitcoinsv.jcl.tools.bytes.ByteArrayWriter
import io.bitcoinj.core.Sha256Hash
import io.bitcoinj.core.Utils
import io.bitcoinj.params.MainNetParams
import io.bitcoinj.params.Net
import io.bitcoinsv.jcl.net.protocol.config.ProtocolConfig
import io.bitcoinsv.jcl.net.protocol.config.ProtocolConfigBuilder
import io.bitcoinsv.jcl.net.protocol.messages.GetdataMsg
import io.bitcoinsv.jcl.net.protocol.messages.HashMsg
import io.bitcoinsv.jcl.net.protocol.messages.InventoryVectorMsg
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsg
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsgBuilder
import io.bitcoinsv.jcl.net.protocol.serialization.GetdataMsgSerializer
import io.bitcoinsv.jcl.net.protocol.serialization.common.BitcoinMsgSerializer
import io.bitcoinsv.jcl.net.protocol.serialization.common.BitcoinMsgSerializerImpl
import io.bitcoinsv.jcl.net.protocol.serialization.common.DeserializerContext
import io.bitcoinsv.jcl.net.protocol.serialization.common.SerializerContext
import io.bitcoinsv.jcl.net.unit.protocol.tools.ByteArrayArtificalStreamProducer
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

    // Body Message
    private static final String REF_GETDATA_MSG_BODY = "01010000002f0a833496c4500bf4af0f11763cfa9658368791dc8cd0a69654de5743b356df"

    // Hash of the Item inside the GetData:
    private static final String REF_ITEM_HASH = "2f0a833496c4500bf4af0f11763cfa9658368791dc8cd0a69654de5743b356df"

    // Whole Body, including Header:
    private static final String REF_GETDATA_MSG_FULL = "e3e1f3e8676574646174610000000000250000001d9656c901010000002f0a833496c4500bf4af0f11763cfa9658368791dc8cd0a69654de5743b356df"



    def "testing getDataMessage  BODY De-Serializing"(int byteInterval, int delayMs) {
        given:
        ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
        DeserializerContext context = DeserializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .maxBytesToRead((long) (REF_GETDATA_MSG_BODY.length()/2))
                    .build()
        GetdataMsg inventoryMsg
            ByteArrayReader byteReader = ByteArrayArtificalStreamProducer.stream(Utils.HEX.decode(REF_GETDATA_MSG_BODY), byteInterval, delayMs);
        when:
            inventoryMsg = GetdataMsgSerializer.getInstance().deserialize(context, byteReader)
        then:
            inventoryMsg.getMessageType().equals(GetdataMsg.MESSAGE_TYPE)
            inventoryMsg.getCount().value == 1
        where:
            byteInterval | delayMs
                10       |    15
    }

    def "testing getDataMessage BODY Serializing"() {
        given:
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
        SerializerContext context  = SerializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .build()
        InventoryVectorMsg inventoryVectorMsg  =  InventoryVectorMsg.builder()
                    .type(InventoryVectorMsg.VectorType.MSG_TX)
                    .hashMsg(HashMsg.builder().hash(Utils.HEX.decode(REF_ITEM_HASH)).build())
                    .build()

            List<InventoryVectorMsg> msgList = new ArrayList<>();
            msgList.add(inventoryVectorMsg)
            GetdataMsg getdataMsg = GetdataMsg.builder().invVectorList(msgList).build();
            ByteArrayWriter byteWriter = new ByteArrayWriter()
            String messageSerialized
        when:
            GetdataMsgSerializer.getInstance().serialize(context, getdataMsg, byteWriter)
            byte[] messageBytes = byteWriter.reader().getFullContent()
            messageSerialized = Utils.HEX.encode(messageBytes)
        then:
            messageSerialized.equals(REF_GETDATA_MSG_BODY)
    }

    def "testing getDataMessage COMPLETE De-serializing"(int byteInterval, int delayMs) {
        given:
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            DeserializerContext context = DeserializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .maxBytesToRead((long) (REF_GETDATA_MSG_FULL.length() / 2))
                    .build()
            ByteArrayReader byteReader = ByteArrayArtificalStreamProducer.stream(Utils.HEX.decode(REF_GETDATA_MSG_FULL), byteInterval, delayMs);
        BitcoinMsgSerializer bitcoinSerializer = BitcoinMsgSerializerImpl.getInstance()
        when:
        BitcoinMsg<GetdataMsg> getDataMsg = bitcoinSerializer.deserialize(context, byteReader, GetdataMsg.MESSAGE_TYPE)
        then:
            getDataMsg.getHeader().getMagic().equals(config.getBasicConfig().getMagicPackage())
            getDataMsg.getHeader().getCommand().equals(GetdataMsg.MESSAGE_TYPE)
            List<InventoryVectorMsg> inventoryList = getDataMsg.getBody().getInvVectorList()
            inventoryList.size() == 1
            Sha256Hash.wrap(inventoryList.get(0).hashMsg.hashBytes).toString().equals(REF_ITEM_HASH)
        where:
            byteInterval | delayMs
                10       |    15
    }

    def "testing getData COMPLETE Serializing"() {
        given:
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            SerializerContext context = SerializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .build()
            InventoryVectorMsg inventoryVectorMsg  = InventoryVectorMsg.builder()
                    .type(InventoryVectorMsg.VectorType.MSG_TX)
                    .hashMsg(HashMsg.builder().hash(Utils.HEX.decode(REF_ITEM_HASH)).build())
                    .build()
            List<InventoryVectorMsg> msgList = new ArrayList<>();
            msgList.add(inventoryVectorMsg);
            GetdataMsg getdataMsg = GetdataMsg.builder().invVectorList(msgList).build();

            BitcoinMsg<GetdataMsg> getdataMsgBitcoinMsg = new BitcoinMsgBuilder<>(config.getBasicConfig(), getdataMsg).build()
            BitcoinMsgSerializer serializer = BitcoinMsgSerializerImpl.getInstance()
        when:
            byte[] bytes = serializer.serialize(context, getdataMsgBitcoinMsg, GetdataMsg.MESSAGE_TYPE).getFullContent()
            String serialized = Utils.HEX.encode(bytes)
        then:
             serialized.equals(REF_GETDATA_MSG_FULL)
    }
}
