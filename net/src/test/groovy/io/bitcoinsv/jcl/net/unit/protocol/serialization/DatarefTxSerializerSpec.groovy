package io.bitcoinsv.jcl.net.unit.protocol.serialization

import io.bitcoinsv.bitcoinjsv.core.Sha256Hash
import io.bitcoinsv.bitcoinjsv.params.MainNetParams
import io.bitcoinsv.bitcoinjsv.params.Net
import io.bitcoinsv.bitcoinjsv.params.RegTestParams
import io.bitcoinsv.jcl.net.protocol.config.ProtocolConfig
import io.bitcoinsv.jcl.net.protocol.config.ProtocolConfigBuilder
import io.bitcoinsv.jcl.net.protocol.messages.DatarefTxMsg
import io.bitcoinsv.jcl.net.protocol.serialization.DatarefTxMsgSerializer
import io.bitcoinsv.jcl.net.protocol.serialization.common.DeserializerContext
import io.bitcoinsv.jcl.net.protocol.serialization.common.SerializerContext
import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader
import io.bitcoinsv.jcl.tools.bytes.ByteArrayWriter
import org.spongycastle.util.encoders.Hex
import spock.lang.Specification

/**
 * Copyright (c) 2024 nChain Ltd
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 *
 * Testing class for the DatarefTx message Serialization.
 *
 * @author nChain Ltd
 */
class DatarefTxSerializerSpec extends Specification {

    // Body Message in Hex Format:
    public static final String SERIALIZED_DATAREFTX_MSG ="01000000013c7551a04560944eedf4236788a21b117e60adbf21111a7cc79734f853b3cde90000000000ffffffff3100e1f50500000000015100e1f50500000000015100e1f50500000000015100e1f50500000000015100e1f50500000000015100e1f50500000000015100e1f50500000000015100e1f50500000000015100e1f50500000000015100e1f50500000000015100e1f50500000000015100e1f50500000000015100e1f50500000000015100e1f50500000000015100e1f50500000000015100e1f50500000000015100e1f50500000000015100e1f50500000000015100e1f50500000000015100e1f50500000000015100e1f50500000000015100e1f50500000000015100e1f50500000000015100e1f50500000000015100e1f50500000000015100e1f50500000000015100e1f50500000000015100e1f50500000000015100e1f50500000000015100e1f50500000000015100e1f50500000000015100e1f50500000000015100e1f50500000000015100e1f50500000000015100e1f50500000000015100e1f50500000000015100e1f50500000000015100e1f50500000000015100e1f50500000000015100e1f50500000000015100e1f50500000000015100e1f50500000000015100e1f50500000000015100e1f50500000000015100e1f50500000000015100e1f50500000000015100e1f50500000000015100e1f50500000000015100e1f5050000000001510000000005013d01000000014f993ee9eb84c0fd6fd840756a341d0a7e0a87259c96e4a86de23cba6dbad1ab0000000000ffffffff0100b4c40400000000015100000000c1f91e581fc2b79a427d1c7710f970e1f2f58e5003792dc720637182a92b38c401002fc9118bd483dec58822861586742a09ebe49c2839dd8f74601a2998075e9394"

    def "Testing DatarefTxMsg complete serializing"() {
        given:
        //config
        ProtocolConfig config = ProtocolConfigBuilder.get(new RegTestParams(Net.REGTEST))
        DeserializerContext deserializerContext = DeserializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .calculateHashes(true)
                    .build()
        SerializerContext serializerContext = SerializerContext.builder()
                .protocolBasicConfig(config.getBasicConfig())
                .build()
        //load serialized data into byte array
        ByteArrayReader reader = new ByteArrayReader(Hex.decode(SERIALIZED_DATAREFTX_MSG))
        //we use to deserialize this msg
        DatarefTxMsgSerializer datarefTxMsgSerializer = DatarefTxMsgSerializer.getInstance();
        //serialize back to this byte array for comparison
        ByteArrayWriter byteArrayWriter = new ByteArrayWriter()

        when:
        DatarefTxMsg datarefTxMsg = datarefTxMsgSerializer.deserialize(deserializerContext, reader)

            datarefTxMsgSerializer.serialize(serializerContext, datarefTxMsg, byteArrayWriter)

        then:
            Hex.toHexString(byteArrayWriter.reader().getFullContent()) == SERIALIZED_DATAREFTX_MSG// Do the serialize and deserialize output the same results?
            //check some last values to be deserialized within the object
            datarefTxMsg.getTxMsg().getLockTime() == 0
            datarefTxMsg.getTxMsg().getHash().get() == Sha256Hash.wrap("abd1ba6dba3ce26da8e4969c25870a7e0a1d346a7540d86ffdc084ebe93e994f")
            datarefTxMsg.getMerkleProofMsg().getNodeCount().value == 1
    }
}