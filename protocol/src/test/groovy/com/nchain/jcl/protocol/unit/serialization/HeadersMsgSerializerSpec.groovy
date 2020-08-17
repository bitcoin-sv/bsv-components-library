package com.nchain.jcl.protocol.unit.serialization

import com.nchain.jcl.protocol.config.ProtocolConfig
import com.nchain.jcl.protocol.config.provided.ProtocolBSVMainConfig
import com.nchain.jcl.protocol.messages.BlockHeaderMsg
import com.nchain.jcl.protocol.messages.HashMsg
import com.nchain.jcl.protocol.messages.HeaderMsg
import com.nchain.jcl.protocol.messages.HeadersMsg
import com.nchain.jcl.protocol.messages.common.BitcoinMsg
import com.nchain.jcl.protocol.serialization.HeadersMsgSerializer
import com.nchain.jcl.protocol.serialization.common.BitcoinMsgSerializer
import com.nchain.jcl.protocol.serialization.common.BitcoinMsgSerializerImpl
import com.nchain.jcl.protocol.serialization.common.DeserializerContext
import com.nchain.jcl.protocol.serialization.common.SerializerContext
import com.nchain.jcl.tools.bytes.ByteArrayReader
import com.nchain.jcl.tools.bytes.ByteArrayWriter
import com.nchain.jcl.tools.crypto.Sha256Wrapper
import spock.lang.Specification


/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 20/08/2019
 *
 * Testing class for the HeadersMsg Serialization.
 * The test is taken the assumption that we have already a correct serialization version of this Message, obtained
 * from another source that we trust (in this case the Java BitcoinJ library). So we serialize/deserialize some
 * messages with out code and compare the results with that reference.
 */
class HeadersMsgSerializerSpec extends Specification {

    def "testing HeadersMsg Serializing and Deserializing"() {
        given:

        SerializerContext serializerContext = SerializerContext.builder()
                .build()

        DeserializerContext deserializerContext = DeserializerContext.builder()
                .build()

        BlockHeaderMsg blockHeaderMsg = BlockHeaderMsg.builder()
                .version(70013)
                .hash(HashMsg.builder().hash(Sha256Wrapper.wrap("a9f965385ffd6da76dbf8226ca0f061d6e05737fdf34ba6edb9ea1d012666b16").getBytes()).build())
                .prevBlockHash(HashMsg.builder().hash(Sha256Wrapper.ZERO_HASH.getBytes()).build())
                .merkleRoot(HashMsg.builder().hash(Sha256Wrapper.ZERO_HASH.getBytes()).build())
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
                .hash(HashMsg.builder().hash(Sha256Wrapper.wrap("a9f965385ffd6da76dbf8226ca0f061d6e05737fdf34ba6edb9ea1d012666b16").getBytes()).build())
                .prevBlockHash(HashMsg.builder().hash(Sha256Wrapper.ZERO_HASH.getBytes()).build())
                .merkleRoot(HashMsg.builder().hash(Sha256Wrapper.ZERO_HASH.getBytes()).build())
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

        BlockHeaderMsg blockHeaderMsg = BlockHeaderMsg.builder()
                .version(70013)
                .hash(HashMsg.builder().hash(Sha256Wrapper.wrap("a9f965385ffd6da76dbf8226ca0f061d6e05737fdf34ba6edb9ea1d012666b16").getBytes()).build())
                .prevBlockHash(HashMsg.builder().hash(Sha256Wrapper.ZERO_HASH.getBytes()).build())
                .merkleRoot(HashMsg.builder().hash(Sha256Wrapper.ZERO_HASH.getBytes()).build())
                .difficultyTarget(1)
                .creationTimestamp(0)
                .nonce(1)
                .transactionCount(1)
                .build()

        HeadersMsg headersMsg = HeadersMsg.builder().blockHeaderMsgList(Arrays.asList(blockHeaderMsg)).build()

        HeaderMsg headerMsg = HeaderMsg.builder()
                .checksum(671030158)
                .command(HeadersMsg.MESSAGE_TYPE)
                .length((int)headersMsg.getLengthInBytes())
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