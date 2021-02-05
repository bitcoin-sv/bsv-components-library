package com.nchain.jcl.net.unit.protocol.serialization


import com.nchain.jcl.net.protocol.config.ProtocolConfig
import com.nchain.jcl.net.protocol.config.provided.ProtocolBSVMainConfig
import com.nchain.jcl.net.protocol.messages.*
import com.nchain.jcl.net.protocol.serialization.BlockHeaderEnMsgSerializer
import com.nchain.jcl.net.protocol.serialization.common.SerializerContext
import com.nchain.jcl.tools.bytes.ByteArrayWriter
import io.bitcoinj.core.Sha256Hash
import io.bitcoinj.core.Utils
import spock.lang.Ignore
import spock.lang.Specification
/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 */
class BlockHeaderEnMsgSpec extends Specification {
    private static final String BLOCK_BYTES = "0100000040f11b68435988807d64dff20261f7d9827825fbb37542601fb94d450" +
            "00000005d0a2717cccfb28565e04baf2708f32068fb80f98765210ce6247b8939ab2012ecd9d24c1844011d00d361050201" +
            "0000000100000000000000000000000000000000000000000000000000000000000000001a00000007041844011d0142fffffff" +
            "f0100f2052a010000004104a313febd5f91b6a13bd9c5317030518fee96d1319a0eb10076917294933d09c17dc1588a06953a26" +
            "4738f2acea0c66b99e796caa4f28158e0dd5f6fed69a185b000000000100000001aa18a952c3f73e5d7440bc570b2aa78f72059887b" +
            "25b6a1790514b7feedec090000000004104ac44bdf511477465cb70fef1d06b9241e74d26047ccbdfa641ec9a0115ad35594cbb58" +
            "a61a6fd56893a405bcffbf6555995ddedc7e6cd4e5ceb83a37e1cf8f98ffffffff02004d92d86a00000014b8083945473bc8289ef" +
            "b681f94de7b07a5b851ad00743ba40b00000014ef01911c9efec6799d1ee5f7c6fb072d9669da8000000000"

    public static final byte[] PREV_BLOCK_HASH = Sha256Hash.wrap("00000000454db91f604275b3fb257882d9f76" +
            "102f2df647d80885943681bf140").bytes
    public static final byte[] MERKLE_ROOT = Sha256Hash.wrap("1220ab39897b24e60c216587f980fb6820" +
            "f30827af4be06585b2cfcc17270a5d").bytes
    public static final byte[] TR1_PK_SCRIPT_ONE = Utils.HEX.decode("04a313febd5f91b6a13bd9c5317030518fee96d1319" +
                                                                 "a0eb10076917294933d09c17dc1588a06953a264738f2acea0c66b99e796caa4f28158e0dd5f6fed69a185b")

    public static final byte[] TR2_PK_SCRIPT_ONE = Utils.HEX.decode("b8083945473bc8289efb681f94de7b07a5b851ad")
    public static final byte[] TR2_PK_SCRIPT_TWO = Utils.HEX.decode("ef01911c9efec6799d1ee5f7c6fb072d9669da80")

  @Ignore  def "testing blockMessage BlockHeaderEnrichedMsg Serializing"() {
        given:
        ProtocolConfig config = new ProtocolBSVMainConfig()
        SerializerContext context = SerializerContext.builder()
                .protocolBasicConfig(config.getBasicConfig())
                .build()
        BlockHeaderEnMsgSerializer serializer = BlockHeaderEnMsgSerializer.getInstance()
        List<HashMsg> transactionMsgList = new ArrayList<>()
        Optional<HashMsg> transactionMsg1 = makeCoinbaseTransaction().getHash()
        Optional<HashMsg> transactionMsg2 = makeTransaction().getHash()

        transactionMsgList.add(transactionMsg1.get())
        transactionMsgList.add(transactionMsg2.get())

        BlockHeaderEnMsg blockHeaderEn = BlockHeaderEnMsg.builder()
                .version(1)
                .prevBlockHash(HashMsg.builder().hash(PREV_BLOCK_HASH).build())
                .merkleRoot(HashMsg.builder().hash(MERKLE_ROOT).build())
                .creationTimestamp(1288886764)
                .nBits(486622232)
                .nonce(90297088)
                .transactionCount(transactionMsgList.size())
                .hasCoinbaseData(true)
                .noMoreHeaders(false)
                .coinbaseMerkleProof(transactionMsgList)
                .coinbaseTX(makeCoinbaseTransaction())
                .build();
        String blockHeaderEnSerializedBytes

        when:
        ByteArrayWriter byteWriter = new ByteArrayWriter()
        serializer.serialize(context, blockHeaderEn, byteWriter)
        blockHeaderEnSerializedBytes = HEX.encode(byteWriter.reader().getFullContent())

        then:
        blockHeaderEnSerializedBytes == BLOCK_BYTES
    }

    TxMsg makeTransaction() {

        TxOutputMsg txOutputMessageOne = TxOutputMsg.builder().txValue(458900000000).pk_script(TR2_PK_SCRIPT_ONE).build();
        TxOutputMsg txOutputMessageTwo = TxOutputMsg.builder().txValue(50000000000).pk_script(TR2_PK_SCRIPT_TWO).build();
        List<TxOutputMsg> txOutputMsgList = new ArrayList<>()
        txOutputMsgList.add(txOutputMessageOne)
        txOutputMsgList.add(txOutputMessageTwo)

        HashMsg hash = HashMsg.builder().hash(HEX.decode("90c0deee7f4b5190176a5bb2879805728fa72a0b57bc40745d3ef7c352a918aa")).build();
        TxOutPointMsg txOutPointMsg = TxOutPointMsg.builder().index(0).hash(hash).build()
        TxInputMsg txInputMessage= TxInputMsg.builder().pre_outpoint(txOutPointMsg)
                .sequence(4294967295)
                .signature_script(
                        HEX.decode("04ac44bdf511477465cb70fef1d06b9241e74d26047ccbdfa641ec9a0115ad35594cbb58a61a6fd56893a405bcffbf6555995ddedc7e6cd4e5ceb83a37e1cf8f98")).build()


        List<TxInputMsg> txInputs = new ArrayList<>();
        txInputs.add(txInputMessage)

        return TxMsg.builder().version(1).tx_in(txInputs).tx_out(txOutputMsgList).build()

    }

    TxMsg makeCoinbaseTransaction() {

        TxOutputMsg txOutputMessageOne = TxOutputMsg.builder().txValue(5000000000).pk_script(TR1_PK_SCRIPT_ONE).build();
        List<TxOutputMsg> txOutputMsgList = new ArrayList<>()
        txOutputMsgList.add(txOutputMessageOne)

        HashMsg hash = HashMsg.builder().hash(HEX.decode("0000000000000000000000000000000000000000000000000000000000000000")).build();
        TxOutPointMsg txOutPointMsg =TxOutPointMsg.builder().index(26).hash(hash).build()

        TxInputMsg txInputMessage =TxInputMsg.builder().pre_outpoint(txOutPointMsg)
                .sequence(4294967295)
                .signature_script(HEX.decode("041844011d0142")).build()
        List<TxInputMsg> txInputs = new ArrayList<>()
        txInputs.add(txInputMessage)

        return TxMsg.builder().version(1).tx_in(txInputs).tx_out(txOutputMsgList).build()

    }
}
