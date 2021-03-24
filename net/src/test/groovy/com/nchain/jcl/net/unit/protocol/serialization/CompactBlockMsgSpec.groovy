package com.nchain.jcl.net.unit.protocol.serialization

import com.nchain.jcl.net.protocol.config.ProtocolConfig
import com.nchain.jcl.net.protocol.config.ProtocolConfigBuilder
import com.nchain.jcl.net.protocol.messages.*
import com.nchain.jcl.net.protocol.serialization.CompactBlockMsgSerializer
import com.nchain.jcl.net.protocol.serialization.TxInputMsgSerializer
import com.nchain.jcl.net.protocol.serialization.TxOutputMsgSerializer
import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext
import com.nchain.jcl.net.protocol.serialization.common.SerializerContext
import com.nchain.jcl.net.unit.protocol.tools.ByteArrayArtificalStreamProducer
import com.nchain.jcl.tools.bytes.ByteArrayReader
import com.nchain.jcl.tools.bytes.ByteArrayWriter
import io.bitcoinj.core.Utils
import io.bitcoinj.params.MainNetParams
import io.bitcoinj.params.Net
import spock.lang.Specification

class CompactBlockMsgSpec extends Specification {
    private static final String BLOCK_HEADER_BYTES = "0100000040f11b68435988807d64dff20261f7d9827825fbb37542601fb94d45000000005d0a2717cccfb28565e04baf2708f32068fb80f98765210ce6247b8939ab2012ecd9d24c1844011d00d36105"

    private static final String NONCE = "8DEC1AA458091500".toLowerCase()

    private static final String NUMBER_OF_SHORT_IDS_BYTES = "03"

    private static final List<String> SHORT_TX_IDS = Arrays.asList(
        "492C03224042".toLowerCase(), // 72843215973449
        "492E0102C0A2".toLowerCase(), // 178945551052361
        "696A05208032".toLowerCase() // 55525874428521
    )

    private static final String SHORT_TX_IDS_BYTES = SHORT_TX_IDS.join()

    private static final String NUMBER_OF_PREFILLED_TX_BYTES = "00"

    private static final String COMPACT_BLOCK_BYTES = BLOCK_HEADER_BYTES + NONCE + NUMBER_OF_SHORT_IDS_BYTES + SHORT_TX_IDS_BYTES + NUMBER_OF_PREFILLED_TX_BYTES

    def "Testing CompactBlockMsg Deserialize"(int byteInterval, int delayMs) {
        given:
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            DeserializerContext context = DeserializerContext.builder()
                .protocolBasicConfig(config.getBasicConfig())
                .build()

            byte[] bytes = Utils.HEX.decode(COMPACT_BLOCK_BYTES)

            CompactBlockMsgSerializer serializer = CompactBlockMsgSerializer.getInstance()
            CompactBlockMsg message

        when:
            ByteArrayReader byteReader = ByteArrayArtificalStreamProducer.stream(bytes, byteInterval, delayMs)
            message = serializer.deserialize(context, byteReader)

        then:
            message.messageType == CompactBlockMsg.MESSAGE_TYPE
            message.getNonce() == 5921250825923725
            message.getShortTxIds().length == 3
            message.getShortTxIds()[0] == 72843215973449
            message.getShortTxIds()[1] == 178945551052361
            message.getShortTxIds()[2] == 55525874428521

        where:
            byteInterval | delayMs
            10           | 5
    }

    def "Testing CompactBlockMsg Serializing"() {
        given:
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            SerializerContext context = SerializerContext.builder()
                .protocolBasicConfig(config.getBasicConfig())
                .build()

            CompactBlockMsgSerializer serializer = CompactBlockMsgSerializer.getInstance()

            BasicBlockHeaderMsg blockHeaderMsg = BasicBlockHeaderMsg.builder()
                .hash(HashMsg.builder().hash(Utils.HEX.decode("2f6428543c10f8b30e76f9519b597259e4839e0c91ed278a7eee27f8c014a525")).build())
                .version(1)
                .prevBlockHash(HashMsg.builder().hash(Utils.HEX.decode("40f11b68435988807d64dff20261f7d9827825fbb37542601fb94d4500000000")).build())
                .merkleRoot(HashMsg.builder().hash(Utils.HEX.decode("5d0a2717cccfb28565e04baf2708f32068fb80f98765210ce6247b8939ab2012")).build())
                .creationTimestamp(1288886764)
                .difficultyTarget(486622232)
                .nonce(90297088)
                .build()

            long[] shortTxIds = new long[3]
            shortTxIds[0] = 72843215973449;
            shortTxIds[1] = 178945551052361;
            shortTxIds[2] = 55525874428521;

            CompactBlockMsg compactBlockMsg = CompactBlockMsg.builder()
                .header(blockHeaderMsg)
                .nonce(5921250825923725)
                .shortTxIds(shortTxIds)
                .prefilledTransactions(new PrefilledTxMsg[0])
                .build()

            String messageSerializedBytes
        when:
            ByteArrayWriter byteWriter = new ByteArrayWriter()
            serializer.serialize(context, compactBlockMsg, byteWriter)
            messageSerializedBytes = Utils.HEX.encode(byteWriter.reader().getFullContent())
            byteWriter.reader()

        then:
            messageSerializedBytes == COMPACT_BLOCK_BYTES
    }
}
