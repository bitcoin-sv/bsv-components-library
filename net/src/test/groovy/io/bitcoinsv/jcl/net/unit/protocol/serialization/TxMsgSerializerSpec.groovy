/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.unit.protocol.serialization


import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader
import io.bitcoinsv.jcl.tools.bytes.ByteArrayWriter
import io.bitcoinj.core.Utils
import io.bitcoinj.params.MainNetParams
import io.bitcoinj.params.Net
import io.bitcoinsv.jcl.net.protocol.config.ProtocolConfig
import io.bitcoinsv.jcl.net.protocol.config.ProtocolConfigBuilder
import io.bitcoinsv.jcl.net.protocol.messages.TxInputMsg
import io.bitcoinsv.jcl.net.protocol.messages.TxMsg
import io.bitcoinsv.jcl.net.protocol.messages.TxOutputMsg
import io.bitcoinsv.jcl.net.protocol.messages.VersionMsg
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsg
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsgBuilder
import io.bitcoinsv.jcl.net.protocol.serialization.TxInputMsgSerializer
import io.bitcoinsv.jcl.net.protocol.serialization.TxMsgSerializer
import io.bitcoinsv.jcl.net.protocol.serialization.TxOutputMsgSerializer
import io.bitcoinsv.jcl.net.protocol.serialization.common.BitcoinMsgSerializer
import io.bitcoinsv.jcl.net.protocol.serialization.common.BitcoinMsgSerializerImpl
import io.bitcoinsv.jcl.net.protocol.serialization.common.DeserializerContext
import io.bitcoinsv.jcl.net.protocol.serialization.common.SerializerContext
import io.bitcoinsv.jcl.net.unit.protocol.tools.ByteArrayArtificalStreamProducer
import spock.lang.Specification

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 *
 * @date 07/10/2019
 *
 * Testing class for the TxOutput message Serialization.
 * The test is taken the assumption that we have already a correct serialization version of this Message, obtained
 * from another source that we trust (in this case the Java BitcoinJ library). So we serialize/deserialize some
 * messages with out code and compare the results with that reference.
 */
class TxMsgSerializerSpec extends Specification {

    // Body Message in Hex Format:
    public static final String REF_MSG ="0500000001bad09aa61d4fff3bba3fb8537dedd6db898996303ac2107060e430c16bb2208f010000000c6a0a00000000000000000000050000000105000000000000000c6a0a0000000000000000000005000000"

    // TxInput in HEx Format:
    public static final REF_INPUT = "bad09aa61d4fff3bba3fb8537dedd6db898996303ac2107060e430c16bb2208f010000000c6a0a0000000000000000000005000000"

    // TxOutput in Hex format
    public static final REF_OUTPUT = "05000000000000000c6a0a00000000000000000000"

    // Locktime
    public static final long REF_LOCKTIME = 5
    // Version
    public static final long REF_VERSION = 5

    // Complete message, including Header:
    public static final String REF_MSG_FULL = "e3e1f3e874780000000000000000000054000000b8cc7f1e" + REF_MSG

    def "Testing TransactionMsg Deserialize"(int byteInterval, int delayMs) {
        given:
        ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
        DeserializerContext context = DeserializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .build()
        TxMsgSerializer serializer = TxMsgSerializer.getInstance()
        TxMsg message
        when:
            ByteArrayReader byteReader = ByteArrayArtificalStreamProducer.stream(Utils.HEX.decode(REF_MSG), byteInterval, delayMs)
            message = serializer.deserialize(context, byteReader)
        then:

            message.getMessageType() == TxMsg.MESSAGE_TYPE
            message.getTx_in_count().value == 1
            message.getTx_in().size() == 1
            message.getTx_out_count().value == 1
            message.getTx_out().size() == 1
            message.version == REF_VERSION
            message.lockTime == REF_LOCKTIME
        where:
            byteInterval | delayMs
                10       |    15
    }

