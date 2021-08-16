/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.unit.protocol.serialization


import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader
import io.bitcoinsv.jcl.tools.bytes.ByteArrayWriter
import io.bitcoinj.core.Coin
import io.bitcoinj.core.Utils
import io.bitcoinj.params.MainNetParams
import io.bitcoinj.params.Net
import io.bitcoinj.script.ScriptBuilder
import io.bitcoinsv.jcl.net.protocol.config.ProtocolConfig
import io.bitcoinsv.jcl.net.protocol.config.ProtocolConfigBuilder
import io.bitcoinsv.jcl.net.protocol.messages.TxOutputMsg
import io.bitcoinsv.jcl.net.protocol.serialization.TxOutputMsgSerializer
import io.bitcoinsv.jcl.net.protocol.serialization.common.DeserializerContext
import io.bitcoinsv.jcl.net.protocol.serialization.common.SerializerContext
import io.bitcoinsv.jcl.net.unit.protocol.tools.ByteArrayArtificalStreamProducer
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
        ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
        DeserializerContext context = DeserializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .build()
        TxOutputMsgSerializer serializer = TxOutputMsgSerializer.getInstance()
        TxOutputMsg message
            ByteArrayReader byteReader = ByteArrayArtificalStreamProducer.stream(Utils.HEX.decode(REF_MSG), byteInterval, delayMs)
        when:
            message = serializer.deserialize(context, byteReader)
        then:

            message.getMessageType() == TxOutputMsg.MESSAGE_TYPE
            message.txValue == REF_VALUE.value
            message.pk_script == REF_SCRIPT
        where:
            byteInterval | delayMs
                10       |    50

    }


    def "Testing TxOutputMessage Serializing"() {
        given:
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
        SerializerContext context = SerializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .build()
            TxOutputMsgSerializer serializer = TxOutputMsgSerializer.getInstance()
            TxOutputMsg txOutputMessage = TxOutputMsg.builder().txValue(REF_VALUE.value).pk_script(REF_SCRIPT).build();

            String messageSerializedBytes
        when:
            ByteArrayWriter byteWriter = new ByteArrayWriter()
            serializer.serialize(context, txOutputMessage, byteWriter)
            messageSerializedBytes =  Utils.HEX.encode(byteWriter.reader().getFullContent())
        then:
            messageSerializedBytes == REF_MSG
    }

}
