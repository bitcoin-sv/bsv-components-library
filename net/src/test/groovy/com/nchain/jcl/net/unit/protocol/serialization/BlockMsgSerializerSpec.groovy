package com.nchain.jcl.net.unit.protocol.serialization

import com.nchain.jcl.net.protocol.config.ProtocolConfig
import com.nchain.jcl.net.protocol.config.provided.ProtocolBSVMainConfig
import com.nchain.jcl.net.protocol.messages.BlockHeaderMsg
import com.nchain.jcl.net.protocol.messages.BlockMsg
import com.nchain.jcl.net.protocol.messages.HashMsg
import com.nchain.jcl.net.protocol.messages.TxMsg
import com.nchain.jcl.net.protocol.messages.TxInputMsg
import com.nchain.jcl.net.protocol.messages.TxOutPointMsg
import com.nchain.jcl.net.protocol.messages.TxOutputMsg
import com.nchain.jcl.net.protocol.messages.VersionMsg
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsgBuilder
import com.nchain.jcl.net.protocol.serialization.BlockMsgSerializer
import com.nchain.jcl.net.protocol.serialization.common.BitcoinMsgSerializer
import com.nchain.jcl.net.protocol.serialization.common.BitcoinMsgSerializerImpl
import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext
import com.nchain.jcl.net.protocol.serialization.common.SerializerContext
import com.nchain.jcl.net.unit.protocol.tools.ByteArrayArtificalStreamProducer
import com.nchain.jcl.base.tools.bytes.ByteArrayReader
import com.nchain.jcl.base.tools.bytes.ByteArrayWriter
import com.nchain.jcl.base.tools.bytes.HEX
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper
import spock.lang.Ignore
import spock.lang.Specification

/**
 * @author n.srivastava@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 11/10/2019 17:13
 *
 * Testing class for theBlockMsgSerilaizer Serialization.
 * The test is taken the assumption that we have already a correct serialization version of this Message, obtained
 * from another source that we trust (in this case the Java BitcoinJ library). So we serialize/deserialize some
 * messages with out code and compare the results with that reference.
 */
class BlockMsgSerializerSpec extends Specification {

    private static final String BLOCK_BYTES = "0100000040f11b68435988807d64dff20261f7d9827825fbb37542601fb94d45000000005d0a2717cccfb28565e04baf2708f32068fb80f98765210ce6247b8939ab2012ecd9d24c1844011d00d3610502010000000100000000000000000000000000000000000000000000000000000000000000001a00000007041844011d0142ffffffff0100f2052a010000004104a313febd5f91b6a13bd9c5317030518fee96d1319a0eb10076917294933d09c17dc1588a06953a264738f2acea0c66b99e796caa4f28158e0dd5f6fed69a185b000000000100000001aa18a952c3f73e5d7440bc570b2aa78f72059887b25b6a1790514b7feedec090000000004104ac44bdf511477465cb70fef1d06b9241e74d26047ccbdfa641ec9a0115ad35594cbb58a61a6fd56893a405bcffbf6555995ddedc7e6cd4e5ceb83a37e1cf8f98ffffffff02004d92d86a00000014b8083945473bc8289efb681f94de7b07a5b851ad00743ba40b00000014ef01911c9efec6799d1ee5f7c6fb072d9669da8000000000"

    public static final String REF_MSG_FULL = "e3e1f3e8426c6f636b000000000000007f010000dd4043ed0100000040f11b68435988807d64dff20261f7d9827825fbb37542601fb94d45000000005d0a2717cccfb28565e04baf2708f32068fb80f98765210ce6247b8939ab2012ecd9d24c1844011d00d3610502010000000100000000000000000000000000000000000000000000000000000000000000001a00000007041844011d0142ffffffff0100f2052a010000004104a313febd5f91b6a13bd9c5317030518fee96d1319a0eb10076917294933d09c17dc1588a06953a264738f2acea0c66b99e796caa4f28158e0dd5f6fed69a185b000000000100000001aa18a952c3f73e5d7440bc570b2aa78f72059887b25b6a1790514b7feedec090000000004104ac44bdf511477465cb70fef1d06b9241e74d26047ccbdfa641ec9a0115ad35594cbb58a61a6fd56893a405bcffbf6555995ddedc7e6cd4e5ceb83a37e1cf8f98ffffffff02004d92d86a00000014b8083945473bc8289efb681f94de7b07a5b851ad00743ba40b00000014ef01911c9efec6799d1ee5f7c6fb072d9669da8000000000"

