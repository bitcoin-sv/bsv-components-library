package com.nchain.jcl.net.unit.protocol.serialization

import com.nchain.jcl.net.protocol.config.ProtocolConfig
import com.nchain.jcl.net.protocol.config.ProtocolConfigBuilder
import com.nchain.jcl.net.protocol.messages.CompactBlockMsg
import com.nchain.jcl.net.protocol.serialization.CompactBlockMsgSerializer
import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext
import com.nchain.jcl.net.unit.protocol.tools.ByteArrayArtificalStreamProducer
import com.nchain.jcl.tools.bytes.ByteArrayReader
import io.bitcoinj.core.Utils
import io.bitcoinj.params.MainNetParams
import io.bitcoinj.params.Net
import spock.lang.Specification

class CompactBlockMsgSpec extends Specification {
    private static final String BLOCK_HEADER_BYTES = "0100000040f11b68435988807d64dff20261f7d9827825fbb37542601fb94d45000000005d0a2717cccfb28565e04baf2708f32068fb80f98765210ce6247b8939ab2012ecd9d24c1844011d00d36105"

    private static final String NONCE = "008DEC1AA4580915".toLowerCase()

    private static final String NUMBER_OF_SHORT_IDS_BYTES = "01"

    private static final List<String> SHORT_TX_IDS = Arrays.asList(
        "424022032C49".toLowerCase(),
        "A2C002012E49".toLowerCase(),
        "328020056A69".toLowerCase()
    )

    private static final String SHORT_TX_IDS_BYTES = SHORT_TX_IDS.join()

    private static final String NUMBER_OF_PREFILLED_TX_BYTES = "00"

    private static final String COMPACT_BLOCK_BYTES = BLOCK_HEADER_BYTES + NONCE + NUMBER_OF_SHORT_IDS_BYTES + SHORT_TX_IDS_BYTES + NUMBER_OF_PREFILLED_TX_BYTES

    def "Testing PrefilledTransactionMsg Deserialize"(int byteInterval, int delayMs) {
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
            message.getNonce() == 5921250825923725740

        where:
            byteInterval | delayMs
            10           | 5
    }
}
