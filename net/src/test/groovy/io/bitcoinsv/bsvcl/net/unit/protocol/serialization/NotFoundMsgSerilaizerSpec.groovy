package io.bitcoinsv.bsvcl.net.unit.protocol.serialization


import io.bitcoinsv.bsvcl.common.bytes.ByteArrayReader
import io.bitcoinsv.bsvcl.common.bytes.ByteArrayWriter
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
 * @date 04/09/2019
 *
 * Testing class for the  class NotFoundMsgSerilaizer
 *
 * The test is taken the assumption that we have already a correct serialization version of this Message, obtained
 * from another source that we trust (in this case the Java BitcoinJ library). So we serialize/deserialize some
 * messages with out code and compare the results with that reference.
 */
class NotFoundMsgSerilaizerSpec extends Specification {
    // Body of the Message
    private static final String REF_NOTFOUND_MSG_BODY = "0101000000a69d45e7abc3b8fc363d13b88aaa2f2ec62bf77b6881e8bd7bd1012fd81d802b"

    // Hash of the Item, as it comes on the wire (litle endian)
    public static final byte[] REF_INV_MSG_BITES = Sha256Hash.wrap("a69d45e7abc3b8fc363d13b88aaa2f2ec62bf77b6881e8bd7bd1012fd81d802b").getBytes()
    private static final io.bitcoinsv.bsvcl.net.protocol.messages.HashMsg REF_HASH_MSG = io.bitcoinsv.bsvcl.net.protocol.messages.HashMsg.builder().hash(REF_INV_MSG_BITES).build()

    // Full Message, inluding Header:
    private static final String REF_NOTFOUND_MSG_FULL = "e3e1f3e86e6f74666f756e640000000025000000e27152ce0101000000a69d45e7abc3b8fc363d13b88aaa2f2ec62bf77b6881e8bd7bd1012fd81d802b"

    def "testing notFoundMessage BODY Serializing"() {
        given:
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig config = io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext context  = io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext.builder()
                .protocolBasicConfig(config.getBasicConfig())
                .build()
            io.bitcoinsv.bsvcl.net.protocol.messages.InventoryVectorMsg inventoryVectorMsg  = io.bitcoinsv.bsvcl.net.protocol.messages.InventoryVectorMsg.builder()
                .type(io.bitcoinsv.bsvcl.net.protocol.messages.InventoryVectorMsg.VectorType.MSG_TX)
                .hashMsg(REF_HASH_MSG)
                .build()
            List<io.bitcoinsv.bsvcl.net.protocol.messages.InventoryVectorMsg> msgList = new ArrayList<>();
            msgList.add(inventoryVectorMsg);
            io.bitcoinsv.bsvcl.net.protocol.messages.VarIntMsg count = io.bitcoinsv.bsvcl.net.protocol.messages.VarIntMsg.builder().value(msgList.size()).build();
            io.bitcoinsv.bsvcl.net.protocol.messages.NotFoundMsg getdataMsg = io.bitcoinsv.bsvcl.net.protocol.messages.NotFoundMsg.builder().invVectorMsgList(msgList).build();

            ByteArrayWriter byteWriter = new ByteArrayWriter()
            String messageSerialized = null
        when:
            io.bitcoinsv.bsvcl.net.protocol.serialization.NotFoundMsgSerilaizer.getInstance().serialize(context, getdataMsg, byteWriter)
            byte[] messageBytes = byteWriter.reader().getFullContent()
            messageSerialized = Utils.HEX.encode(messageBytes)
        then:
            messageSerialized.equals(REF_NOTFOUND_MSG_BODY)
    }

    def "testing notFoundMsg  BODY De-Serializing"(int byteInterval, int delayMs) {
        given:
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig config = io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext context = io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext.builder()
                .protocolBasicConfig(config.getBasicConfig())
                .maxBytesToRead((long) (REF_NOTFOUND_MSG_BODY.length()/2))
                .build()
            io.bitcoinsv.bsvcl.net.protocol.messages.NotFoundMsg inventoryMsg = null
            ByteArrayReader byteArrayReader = io.bitcoinsv.bsvcl.net.unit.protocol.tools.ByteArrayArtificalStreamProducer.stream(Utils.HEX.decode(REF_NOTFOUND_MSG_BODY), byteInterval, delayMs);
        when:
            inventoryMsg = io.bitcoinsv.bsvcl.net.protocol.serialization.NotFoundMsgSerilaizer.getInstance().deserialize(context, byteArrayReader)
        then:
            inventoryMsg.getMessageType().equals(io.bitcoinsv.bsvcl.net.protocol.messages.NotFoundMsg.MESSAGE_TYPE)
            inventoryMsg.getCount().value.toString().equals("1")
        where:
            byteInterval | delayMs
                10       |    75
    }

