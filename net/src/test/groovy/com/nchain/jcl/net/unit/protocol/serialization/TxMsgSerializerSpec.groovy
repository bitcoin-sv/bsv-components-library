package com.nchain.jcl.net.unit.protocol.serialization


import com.nchain.jcl.net.protocol.config.ProtocolConfig
import com.nchain.jcl.net.protocol.config.ProtocolConfigBuilder
import com.nchain.jcl.net.protocol.config.provided.ProtocolBSVMainConfig
import com.nchain.jcl.net.protocol.messages.HashMsg
import com.nchain.jcl.net.protocol.messages.TxMsg
import com.nchain.jcl.net.protocol.messages.TxInputMsg
import com.nchain.jcl.net.protocol.messages.TxOutPointMsg
import com.nchain.jcl.net.protocol.messages.TxOutputMsg
import com.nchain.jcl.net.protocol.messages.VersionMsg
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsgBuilder
import com.nchain.jcl.net.protocol.serialization.TxMsgSerializer
import com.nchain.jcl.net.protocol.serialization.common.BitcoinMsgSerializer
import com.nchain.jcl.net.protocol.serialization.common.BitcoinMsgSerializerImpl
import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext
import com.nchain.jcl.net.protocol.serialization.common.MsgSerializersFactory
import com.nchain.jcl.net.protocol.serialization.common.SerializerContext
import com.nchain.jcl.net.unit.protocol.tools.ByteArrayArtificalStreamProducer
import com.nchain.jcl.tools.bytes.ByteArrayReader
import com.nchain.jcl.tools.bytes.ByteArrayWriter
import io.bitcoinj.bitcoin.api.base.Tx
import io.bitcoinj.bitcoin.bean.base.FullBlockBean
import io.bitcoinj.bitcoin.bean.base.TxBean
import io.bitcoinj.core.Utils
import io.bitcoinj.params.MainNetParams
import io.bitcoinj.params.Net
import org.spongycastle.crypto.tls.DefaultTlsServer
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
    public static final String REF_MSG ="010000000193e3073ecc1d27f17e3d287ccefdfdba5f7d8c160242dbcd547b18baef12f9b31a0000006b483045022100af501dc9ef2907247d28a5169b8362ca49" +
            "4e1993f833928b77264e604329eec40220313594f38f97c255bcea6d5a4a68e920508ef93fd788bcf5b0ad2fa5d34940180121034bb555cc39ba30561793cf39a35c403fe8cf4a89403b02b51e058960520" +
            "bd1e3ffffffff02b3bb0200000000001976a914f7d52018971f4ab9b56f0036958f84ae0325ccdc88ac98100700000000001976a914f230f0a16a98433eca0fa70487b85fb83f7b61cd88ac00000000"
    public static final byte[] signature_script  = Utils.HEX.decode("483045022100af501dc9ef2907247d28a5169b8362ca494e1993f833928b77264e604329eec40220313594f38f97c255bcea6d5a4a68e920508ef93fd788bcf5b0ad2fa5d34940180121034bb555cc39ba30561793cf39a35c403fe8cf4a89403b02b51e058960520bd1e3")
    public static final byte[] pk_script_one = Utils.HEX.decode("76a914f7d52018971f4ab9b56f0036958f84ae0325ccdc88ac")
    public static final byte[] pk_script_two = Utils.HEX.decode("76a914f230f0a16a98433eca0fa70487b85fb83f7b61cd88ac")
    public static final byte[] outpoint_bytes = Utils.HEX.decode("b3f912efba187b54cddb4202168c7d5fbafdfdce7c283d7ef1271dcc3e07e393")

    public static final String REF_MSG_FULL ="e3e1f3e8747800000000000000000000e20000001c9c6bb4010000000193e3073ecc1d27f17e3d287ccefdfdba5f7d8c160242dbcd547b18baef12f9b31a0000006" +
            "b483045022100af501dc9ef2907247d28a5169b8362ca494e1993f833928b77264e604329eec40220313594f38f97c255bcea6d5a4a68e920508ef93fd788bcf5b0ad2fa5d34940180121034bb555cc39ba30561793cf39a35c" +
            "403fe8cf4a89403b" +
            "02b51e058960520bd1e3ffffffff02b3bb0200000000001976a914f7d52018971f4ab9b56f0036958f84ae0325ccdc88ac98100700000000001976a914f230f0a16a98433eca0fa70487b85fb83f7b61cd88ac00000000"

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
            message.tx_in.get(0).pre_outpoint.hash.hashBytes == outpoint_bytes
            message.tx_in.get(0).pre_outpoint.index == 26
            message.tx_in.get(0).signature_script == signature_script
            message.tx_in.get(0).sequence == 4294967295
            message.getTx_out_count().value == 2

            message.tx_out.get(0).txValue == 179123
            message.tx_out.get(0).pk_script== pk_script_one

            message.tx_out.get(1).txValue == 463000
            message.tx_out.get(1).pk_script== pk_script_two
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

            TxOutputMsg txOutputMessageOne = TxOutputMsg.builder().txValue(179123).pk_script(pk_script_one).build();
            TxOutputMsg txOutputMessageTwo  =TxOutputMsg.builder().txValue(463000).pk_script(pk_script_two).build();
            List<TxOutputMsg> txOutputMsgs = new ArrayList<>();
            txOutputMsgs.add(txOutputMessageOne);
            txOutputMsgs.add(txOutputMessageTwo);
            HashMsg hash = HashMsg.builder().hash(outpoint_bytes).build();

            TxOutPointMsg txOutPointMsg =TxOutPointMsg.builder().index(26).hash(hash).build();
            TxInputMsg txInputMessage = TxInputMsg.builder().pre_outpoint(txOutPointMsg)
                    .sequence(4294967295)
                    .signature_script(signature_script).build()
            List<TxInputMsg> txInputs = new ArrayList<>();
            txInputs.add(txInputMessage)

            TxMsg transactionMsg = TxMsg.builder().version(1).tx_in(txInputs).tx_out(txOutputMsgs).build()
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
            message.getBody().tx_in.get(0).pre_outpoint.hash.hashBytes == outpoint_bytes
            message.getBody().tx_in.get(0).pre_outpoint.index == 26
            message.getBody().tx_in.get(0).signature_script == signature_script
            message.getBody().tx_in.get(0).sequence == 4294967295
            message.getBody().getTx_out_count().value == 2

            message.getBody().tx_out.get(0).txValue == 179123
            message.getBody().tx_out.get(0).pk_script== pk_script_one

            message.getBody().tx_out.get(1).txValue == 463000
            message.getBody().tx_out.get(1).pk_script== pk_script_two
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

            TxOutputMsg txOutputMessageOne = TxOutputMsg.builder().txValue(179123).pk_script(pk_script_one).build();
            TxOutputMsg txOutputMessageTwo  = TxOutputMsg.builder().txValue(463000).pk_script(pk_script_two).build();
            List<TxOutputMsg> txOutputMsgs = new ArrayList<>();
            txOutputMsgs.add(txOutputMessageOne);
            txOutputMsgs.add(txOutputMessageTwo);
            HashMsg hash = HashMsg.builder().hash(outpoint_bytes).build();

            TxOutPointMsg txOutPointMsg =TxOutPointMsg.builder().index(26).hash(hash).build();


            TxInputMsg txInputMessage =TxInputMsg.builder().pre_outpoint(txOutPointMsg)
                    .sequence(4294967295)
                    .signature_script(signature_script).build()
            List<TxInputMsg> txInputs = new ArrayList<>();
            txInputs.add(txInputMessage)


            TxMsg transactionMsg = TxMsg.builder().version(1).tx_in(txInputs).tx_out(txOutputMsgs).build()

            BitcoinMsg<VersionMsg> bitcoinVersionMsg = new BitcoinMsgBuilder<>(config.getBasicConfig(), transactionMsg).build()
            BitcoinMsgSerializer bitcoinSerializer = new BitcoinMsgSerializerImpl()
        when:
            byte[] msgBytes = bitcoinSerializer.serialize(context, bitcoinVersionMsg, TxMsg.MESSAGE_TYPE).getFullContent()
            String msgDeserialized = Utils.HEX.encode(msgBytes)
        then:
            msgDeserialized.equals(REF_MSG_FULL)
    }
}