    public static final byte[] PREV_BLOCK_HASH = Sha256Wrapper.wrap("00000000454db91f604275b3fb257882d9f76102f2df647d80885943681bf140").bytes
    public static final byte[] MERKLE_ROOT = Sha256Wrapper.wrap("1220ab39897b24e60c216587f980fb6820f30827af4be06585b2cfcc17270a5d").bytes

    public static final byte[] TR1_PK_SCRIPT_ONE = HEX.decode("04a313febd5f91b6a13bd9c5317030518fee96d1319a0eb10076917294933d09c17dc1588a06953a264738f2acea0c66b99e796caa4f28158e0dd5f6fed69a185b")

    public static final byte[] TR2_PK_SCRIPT_ONE = HEX.decode("b8083945473bc8289efb681f94de7b07a5b851ad")
    public static final byte[] TR2_PK_SCRIPT_TWO = HEX.decode("ef01911c9efec6799d1ee5f7c6fb072d9669da80")



    def "testing blockMessage BlockMsgSerializer Deserialize"(int byteInterval, int delayMs) {
        given:

            ProtocolConfig config = new ProtocolBSVMainConfig()
            DeserializerContext context = DeserializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .build()
            BlockMsgSerializer serializer = BlockMsgSerializer.getInstance()
        BlockMsg blockMsg


        when:
            byte[] blockBytes = HEX.decode(BLOCK_BYTES)
            ByteArrayReader byteReader = ByteArrayArtificalStreamProducer.stream(blockBytes, byteInterval, delayMs);
            blockMsg = serializer.deserialize(context, byteReader)
        then:
            blockMsg.blockHeader.version.longValue() == Long.valueOf(1).longValue()
            blockMsg.blockHeader.prevBlockHash.getHashBytes() == Sha256Wrapper.wrap(PREV_BLOCK_HASH).bytes
            blockMsg.blockHeader.merkleRoot.getHashBytes() == Sha256Wrapper.wrap(MERKLE_ROOT).bytes
            blockMsg.blockHeader.creationTimestamp == Long.valueOf(1288886764).longValue()
            blockMsg.blockHeader.difficultyTarget == Long.valueOf(486622232).longValue()
            blockMsg.blockHeader.nonce == Long.valueOf(90297088).longValue()
            blockMsg.blockHeader.transactionCount.value == Long.valueOf(2).longValue()
            blockMsg.transactionMsg.get(0).getVersion() == Long.valueOf(1).longValue()
            blockMsg.transactionMsg.get(0).getTx_in_count().value == Long.valueOf(1).longValue()
            blockMsg.transactionMsg.get(0).getTx_out_count().value == Long.valueOf(1).longValue()
            blockMsg.transactionMsg.get(1).getVersion() == Long.valueOf(1).longValue()
            blockMsg.transactionMsg.get(1).getTx_in_count().value == Long.valueOf(1).longValue()
            blockMsg.transactionMsg.get(1).getTx_out_count().value == Long.valueOf(2).longValue()
            blockMsg.messageType == BlockMsg.MESSAGE_TYPE
        where:
            byteInterval | delayMs
                10       |    15
    }

    def "testing blockMessage BitcoinMsgSerializer Deserialize"(int byteInterval, int delayMs) {
        given:
            ProtocolConfig config = new ProtocolBSVMainConfig()
            DeserializerContext context = DeserializerContext.builder()
                .protocolBasicConfig(config.getBasicConfig())
                .build()
            ByteArrayReader byteReader = ByteArrayArtificalStreamProducer.stream(HEX.decode(REF_MSG_FULL), byteInterval, delayMs);
            BitcoinMsgSerializer bitcoinSerializer = new BitcoinMsgSerializerImpl()
        when:
            BitcoinMsg<BlockMsg> blockMsgBody = bitcoinSerializer.deserialize(context, byteReader, BlockMsg.MESSAGE_TYPE)

        then:

            blockMsgBody.getHeader().getMagic() == config.getBasicConfig().getMagicPackage()
            blockMsgBody.getHeader().getCommand().toUpperCase() == BlockMsg.MESSAGE_TYPE.toUpperCase()
            blockMsgBody.getBody().blockHeader.version.longValue() == Long.valueOf(1).longValue()
            blockMsgBody.getBody().blockHeader.prevBlockHash.getHashBytes() == Sha256Wrapper.wrap(PREV_BLOCK_HASH).bytes
            blockMsgBody.getBody().blockHeader.merkleRoot.getHashBytes() == Sha256Wrapper.wrap(MERKLE_ROOT).bytes
            blockMsgBody.getBody().blockHeader.creationTimestamp == Long.valueOf(1288886764).longValue()
            blockMsgBody.getBody().blockHeader.difficultyTarget == Long.valueOf(486622232).longValue()
            blockMsgBody.getBody().blockHeader.nonce == Long.valueOf(90297088).longValue()
            blockMsgBody.getBody().blockHeader.transactionCount.value == Long.valueOf(2).longValue()
            blockMsgBody.getBody().transactionMsg.get(0).getVersion() == Long.valueOf(1).longValue()
            blockMsgBody.getBody().transactionMsg.get(0).getTx_in_count().value == Long.valueOf(1).longValue()
            blockMsgBody.getBody().transactionMsg.get(0).getTx_out_count().value == Long.valueOf(1).longValue()
            blockMsgBody.getBody().transactionMsg.get(1).getVersion() == Long.valueOf(1).longValue()
            blockMsgBody.getBody().transactionMsg.get(1).getTx_in_count().value == Long.valueOf(1).longValue()
            blockMsgBody.getBody().transactionMsg.get(1).getTx_out_count().value == Long.valueOf(2).longValue()
            blockMsgBody.getBody().messageType == BlockMsg.MESSAGE_TYPE
        where:
            byteInterval | delayMs
                10       |    15
    }

