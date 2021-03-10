package com.nchain.jcl.net.unit.protocol.serialization

import com.nchain.jcl.net.protocol.config.ProtocolConfig
import com.nchain.jcl.net.protocol.config.ProtocolConfigBuilder
import com.nchain.jcl.net.protocol.messages.RawTxMsg
import com.nchain.jcl.net.protocol.messages.TxInputMsg
import com.nchain.jcl.net.protocol.messages.TxMsg
import com.nchain.jcl.net.protocol.messages.TxOutputMsg
import com.nchain.jcl.net.protocol.messages.VersionMsg
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsgBuilder
import com.nchain.jcl.net.protocol.serialization.RawTxMsgSerializer
import com.nchain.jcl.net.protocol.serialization.TxInputMsgSerializer
import com.nchain.jcl.net.protocol.serialization.TxMsgSerializer
import com.nchain.jcl.net.protocol.serialization.TxOutputMsgSerializer
import com.nchain.jcl.net.protocol.serialization.common.BitcoinMsgSerializer
import com.nchain.jcl.net.protocol.serialization.common.BitcoinMsgSerializerImpl
import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext
import com.nchain.jcl.net.protocol.serialization.common.SerializerContext
import com.nchain.jcl.net.unit.protocol.tools.ByteArrayArtificalStreamProducer
import com.nchain.jcl.tools.bytes.ByteArrayReader
import com.nchain.jcl.tools.bytes.ByteArrayWriter
import io.bitcoinj.core.Utils
import io.bitcoinj.params.MainNetParams
import io.bitcoinj.params.Net
import spock.lang.Specification

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 *
 * @date 07/10/2019
 *
 * Testing class for the RawTx message Serialization.
 * The test is taken the assumption that we have already a correct serialization version of this Message, obtained
 * from another source that we trust (in this case the Java BitcoinJ library). So we serialize/deserialize some
 * messages with out code and compare the results with that reference.
 */
class RawTxMsgSerializerSpec extends Specification {

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
                    .maxBytesToRead(Utils.HEX.decode(REF_MSG).length)
                    .build()
            RawTxMsgSerializer serializer = RawTxMsgSerializer.getInstance()
            RawTxMsg message
        when:
            ByteArrayReader byteReader = ByteArrayArtificalStreamProducer.stream(Utils.HEX.decode(REF_MSG), byteInterval, delayMs)
            message = serializer.deserialize(context, byteReader)
        then:
            message.getMessageType() == RawTxMsg.MESSAGE_TYPE
            message.content == Utils.HEX.decode(REF_MSG)
        where:
            byteInterval | delayMs
                10       |    15
    }
}
