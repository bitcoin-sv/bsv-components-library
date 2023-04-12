package io.bitcoinsv.bsvcl.net.protocol.serialization

import io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig
import io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder
import io.bitcoinsv.bsvcl.net.protocol.messages.TxInputMsg
import io.bitcoinsv.bsvcl.net.protocol.messages.TxOutPointMsg
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext
import io.bitcoinsv.bsvcl.net.protocol.tools.ByteArrayArtificalStreamProducer
import io.bitcoinsv.bsvcl.common.bytes.ByteArrayReader
import io.bitcoinsv.bsvcl.common.bytes.ByteArrayReaderRealTime
import io.bitcoinsv.bsvcl.common.bytes.ByteArrayWriter
import io.bitcoinsv.bitcoinjsv.core.Coin
import io.bitcoinsv.bitcoinjsv.core.Utils
import io.bitcoinsv.bitcoinjsv.params.MainNetParams
import io.bitcoinsv.bitcoinjsv.params.Net
import io.bitcoinsv.bitcoinjsv.script.ScriptBuilder
import spock.lang.Specification

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 26/09/2019
 *
 * Testing class for the TxInput message Serialization.
 * The test is taken the assumption that we have already a correct serialization version of this Message, obtained
 * from another source that we trust (in this case the Java BitcoinJ library). So we serialize/deserialize some
 * messages with out code and compare the results with that reference.
 */
class TxInputMsgSerializerSpec extends Specification {

    // Whole Mesage in HEx format
    public static final String REF_FULL_MSG = "bad09aa61d4fff3bba3fb8537dedd6db898996303ac2107060e430c16bb2208f010000000c6a0a0000000000000000000005000000"

    // TxOutpont in Hex Format:
    public static final String REF_OUTPOINT = "bad09aa61d4fff3bba3fb8537dedd6db898996303ac2107060e430c16bb2208f01000000"

    // Script used (empty array of 10 bytes)
    public static final byte[] REF_SCRIPT_BYTES = ScriptBuilder.createOpReturnScript(new byte[10]).program
    // Sequence number
    public static final long REF_SEQUENCE = 5
    // Value
    public static final Coin REF_VALUE = Coin.valueOf(5)



    def "Testing TxInputMessage Deserializing"(int byteInterval, int delayMs) {
        given:
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            DeserializerContext context = DeserializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .build()
            TxInputMsgSerializer serializer = TxInputMsgSerializer.getInstance()
            TxInputMsg message
        when:
            ByteArrayReader byteReader = ByteArrayArtificalStreamProducer.stream(Utils.HEX.decode(REF_FULL_MSG), byteInterval, delayMs)
           message = serializer.deserialize(context, new ByteArrayReaderRealTime(byteReader))
        then:
           message.getSequence()  == REF_SEQUENCE
           message.getSignature_script() == REF_SCRIPT_BYTES
           message.getScript_length().value == REF_SCRIPT_BYTES.length
           message.getMessageType() == TxInputMsg.MESSAGE_TYPE
        where:
            byteInterval | delayMs
                10       |    25
    }


    def "Testing TxInputMessage Serializing"() {
        given:
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            SerializerContext context = SerializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .build()
            TxInputMsgSerializer serializer = TxInputMsgSerializer.getInstance()

            // We build our TxInput Object:

            // We build the TxOutpoint directly by deserializing it:
            ByteArrayReader reader = new ByteArrayReader(Utils.HEX.decode(REF_OUTPOINT))
            TxOutPointMsg txOutPointMessage =  TxOutPointMsgSerializer.getInstance().deserialize(null, reader)

            // We assign the rest of variables:
            TxInputMsg txInputMessage = TxInputMsg.builder().pre_outpoint(txOutPointMessage)
                .sequence(REF_SEQUENCE)
                .signature_script(REF_SCRIPT_BYTES)
                .pre_outpoint(txOutPointMessage)
                .build()

        when:
            ByteArrayWriter byteWriter = new ByteArrayWriter()
            serializer.serialize(context, txInputMessage, byteWriter)
            String messageSerializedBytes =  Utils.HEX.encode(byteWriter.reader().getFullContent())
        then:
             messageSerializedBytes == REF_FULL_MSG
    }
}