    def "testing notFoundMessage COMPLETE Serializing"() {
        given:
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig config = io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext context = io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext.builder()
                .protocolBasicConfig(config.getBasicConfig())
                .build()
            io.bitcoinsv.bsvcl.net.protocol.messages.InventoryVectorMsg inventoryVectorMsg  = io.bitcoinsv.bsvcl.net.protocol.messages.InventoryVectorMsg.builder()
                .type(io.bitcoinsv.bsvcl.net.protocol.messages.InventoryVectorMsg.VectorType.MSG_TX)
                .hashMsg(REF_HASH_MSG)
                .build()
            List<io.bitcoinsv.bsvcl.net.protocol.messages.InventoryVectorMsg> msgList = new ArrayList<>();
            msgList.add(inventoryVectorMsg);
            io.bitcoinsv.bsvcl.net.protocol.messages.VarIntMsg count = io.bitcoinsv.bsvcl.net.protocol.messages.VarIntMsg.builder().value(msgList.size()).build();
            io.bitcoinsv.bsvcl.net.protocol.messages.NotFoundMsg getdataMsg = io.bitcoinsv.bsvcl.net.protocol.messages.NotFoundMsg.builder().invVectorMsgList(msgList).build();

            io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsg<io.bitcoinsv.bsvcl.net.protocol.messages.NotFoundMsg> notFoundMsgBitcoinMsg = new io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsgBuilder<>(config.getBasicConfig(), getdataMsg).build()
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.BitcoinMsgSerializer serializer = io.bitcoinsv.bsvcl.net.protocol.serialization.common.BitcoinMsgSerializerImpl.getInstance()
        when:
            byte[] bytes = serializer.serialize(context, notFoundMsgBitcoinMsg).getFullContent()
            String serialized = Utils.HEX.encode(bytes)
        then:
            serialized.equals(REF_NOTFOUND_MSG_FULL)
    }

    def "testing notFoundMessage COMPLETE De-serializing"(int byteInterval, int delayMs) {
        given:
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig config = io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext context = io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext.builder()
                .protocolBasicConfig(config.getBasicConfig())
                .maxBytesToRead((long) (REF_NOTFOUND_MSG_FULL.length() / 2))
                .build()
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.BitcoinMsgSerializer bitcoinSerializer = io.bitcoinsv.bsvcl.net.protocol.serialization.common.BitcoinMsgSerializerImpl.getInstance()
            ByteArrayReader byteArrayReader = io.bitcoinsv.bsvcl.net.unit.protocol.tools.ByteArrayArtificalStreamProducer.stream(Utils.HEX.decode(REF_NOTFOUND_MSG_FULL), byteInterval, delayMs);
        when:
            io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsg<io.bitcoinsv.bsvcl.net.protocol.messages.NotFoundMsg> getDataMsg = bitcoinSerializer.deserialize(context, byteArrayReader)
        then:
            getDataMsg.getHeader().getMagic().equals(config.getBasicConfig().getMagicPackage())
            getDataMsg.getHeader().getCommand().equals(io.bitcoinsv.bsvcl.net.protocol.messages.NotFoundMsg.MESSAGE_TYPE)
            List<io.bitcoinsv.bsvcl.net.protocol.messages.InventoryVectorMsg> inventoryList = getDataMsg.getBody().getInvVectorList()
            io.bitcoinsv.bsvcl.net.protocol.messages.InventoryVectorMsg inventoryVectorMsg = inventoryList.get(0)
            inventoryList.size() == 1
            inventoryVectorMsg.getType() == io.bitcoinsv.bsvcl.net.protocol.messages.InventoryVectorMsg.VectorType.MSG_TX
        where:
            byteInterval | delayMs
                10       |    15
    }
}
