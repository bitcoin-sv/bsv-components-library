/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.unit.protocol.serialization


import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader
import io.bitcoinsv.jcl.tools.bytes.ByteArrayWriter
import io.bitcoinsv.bitcoinjsv.core.Sha256Hash
import io.bitcoinsv.bitcoinjsv.core.Utils
import io.bitcoinsv.bitcoinjsv.params.MainNetParams
import io.bitcoinsv.bitcoinjsv.params.Net
import io.bitcoinsv.jcl.net.protocol.config.ProtocolConfig
import io.bitcoinsv.jcl.net.protocol.config.ProtocolConfigBuilder
import io.bitcoinsv.jcl.net.protocol.messages.HashMsg
import io.bitcoinsv.jcl.net.protocol.messages.InventoryVectorMsg
import io.bitcoinsv.jcl.net.protocol.serialization.InventoryVectorMsgSerializer
import io.bitcoinsv.jcl.net.protocol.serialization.common.DeserializerContext
import io.bitcoinsv.jcl.net.protocol.serialization.common.SerializerContext
import io.bitcoinsv.jcl.net.unit.protocol.tools.ByteArrayArtificalStreamProducer
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

    // Whole Message in Hex format
    private static final String REF_MSG = "010000000ab288d8e32c31c81d8860149f1adf3c0355db67f6cf52cadf7e2a9a001810bb"

    // Hash in Hex format (in little endinan, as it comes from the wire):
    private static final String REF_HASH = "0ab288d8e32c31c81d8860149f1adf3c0355db67f6cf52cadf7e2a9a001810bb"

    // Item Type:
    private static final InventoryVectorMsg.VectorType REF_TYPE = InventoryVectorMsg.VectorType.MSG_TX


    def "testing InventoryVector Message BODY Serializing"() {
        given:
        ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
        SerializerContext context = SerializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .build()
            InventoryVectorMsg inventoryVectorMsg  = InventoryVectorMsg.builder()
                    .type(REF_TYPE)
                    .hashMsg(HashMsg.builder().hash(Utils.HEX.decode(REF_HASH)).build())
                    .build();

            ByteArrayWriter byteWriter = new ByteArrayWriter()
            String messageSerialized = null
        when:
        InventoryVectorMsgSerializer.getInstance().serialize(context, inventoryVectorMsg, byteWriter)
            byte[] messageBytes = byteWriter.reader().getFullContent()
            messageSerialized = Utils.HEX.encode(messageBytes)
        then:
             messageSerialized.equals(REF_MSG)
    }

    def "testing InventoryVector Message BODY De-Serializing"(int byteInterval, int delayMs) {
        given:
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
        DeserializerContext context = DeserializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .maxBytesToRead((long) (REF_MSG.length()/2))
                    .build()
            InventoryVectorMsg inventoryVectorMsg = null
            ByteArrayReader byteReader = ByteArrayArtificalStreamProducer.stream(Utils.HEX.decode(REF_MSG), byteInterval, delayMs);
        when:
             inventoryVectorMsg = InventoryVectorMsgSerializer.getInstance().deserialize(context, byteReader)
        then:
             inventoryVectorMsg.getType().equals(REF_TYPE)
             Sha256Hash.wrap(inventoryVectorMsg.hashMsg.hashBytes).toString().equals(REF_HASH)
        where:
            byteInterval | delayMs
                10       |    15
    }

}
