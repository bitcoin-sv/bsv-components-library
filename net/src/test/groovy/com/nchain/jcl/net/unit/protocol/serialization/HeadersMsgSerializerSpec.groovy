package com.nchain.jcl.net.unit.protocol.serialization


import com.nchain.jcl.net.protocol.messages.BlockHeaderMsg
import com.nchain.jcl.net.protocol.messages.HashMsg
import com.nchain.jcl.net.protocol.messages.HeaderMsg
import com.nchain.jcl.net.protocol.messages.HeadersMsg
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg
import com.nchain.jcl.net.protocol.serialization.HeadersMsgSerializer
import com.nchain.jcl.net.protocol.serialization.common.BitcoinMsgSerializer
import com.nchain.jcl.net.protocol.serialization.common.BitcoinMsgSerializerImpl
import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext
import com.nchain.jcl.net.protocol.serialization.common.SerializerContext
import com.nchain.jcl.tools.bytes.ByteArrayReader
import com.nchain.jcl.tools.bytes.ByteArrayWriter
import io.bitcoinj.core.Sha256Hash
import io.bitcoinj.core.Utils
import spock.lang.Ignore
import spock.lang.Specification

/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 20/08/2019
 *
 * Testing class for the HeadersMsg Serialization and Deserialization
 */
class HeadersMsgSerializerSpec extends Specification {

    private String VALID_HEADER = "02000000205e1fca54e6fd0a693dd0972354410eab83257ed88c62e60200000000000000003ebc40fea1c8d229e93c9bc72324b2d2012d569786cd7291c0ede1159ce6161939903e5f8a140418b4e8b2040000e0ff2f6ada22d693ecbb7fab93f28f59987b900e24a7a59889af020000000000000000c58585f70e2dbc058679bde693f23da24fa062f2d2e8df9949f9cb7d6ced60fb19a43e5fafdb03189019645f00";

    // TODO: Commenting this test, since the data is wrong. The hash used in this test is NOT in the VALID_HEADER above
    // TODO: Apart from that, the test uses a List of 1 Header with ZERO HASHEs in some places, which are NOt in the VALID_HEADER above
    @Ignore
    def "testing HeadersMsg Serializing and Deserializing"() {
        given:

            SerializerContext serializerContext = SerializerContext.builder()
                .build()

            DeserializerContext deserializerContext = DeserializerContext.builder()
                .build()

            BlockHeaderMsg blockHeaderMsg = BlockHeaderMsg.builder()
                .version(70013)
                .hash(HashMsg.builder().hash(Sha256Hash.wrap("a9f965385ffd6da76dbf8226ca0f061d6e05737fdf34ba6edb9ea1d012666b16").getBytes()).build())
                .prevBlockHash(HashMsg.builder().hash(Sha256Hash.ZERO_HASH.getBytes()).build())
                .merkleRoot(HashMsg.builder().hash(Sha256Hash.ZERO_HASH.getBytes()).build())
                .difficultyTarget(1)
                .creationTimestamp(0)
                .nonce(1)
                .transactionCount(1)
                .build()

            HeadersMsg headersMsg = HeadersMsg.builder().blockHeaderMsgList(Arrays.asList(blockHeaderMsg)).build()

            ByteArrayWriter byteWriter = new ByteArrayWriter()
        when:
            HeadersMsgSerializer.getInstance().serialize(serializerContext, headersMsg, byteWriter)
            HeadersMsg deserializedHeadersMsg = HeadersMsgSerializer.getInstance().deserialize(deserializerContext, byteWriter.reader())
        then:
            blockHeaderMsg.equals(deserializedHeadersMsg.getBlockHeaderMsgList().get(0))
    }

    def "testing HeadersMsg throwing Exception "() {
        given:
            BlockHeaderMsg blockHeaderMsg = BlockHeaderMsg.builder()
                .version(70013)
                .hash(HashMsg.builder().hash(Sha256Hash.wrap("a9f965385ffd6da76dbf8226ca0f061d6e05737fdf34ba6edb9ea1d012666b16").getBytes()).build())
                .prevBlockHash(HashMsg.builder().hash(Sha256Hash.ZERO_HASH.getBytes()).build())
                .merkleRoot(HashMsg.builder().hash(Sha256Hash.ZERO_HASH.getBytes()).build())
                .difficultyTarget(1)
                .creationTimestamp(0)
                .nonce(1)
                .transactionCount(1)
                .build()

            List<BlockHeaderMsg> msgList = new ArrayList<>();

        when:
            for (int i = 0; i < HeadersMsg.MAX_ADDRESSES + 1; i++) {
                msgList.add(blockHeaderMsg);
            }

            HeadersMsg.builder().blockHeaderMsgList(msgList).build();
        then:
            IllegalArgumentException ex = thrown()
            ex.message == 'Headers message exceeds maximum size'

    }

    def "testing HeadersMsg COMPLETE Serializing and Deserializing"() {
        given:

            SerializerContext serializerContext = SerializerContext.builder()
                .build()

            DeserializerContext deserializerContext = DeserializerContext.builder()
                .build()

            ByteArrayWriter byteArrayWriter = new ByteArrayWriter()
            byteArrayWriter.write(Utils.HEX.decode(VALID_HEADER))

            HeadersMsg headersMsg = HeadersMsgSerializer.getInstance().deserialize(deserializerContext, byteArrayWriter.reader())

            HeaderMsg headerMsg = HeaderMsg.builder()
                .checksum(714299174)
                .command(HeadersMsg.MESSAGE_TYPE)
                .length((int) headersMsg.getLengthInBytes())
                .magic(1)
                .build();

            BitcoinMsg<HeadersMsg> headersBitcoinMsg = new BitcoinMsg<HeadersMsg>(headerMsg, headersMsg);

            BitcoinMsgSerializer serializer = BitcoinMsgSerializerImpl.getInstance()
        when:
            ByteArrayReader byteArrayReader = serializer.serialize(serializerContext, headersBitcoinMsg, HeadersMsg.MESSAGE_TYPE)
            BitcoinMsg<HeadersMsg> deserializedHeadersBitcoinMsg = serializer.deserialize(deserializerContext, byteArrayReader, HeadersMsg.MESSAGE_TYPE)
        then:
            headersBitcoinMsg.equals(deserializedHeadersBitcoinMsg)

    }

}
