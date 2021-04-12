package com.nchain.jcl.net.unit.protocol.serialization

import com.nchain.jcl.net.protocol.config.ProtocolConfig
import com.nchain.jcl.net.protocol.config.ProtocolConfigBuilder
import com.nchain.jcl.net.protocol.messages.BlockTransactionsRequestMsg
import com.nchain.jcl.net.protocol.messages.HashMsg
import com.nchain.jcl.net.protocol.messages.VarIntMsg
import com.nchain.jcl.net.protocol.serialization.BlockTransactionsRequestMsgSerializer
import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext
import com.nchain.jcl.net.protocol.serialization.common.SerializerContext
import com.nchain.jcl.net.unit.protocol.tools.ByteArrayArtificalStreamProducer
import com.nchain.jcl.tools.bytes.ByteArrayReader
import com.nchain.jcl.tools.bytes.ByteArrayWriter
import io.bitcoinj.core.Utils
import io.bitcoinj.params.MainNetParams
import io.bitcoinj.params.Net
import spock.lang.Specification

class BlockTransactionsRequestMsgSpec extends Specification {
    private static final String BLOCK_HASH = "40f11b68435988807d64dff20261f7d9827825fbb37542601fb94d4500000000"

    private static final String NUMBER_OF_INDEXES = "02"

    private static final List<String> INDEXES = Arrays.asList(
        "10",
        "a2"
    )

    private static final String MESSAGE_BYTES = BLOCK_HASH + NUMBER_OF_INDEXES + INDEXES.join()

    def "Testing BlockTransactionsRequestMsg Deserialize"(int byteInterval, int delayMs) {
        given:
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            DeserializerContext context = DeserializerContext.builder()
                .protocolBasicConfig(config.getBasicConfig())
                .build()

            byte[] bytes = Utils.HEX.decode(MESSAGE_BYTES)

            BlockTransactionsRequestMsgSerializer serializer = BlockTransactionsRequestMsgSerializer.getInstance()
            BlockTransactionsRequestMsg message

        when:
            ByteArrayReader byteReader = ByteArrayArtificalStreamProducer.stream(bytes, byteInterval, delayMs)
            message = serializer.deserialize(context, byteReader)

        then:
            message.messageType == BlockTransactionsRequestMsg.MESSAGE_TYPE
            message.getIndexesLength().getValue() == 2
            message.getIndexes().get(0).getValue() == 16
            message.getIndexes().get(1).getValue() == 162

        where:
            byteInterval | delayMs
            10           | 5
    }

    def "Testing BlockTransactionsRequestMsg Serializing"() {
        given:
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            SerializerContext context = SerializerContext.builder()
                .protocolBasicConfig(config.getBasicConfig())
                .build()

            BlockTransactionsRequestMsgSerializer serializer = BlockTransactionsRequestMsgSerializer.getInstance()

            BlockTransactionsRequestMsg blockHeaderMsg = BlockTransactionsRequestMsg.builder()
                .blockHash(
                    HashMsg.builder()
                        .hash(Utils.HEX.decode(BLOCK_HASH))
                        .build()
                )
                .indexesLength(
                    VarIntMsg.builder()
                        .value(2)
                        .build()
                )
                .indexes(
                    Arrays.asList(
                        VarIntMsg.builder().value(16).build(),
                        VarIntMsg.builder().value(162).build()
                    )
                )
                .build()

            String messageSerializedBytes
        when:
            ByteArrayWriter byteWriter = new ByteArrayWriter()
            serializer.serialize(context, blockHeaderMsg, byteWriter)
            messageSerializedBytes = Utils.HEX.encode(byteWriter.reader().getFullContent())
            byteWriter.reader()

        then:
            messageSerializedBytes == MESSAGE_BYTES
    }
}