    def "testing blockMessage BlockMsgSerializer Serializing"() {
        given:
            ProtocolConfig config = new ProtocolBSVMainConfig()
            SerializerContext context = SerializerContext.builder()
                .protocolBasicConfig(config.getBasicConfig())
                .build()
            BlockMsgSerializer serializer = BlockMsgSerializer.getInstance()
            List<TxMsg> transactionMsgList = new ArrayList<>()
            TxMsg transactionMsg1 = makeCoinbaseTransaction()
            TxMsg transactionMsg2 = makeTransaction()

            transactionMsgList.add(transactionMsg1)
            transactionMsgList.add(transactionMsg2)

            BlockHeaderMsg blockHeader = BlockHeaderMsg.builder()
                    .version(1)
                    .prevBlockHash(HashMsg.builder().hash(PREV_BLOCK_HASH).build())
                    .merkleRoot(HashMsg.builder().hash(MERKLE_ROOT).build())
                    .creationTimestamp(1288886764)
                    .difficultyTarget(486622232)
                    .nonce(90297088)
                    .transactionCount(transactionMsgList.size())
                    .build();
            BlockMsg blockMsg = BlockMsg.builder()
                .blockHeader(blockHeader)
                .transactionMsgs(transactionMsgList).build()

            String blockMsgSerializedBytes

        when:
            ByteArrayWriter byteWriter = new ByteArrayWriter()
            serializer.serialize(context, blockMsg, byteWriter)
            blockMsgSerializedBytes = HEX.encode(byteWriter.reader().getFullContent())

        then:
            blockMsgSerializedBytes == BLOCK_BYTES
    }


    @Ignore   def "testing blockMessage BitcoinMsgSerializer Serializing"() {
        given:
        ProtocolConfig config = new ProtocolBSVMainConfig()
        SerializerContext context = SerializerContext.builder()
                .protocolconfig(config)
                .build()

        List<TxMsg> transactionMsgList = new ArrayList<>()
        TxMsg transactionMsg1 = makeCoinbaseTransaction()
        TxMsg transactionMsg2 = makeTransaction()

        transactionMsgList.add(transactionMsg1)
        transactionMsgList.add(transactionMsg2)
        BlockMsg blockMsg = BlockMsg.builder()
                .version(1)
                .prevBlockHash(HashMsg.builder().hash(PREV_BLOCK_HASH).build())
                .merkleRoot(HashMsg.builder().hash(MERKLE_ROOT).build())
                .creationTimestamp(1288886764)
                .difficultyTarget(486622232)
                .nonce(90297088)
                .transactionMsgs(transactionMsgList).build()

        BitcoinMsg<VersionMsg> bitcoinVersionMsg = new BitcoinMsgBuilder<>(config, blockMsg).build()
        BitcoinMsgSerializer bitcoinSerializer = new BitcoinMsgSerializerImpl()


        when:

        byte[] msgBytes = bitcoinSerializer.serialize(context, bitcoinVersionMsg, BlockMsg.MESSAGE_TYPE)
        String msgDeserializeVal = HEX.encode(msgBytes)
        then:
        msgDeserializeVal.equals(REF_MSG_FULL)
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
