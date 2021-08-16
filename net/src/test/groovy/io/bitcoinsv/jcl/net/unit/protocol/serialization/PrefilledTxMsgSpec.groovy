package io.bitcoinsv.jcl.net.unit.protocol.serialization


import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader
import io.bitcoinsv.jcl.tools.bytes.ByteArrayWriter
import io.bitcoinj.core.Utils
import io.bitcoinj.params.MainNetParams
import io.bitcoinj.params.Net
import io.bitcoinsv.jcl.net.protocol.config.ProtocolConfig
import io.bitcoinsv.jcl.net.protocol.config.ProtocolConfigBuilder
import io.bitcoinsv.jcl.net.protocol.messages.PrefilledTxMsg
import io.bitcoinsv.jcl.net.protocol.messages.TxInputMsg
import io.bitcoinsv.jcl.net.protocol.messages.TxMsg
import io.bitcoinsv.jcl.net.protocol.messages.TxOutputMsg
import io.bitcoinsv.jcl.net.protocol.messages.VarIntMsg
import io.bitcoinsv.jcl.net.protocol.serialization.PrefilledTxMsgSerializer
import io.bitcoinsv.jcl.net.protocol.serialization.TxInputMsgSerializer
import io.bitcoinsv.jcl.net.protocol.serialization.TxOutputMsgSerializer
import io.bitcoinsv.jcl.net.protocol.serialization.common.DeserializerContext
import io.bitcoinsv.jcl.net.protocol.serialization.common.SerializerContext
import io.bitcoinsv.jcl.net.unit.protocol.tools.ByteArrayArtificalStreamProducer
import spock.lang.Specification

class PrefilledTxMsgSpec extends Specification {
    // index
    public static final INDEX = 0xFA

    // Prefilled transaction input with index 0xFA
    public static final PREFILLED_TRANSACTION_BYTES = "fa0500000001bad09aa61d4fff3bba3fb8537dedd6db898996303ac2107060e430c16bb2208f010000000c6a0a00000000000000000000050000000105000000000000000c6a0a0000000000000000000005000000"

    // TxInput in Hex Format:
    public static final REF_INPUT = "bad09aa61d4fff3bba3fb8537dedd6db898996303ac2107060e430c16bb2208f010000000c6a0a0000000000000000000005000000"

    // TxOutput in Hex format
    public static final REF_OUTPUT = "05000000000000000c6a0a00000000000000000000"

    // Locktime
    public static final long REF_LOCKTIME = 5

    // Version
    public static final long REF_VERSION = 5

    def "Testing PrefilledTransactionMsg Deserialize"(int byteInterval, int delayMs) {
        given:
        ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
        DeserializerContext context = DeserializerContext.builder()
                .protocolBasicConfig(config.getBasicConfig())
                .build()

            byte[] prefilledTxBytes = Utils.HEX.decode(PREFILLED_TRANSACTION_BYTES)

        PrefilledTxMsgSerializer serializer = PrefilledTxMsgSerializer.getInstance()
        PrefilledTxMsg message

        when:
            ByteArrayReader byteReader = ByteArrayArtificalStreamProducer.stream(prefilledTxBytes, byteInterval, delayMs)
            message = serializer.deserialize(context, byteReader)

        then:
            message.getMessageType() == PrefilledTxMsg.MESSAGE_TYPE
            message.getIndex().value == INDEX
            message.getTransaction().getTx_in_count().value == 1
            message.getTransaction().getTx_in().size() == 1
            message.getTransaction().getTx_out_count().value == 1
            message.getTransaction().getTx_out().size() == 1
            message.getTransaction().version == REF_VERSION
            message.getTransaction().lockTime == REF_LOCKTIME

        where:
            byteInterval | delayMs
            10           | 5
    }

    def "Testing PrefilledTransactionInputMessage Serializing"() {
        given:
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
        SerializerContext context = SerializerContext.builder()
                .protocolBasicConfig(config.getBasicConfig())
                .build()

            PrefilledTxMsgSerializer serializer = PrefilledTxMsgSerializer.getInstance()

            ByteArrayReader reader = new ByteArrayReader(Utils.HEX.decode(REF_INPUT))
        TxInputMsg txInput = TxInputMsgSerializer.getInstance().deserialize(null, reader)

            reader = new ByteArrayReader(Utils.HEX.decode(REF_OUTPUT))
        TxOutputMsg txOutput = TxOutputMsgSerializer.getInstance().deserialize(null, reader)

        TxMsg transactionMsg = TxMsg.builder()
                .version(REF_VERSION)
                .lockTime(REF_LOCKTIME)
                .tx_in(Arrays.asList(txInput))
                .tx_out(Arrays.asList(txOutput))
                .build()

            PrefilledTxMsg prefilledTransactionMsg = PrefilledTxMsg.builder()
                .index(VarIntMsg.builder().value(INDEX).build())
                .transaction(transactionMsg)
                .build()

            String messageSerializedBytes
        when:
            ByteArrayWriter byteWriter = new ByteArrayWriter()
            serializer.serialize(context, prefilledTransactionMsg, byteWriter)
            messageSerializedBytes = Utils.HEX.encode(byteWriter.reader().getFullContent())
            byteWriter.reader()

        then:
            messageSerializedBytes == PREFILLED_TRANSACTION_BYTES
    }
}
