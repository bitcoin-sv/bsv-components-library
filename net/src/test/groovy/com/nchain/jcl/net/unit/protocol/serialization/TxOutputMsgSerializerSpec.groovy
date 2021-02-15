package com.nchain.jcl.net.unit.protocol.serialization

import com.nchain.jcl.net.protocol.config.ProtocolConfig
import com.nchain.jcl.net.protocol.config.ProtocolConfigBuilder
import com.nchain.jcl.net.protocol.config.provided.ProtocolBSVMainConfig
import com.nchain.jcl.net.protocol.messages.TxOutputMsg
import com.nchain.jcl.net.protocol.serialization.TxOutputMsgSerializer
import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext
import com.nchain.jcl.net.protocol.serialization.common.SerializerContext
import com.nchain.jcl.net.unit.protocol.tools.ByteArrayArtificalStreamProducer
import com.nchain.jcl.tools.bytes.ByteArrayReader
import com.nchain.jcl.tools.bytes.ByteArrayWriter
import io.bitcoinj.core.Sha256Hash
import io.bitcoinj.core.Utils
import io.bitcoinj.params.MainNetParams
import io.bitcoinj.params.Net
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

    public static final String REF_MSG = "00f2052a01000000202b801dd82f01d17bbde881687bf72bc62e2faa8ab8133d36fcb8c3abe7459da6"

    public static final byte[] REF_BITES = Sha256Hash.wrap("2b801dd82f01d17bbde881687bf72bc62e2faa8ab8133d36fcb8c3abe7459da6").getBytes();
    //2b801dd82f01d17bbde881687bf72bc62e2faa8ab8133d36fcb8c3abe7459da6
    public static final long COIN_VALUE = 5000000000

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
            message.txValue == COIN_VALUE
            message.pk_script == REF_BITES
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
            TxOutputMsg txOutputMessage = TxOutputMsg.builder().txValue(COIN_VALUE).pk_script(REF_BITES).build();

            String messageSerializedBytes
        when:
            ByteArrayWriter byteWriter = new ByteArrayWriter()
            serializer.serialize(context, txOutputMessage, byteWriter)
            messageSerializedBytes =  Utils.HEX.encode(byteWriter.reader().getFullContent())
        then:
            messageSerializedBytes == REF_MSG
    }

}