    def "Testing TxInputMessage Serializing"() {
        given:
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
        SerializerContext context = SerializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .build()
            TxMsgSerializer serializer = TxMsgSerializer.getInstance()

            // We build the inputs and outputs directly from the Hex representation:
            ByteArrayReader reader = new ByteArrayReader(Utils.HEX.decode(REF_INPUT))
        TxInputMsg txInput = TxInputMsgSerializer.getInstance().deserialize(null, reader)

            reader = new ByteArrayReader(Utils.HEX.decode(REF_OUTPUT))
        TxOutputMsg txOutput = TxOutputMsgSerializer.getInstance().deserialize(null, reader)

            // We build the TX:
            TxMsg transactionMsg = TxMsg.builder()
                    .version(REF_VERSION)
                    .lockTime(REF_LOCKTIME)
                    .tx_in(Arrays.asList(txInput))
                    .tx_out(Arrays.asList(txOutput))
                    .build()

            String messageSerializedBytes
        when:
                ByteArrayWriter byteWriter = new ByteArrayWriter()
                serializer.serialize(context, transactionMsg, byteWriter)
                messageSerializedBytes =  Utils.HEX.encode(byteWriter.reader().getFullContent())
                byteWriter.reader()
        then:
             messageSerializedBytes == REF_MSG
    }

    def "testing TransactionMsg COMPLETE de-serializing"(int byteInterval, int delayMs) {
        given:
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            DeserializerContext context = DeserializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .build()
            ByteArrayReader byteReader = ByteArrayArtificalStreamProducer.stream(Utils.HEX.decode(REF_MSG_FULL), byteInterval, delayMs)
        BitcoinMsgSerializer bitcoinSerializer = new BitcoinMsgSerializerImpl()
        when:
        BitcoinMsg<TxMsg> message = bitcoinSerializer.deserialize(context, byteReader, TxMsg.MESSAGE_TYPE)

        then:
            message.getHeader().getMagic().equals(config.getBasicConfig().getMagicPackage())
            message.getHeader().getCommand().toUpperCase().equals(TxMsg.MESSAGE_TYPE.toUpperCase())
            message.getBody().getMessageType() == TxMsg.MESSAGE_TYPE

            message.getBody().getTx_in_count().value == 1
            message.getBody().getTx_in().size() == 1
            message.getBody().getTx_out_count().value == 1
            message.getBody().getTx_out().size() == 1
            message.getBody().version == REF_VERSION
            message.getBody().lockTime == REF_LOCKTIME

        where:
            byteInterval | delayMs
                50       |    3
    }

    def "testing Version Message COMPLETE Serializing"() {
        given:
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            SerializerContext context = SerializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .build()

            // We build the inputs and outputs directly from the Hex representation:
            ByteArrayReader reader = new ByteArrayReader(Utils.HEX.decode(REF_INPUT))
            TxInputMsg txInput = TxInputMsgSerializer.getInstance().deserialize(null, reader)

            reader = new ByteArrayReader(Utils.HEX.decode(REF_OUTPUT))
            TxOutputMsg txOutput = TxOutputMsgSerializer.getInstance().deserialize(null, reader)

            // We build the whole Tx Message
            TxMsg transactionMsg = TxMsg.builder()
                    .version(REF_VERSION)
                    .lockTime(REF_LOCKTIME)
                    .tx_in(Arrays.asList(txInput))
                    .tx_out(Arrays.asList(txOutput))
                    .build()

            BitcoinMsg<VersionMsg> bitcoinVersionMsg = new BitcoinMsgBuilder<>(config.getBasicConfig(), transactionMsg).build()
            BitcoinMsgSerializer bitcoinSerializer = new BitcoinMsgSerializerImpl()
        when:
            byte[] msgBytes = bitcoinSerializer.serialize(context, bitcoinVersionMsg, TxMsg.MESSAGE_TYPE).getFullContent()
            String msgDeserialized = Utils.HEX.encode(msgBytes)
        then:
            msgDeserialized.equals(REF_MSG_FULL)
    }
}
