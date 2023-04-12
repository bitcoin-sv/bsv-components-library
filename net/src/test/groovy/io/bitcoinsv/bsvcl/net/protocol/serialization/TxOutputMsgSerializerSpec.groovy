package io.bitcoinsv.bsvcl.net.protocol.serialization


import io.bitcoinsv.bsvcl.common.bytes.ByteArrayReader
import io.bitcoinsv.bsvcl.common.bytes.ByteArrayWriter
import io.bitcoinsv.bitcoinjsv.core.Coin
import io.bitcoinsv.bitcoinjsv.core.Utils
import io.bitcoinsv.bitcoinjsv.params.MainNetParams
import io.bitcoinsv.bitcoinjsv.params.Net
import io.bitcoinsv.bitcoinjsv.script.ScriptBuilder
import io.bitcoinsv.bsvcl.net.protocol.tools.ByteArrayArtificalStreamProducer
import spock.lang.Specification

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 01/10/2019
 *
 * Testing class for the TxOutput message Serialization.
 * The test is taken the assumption that we have already a correct serialization version of this Message, obtained
 * from another source that we trust (in this case the Java BitcoinJ library). So we serialize/deserialize some
 * messages with out code and compare the results with that reference.
 */
class TxOutputMsgSerializerSpec extends Specification {

    // Whole Message in Hex format
    public static final String REF_MSG = "05000000000000000c6a0a00000000000000000000"

    // SCrit used
    public static final byte[] REF_SCRIPT = ScriptBuilder.createOpReturnScript(new byte[10]).program
    // Value used
    public static final Coin REF_VALUE = Coin.valueOf(5)

    def "Testing TxOutputMessage Deserializing"(int byteInterval, int delayMs) {
        given:
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig config = io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext context = io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .build()
            io.bitcoinsv.bsvcl.net.protocol.serialization.TxOutputMsgSerializer serializer = io.bitcoinsv.bsvcl.net.protocol.serialization.TxOutputMsgSerializer.getInstance()
            io.bitcoinsv.bsvcl.net.protocol.messages.TxOutputMsg message
            ByteArrayReader byteReader = ByteArrayArtificalStreamProducer.stream(Utils.HEX.decode(REF_MSG), byteInterval, delayMs)
        when:
            message = serializer.deserialize(context, byteReader)
        then:

            message.getMessageType() == io.bitcoinsv.bsvcl.net.protocol.messages.TxOutputMsg.MESSAGE_TYPE
            message.txValue == REF_VALUE.value
            message.pk_script == REF_SCRIPT
        where:
            byteInterval | delayMs
                10       |    50

    }


    def "Testing TxOutputMessage Serializing"() {
        given:
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig config = io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext context = io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .build()
            io.bitcoinsv.bsvcl.net.protocol.serialization.TxOutputMsgSerializer serializer = io.bitcoinsv.bsvcl.net.protocol.serialization.TxOutputMsgSerializer.getInstance()
            io.bitcoinsv.bsvcl.net.protocol.messages.TxOutputMsg txOutputMessage = io.bitcoinsv.bsvcl.net.protocol.messages.TxOutputMsg.builder().txValue(REF_VALUE.value).pk_script(REF_SCRIPT).build();

            String messageSerializedBytes
        when:
            ByteArrayWriter byteWriter = new ByteArrayWriter()
            serializer.serialize(context, txOutputMessage, byteWriter)
            messageSerializedBytes =  Utils.HEX.encode(byteWriter.reader().getFullContent())
        then:
            messageSerializedBytes == REF_MSG
    }

}
