package com.nchain.jcl.protocol.unit.serialization

import com.nchain.jcl.protocol.config.ProtocolConfig
import com.nchain.jcl.protocol.config.provided.ProtocolBSVMainConfig
import com.nchain.jcl.protocol.messages.HashMsg
import com.nchain.jcl.protocol.messages.InventoryVectorMsg
import com.nchain.jcl.protocol.messages.common.BitcoinMsg
import com.nchain.jcl.protocol.messages.common.BitcoinMsgBuilder
import com.nchain.jcl.protocol.serialization.InventoryVectorMsgSerializer
import com.nchain.jcl.protocol.serialization.common.*
import com.nchain.jcl.tools.bytes.HEX
import com.nchain.jcl.tools.crypto.Sha256Wrapper
import com.nchain.jcl.tools.bytes.ByteArrayReader
import com.nchain.jcl.tools.bytes.ByteArrayWriter
import spock.lang.Specification
/**
* @author m.jose@nchain.com
* Copyright (c) 2018-2019 Bitcoin Association
* Distributed under the Open BSV software license, see the accompanying file LICENSE.
* @date 13/08/2019
*
* Testing class for the InventoryVector Serialization.
 * The test is taken the assumption that we have already a correct serialization version of this Message, obtained
 * from another source that we trust (in this case the Java BitcoinJ library). So we serialize/deserialize some
 * messages with out code and compare the results with that reference.
*/
class InventoryVectorMsgSerializerSpec extends Specification {

    private static final String REF_INV_VEC_BODY_MSG = "01000000a69d45e7abc3b8fc363d13b88aaa2f2ec62bf77b6881e8bd7bd1012fd81d802b"
    private static final String REF_INV_VEC_FULL_MSG = "e3e1f3e8696e76656e746f727956656324000000653cfe1701000000a69d45e7abc3b8fc363d13b88aaa2f2ec62bf77b6881e8bd7bd1012fd81d802b"
    private static final InventoryVectorMsg.VectorType REF_INV_VEC_TYPE = InventoryVectorMsg.VectorType.MSG_TX
    public static final byte[] REF_INV_VEC_BYTES = Sha256Wrapper.wrap("2b801dd82f01d17bbde881687bf72bc62e2faa8ab8133d36fcb8c3abe7459da6").getBytes()
    private static final HashMsg REF_HASH_MSG = HashMsg.builder().hash(REF_INV_VEC_BYTES).build()



    def "testing InventoryVector Message BODY Serializing"() {
        given:
            ProtocolConfig config = new ProtocolBSVMainConfig()
            SerializerContext context = SerializerContext.builder()
                    .protocolconfig(config)
                    .build()
            InventoryVectorMsg inventoryVectorMsg  = InventoryVectorMsg.builder()
                    .type(REF_INV_VEC_TYPE)
                    .hashMsg(REF_HASH_MSG)
                    .build();

            ByteArrayWriter byteWriter = new ByteArrayWriter()
            String messageSerialized = null
        when:
            InventoryVectorMsgSerializer.getInstance().serialize(context, inventoryVectorMsg, byteWriter)
            byte[] messageBytes = byteWriter.reader().getFullContent()
            messageSerialized = HEX.encode(messageBytes)
        then:
             messageSerialized.equals(REF_INV_VEC_BODY_MSG)
    }

    def "testing InventoryVector Message BODY De-Serializing"() {
        given:
            ProtocolConfig config = new ProtocolBSVMainConfig()
            DeserializerContext context = DeserializerContext.builder()
                    .protocolconfig(config)
                    .maxBytesToRead((long) (REF_INV_VEC_BODY_MSG.length()/2))
                    .build()
            InventoryVectorMsg inventoryVectorMsg = null
            ByteArrayReader byteReader = new ByteArrayReader(HEX.decode(REF_INV_VEC_BODY_MSG))
        when:
             inventoryVectorMsg = InventoryVectorMsgSerializer.getInstance().deserialize(context, byteReader)
        then:
             inventoryVectorMsg.getType().getValue().equals(REF_INV_VEC_TYPE.value)
    }

    def "testing InventoryVector Message COMPLETE Serializing"() {
        given:
            ProtocolConfig config = new ProtocolBSVMainConfig()
            SerializerContext context = SerializerContext.builder()
                    .protocolconfig(config)
                    .build()
            InventoryVectorMsg msg = InventoryVectorMsg.builder()
                    .type(REF_INV_VEC_TYPE)
                    .hashMsg(REF_HASH_MSG).build()

        BitcoinMsg<InventoryVectorMsg> inventoryBitcoinMsg = new BitcoinMsgBuilder<>(config, msg).build()
        BitcoinMsgSerializer serializer = BitcoinMsgSerializerImpl.getInstance()
        when:
            byte[] bytes = serializer.serialize(context, inventoryBitcoinMsg, InventoryVectorMsg.MESSAGE_TYPE).getFullContent()
            String rejectMsgSerialized = HEX.encode(bytes)
        then:
            rejectMsgSerialized.equals(REF_INV_VEC_FULL_MSG)
    }

    def "testing InventoryVector Message COMPLETE De-serializing"() {
        given:
            ProtocolConfig config = new ProtocolBSVMainConfig()
            DeserializerContext context = DeserializerContext.builder()
                    .protocolconfig(config)
                    .build()
            ByteArrayReader byteReader = new ByteArrayReader(HEX.decode(REF_INV_VEC_FULL_MSG))
            BitcoinMsgSerializer bitcoinSerializer = BitcoinMsgSerializerImpl.getInstance()
        when:
            BitcoinMsg<InventoryVectorMsg> bitcoinMsg = bitcoinSerializer.deserialize(
                    context, byteReader, InventoryVectorMsg.MESSAGE_TYPE)
        then:
            bitcoinMsg.getHeader().getMagic().equals(config.getMagicPackage())
            bitcoinMsg.getHeader().getCommand().equals(InventoryVectorMsg.MESSAGE_TYPE)
            bitcoinMsg.getBody().type.getValue().equals(REF_INV_VEC_TYPE.value)
    }

}
