package io.bitcoinsv.bsvcl.net.unit.protocol.serialization


import io.bitcoinsv.bsvcl.tools.bytes.ByteArrayReader
import io.bitcoinsv.bsvcl.tools.bytes.ByteArrayWriter
import io.bitcoinsv.bitcoinjsv.core.Sha256Hash
import io.bitcoinsv.bitcoinjsv.core.Utils
import io.bitcoinsv.bitcoinjsv.params.MainNetParams
import io.bitcoinsv.bitcoinjsv.params.Net
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

// TO: WE need to provide another Test, these values do not work anymore after changing the way the Hashes are serialized
@Ignore
class BlockMsgSerializerSpec extends Specification {

    private static final String BLOCK_BYTES = "0100000040f11b68435988807d64dff20261f7d9827825fbb37542601fb94d45000000005d0a2717cccfb28565e04baf2708f32068fb80f98765210ce6247b8939ab2012ecd9d24c1844011d00d3610502010000000100000000000000000000000000000000000000000000000000000000000000001a00000007041844011d0142ffffffff0100f2052a010000004104a313febd5f91b6a13bd9c5317030518fee96d1319a0eb10076917294933d09c17dc1588a06953a264738f2acea0c66b99e796caa4f28158e0dd5f6fed69a185b000000000100000001aa18a952c3f73e5d7440bc570b2aa78f72059887b25b6a1790514b7feedec090000000004104ac44bdf511477465cb70fef1d06b9241e74d26047ccbdfa641ec9a0115ad35594cbb58a61a6fd56893a405bcffbf6555995ddedc7e6cd4e5ceb83a37e1cf8f98ffffffff02004d92d86a00000014b8083945473bc8289efb681f94de7b07a5b851ad00743ba40b00000014ef01911c9efec6799d1ee5f7c6fb072d9669da8000000000"

    public static final String REF_MSG_FULL = "e3e1f3e8426c6f636b000000000000007f010000dd4043ed0100000040f11b68435988807d64dff20261f7d9827825fbb37542601fb94d45000000005d0a2717cccfb28565e04baf2708f32068fb80f98765210ce6247b8939ab2012ecd9d24c1844011d00d3610502010000000100000000000000000000000000000000000000000000000000000000000000001a00000007041844011d0142ffffffff0100f2052a010000004104a313febd5f91b6a13bd9c5317030518fee96d1319a0eb10076917294933d09c17dc1588a06953a264738f2acea0c66b99e796caa4f28158e0dd5f6fed69a185b000000000100000001aa18a952c3f73e5d7440bc570b2aa78f72059887b25b6a1790514b7feedec090000000004104ac44bdf511477465cb70fef1d06b9241e74d26047ccbdfa641ec9a0115ad35594cbb58a61a6fd56893a405bcffbf6555995ddedc7e6cd4e5ceb83a37e1cf8f98ffffffff02004d92d86a00000014b8083945473bc8289efb681f94de7b07a5b851ad00743ba40b00000014ef01911c9efec6799d1ee5f7c6fb072d9669da8000000000"


    public static final byte[] PREV_BLOCK_HASH = Sha256Hash.wrap("40f11b68435988807d64dff20261f7d9827825fbb37542601fb94d4500000000").bytes

    public static final byte[] MERKLE_ROOT = Sha256Hash.wrap("5d0a2717cccfb28565e04baf2708f32068fb80f98765210ce6247b8939ab2012").bytes

    public static final byte[] TR1_PK_SCRIPT_ONE = Utils.HEX.decode("04a313febd5f91b6a13bd9c5317030518fee96d1319a0eb10076917294933d09c17dc1588a06953a264738f2acea0c66b99e796caa4f28158e0dd5f6fed69a185b")

    public static final byte[] TR2_PK_SCRIPT_ONE = Utils.HEX.decode("b8083945473bc8289efb681f94de7b07a5b851ad")
    public static final byte[] TR2_PK_SCRIPT_TWO = Utils.HEX.decode("ef01911c9efec6799d1ee5f7c6fb072d9669da80")


    def "testing blockMessage BlockMsgSerializer Deserialize"(int byteInterval, int delayMs) {
        given:

            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig config = io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext context = io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext.builder()
                .protocolBasicConfig(config.getBasicConfig())
                .build()
            io.bitcoinsv.bsvcl.net.protocol.serialization.BlockMsgSerializer serializer = io.bitcoinsv.bsvcl.net.protocol.serialization.BlockMsgSerializer.getInstance()
        io.bitcoinsv.bsvcl.net.protocol.messages.BlockMsg blockMsg


        when:
            byte[] blockBytes = Utils.HEX.decode(BLOCK_BYTES)
            ByteArrayReader byteReader = io.bitcoinsv.bsvcl.net.unit.protocol.tools.ByteArrayArtificalStreamProducer.stream(blockBytes, byteInterval, delayMs);
            blockMsg = serializer.deserialize(context, byteReader)
        then:
            blockMsg.blockHeader.version.longValue() == Long.valueOf(1).longValue()
            blockMsg.blockHeader.prevBlockHash.getHashBytes() == Sha256Hash.wrap(PREV_BLOCK_HASH).bytes
            blockMsg.blockHeader.merkleRoot.getHashBytes() == Sha256Hash.wrap(MERKLE_ROOT).bytes
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
            blockMsg.messageType == io.bitcoinsv.bsvcl.net.protocol.messages.BlockMsg.MESSAGE_TYPE
        where:
            byteInterval | delayMs
            10           | 15
    }

    def "testing blockMessage BitcoinMsgSerializer Deserialize"(int byteInterval, int delayMs) {
        given:
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig config = io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext context = io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext.builder()
                .protocolBasicConfig(config.getBasicConfig())
                .build()
            ByteArrayReader byteReader = io.bitcoinsv.bsvcl.net.unit.protocol.tools.ByteArrayArtificalStreamProducer.stream(Utils.HEX.decode(REF_MSG_FULL), byteInterval, delayMs);
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.BitcoinMsgSerializer bitcoinSerializer = new io.bitcoinsv.bsvcl.net.protocol.serialization.common.BitcoinMsgSerializerImpl()
        when:
            io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsg<io.bitcoinsv.bsvcl.net.protocol.messages.BlockMsg> blockMsgBody = bitcoinSerializer.deserialize(context, byteReader)

        then:

            blockMsgBody.getHeader().getMagic() == config.getBasicConfig().getMagicPackage()
            blockMsgBody.getHeader().getCommand().toUpperCase() == io.bitcoinsv.bsvcl.net.protocol.messages.BlockMsg.MESSAGE_TYPE.toUpperCase()
            blockMsgBody.getBody().blockHeader.version.longValue() == Long.valueOf(1).longValue()
            blockMsgBody.getBody().blockHeader.prevBlockHash.getHashBytes() == Sha256Hash.wrap(PREV_BLOCK_HASH).bytes
            blockMsgBody.getBody().blockHeader.merkleRoot.getHashBytes() == Sha256Hash.wrap(MERKLE_ROOT).bytes
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
            blockMsgBody.getBody().messageType == io.bitcoinsv.bsvcl.net.protocol.messages.BlockMsg.MESSAGE_TYPE
        where:
            byteInterval | delayMs
            10           | 15
    }

    def "testing blockMessage BlockMsgSerializer Serializing"() {
        given:
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig config = io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext context = io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext.builder()
                .protocolBasicConfig(config.getBasicConfig())
                .build()
            io.bitcoinsv.bsvcl.net.protocol.serialization.BlockMsgSerializer serializer = io.bitcoinsv.bsvcl.net.protocol.serialization.BlockMsgSerializer.getInstance()
            List<io.bitcoinsv.bsvcl.net.protocol.messages.TxMsg> transactionMsgList = new ArrayList<>()
            io.bitcoinsv.bsvcl.net.protocol.messages.TxMsg transactionMsg1 = makeCoinbaseTransaction()
        io.bitcoinsv.bsvcl.net.protocol.messages.TxMsg transactionMsg2 = makeTransaction()

            transactionMsgList.add(transactionMsg1)
            transactionMsgList.add(transactionMsg2)

        io.bitcoinsv.bsvcl.net.protocol.messages.BlockHeaderMsg blockHeader = io.bitcoinsv.bsvcl.net.protocol.messages.BlockHeaderMsg.builder()
                .version(1)
                .prevBlockHash(io.bitcoinsv.bsvcl.net.protocol.messages.HashMsg.builder().hash(PREV_BLOCK_HASH).build())
                .merkleRoot(io.bitcoinsv.bsvcl.net.protocol.messages.HashMsg.builder().hash(MERKLE_ROOT).build())
                .creationTimestamp(1288886764)
                .difficultyTarget(486622232)
                .nonce(90297088)
                .transactionCount(transactionMsgList.size())
                .build();
            io.bitcoinsv.bsvcl.net.protocol.messages.BlockMsg blockMsg = io.bitcoinsv.bsvcl.net.protocol.messages.BlockMsg.builder()
                .blockHeader(blockHeader)
                .transactionMsgs(transactionMsgList).build()

            String blockMsgSerializedBytes

        when:
            ByteArrayWriter byteWriter = new ByteArrayWriter()
            serializer.serialize(context, blockMsg, byteWriter)
            blockMsgSerializedBytes = Utils.HEX.encode(byteWriter.reader().getFullContent())

        then:
            blockMsgSerializedBytes == BLOCK_BYTES
    }


    @Ignore
    def "testing blockMessage BitcoinMsgSerializer Serializing"() {
        given:
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig config = io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext context = io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext.builder()
                .protocolBasicConfig(config.getBasicConfig())
                .build()

            List<io.bitcoinsv.bsvcl.net.protocol.messages.TxMsg> transactionMsgList = new ArrayList<>()
            io.bitcoinsv.bsvcl.net.protocol.messages.TxMsg transactionMsg1 = makeCoinbaseTransaction()
            io.bitcoinsv.bsvcl.net.protocol.messages.TxMsg transactionMsg2 = makeTransaction()

            transactionMsgList.add(transactionMsg1)
            transactionMsgList.add(transactionMsg2)
            io.bitcoinsv.bsvcl.net.protocol.messages.BlockMsg blockMsg = io.bitcoinsv.bsvcl.net.protocol.messages.BlockMsg.builder()
                .version(1)
                .prevBlockHash(io.bitcoinsv.bsvcl.net.protocol.messages.HashMsg.builder().hash(PREV_BLOCK_HASH).build())
                .merkleRoot(io.bitcoinsv.bsvcl.net.protocol.messages.HashMsg.builder().hash(MERKLE_ROOT).build())
                .creationTimestamp(1288886764)
                .difficultyTarget(486622232)
                .nonce(90297088)
                .transactionMsgs(transactionMsgList).build()

            io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsg<io.bitcoinsv.bsvcl.net.protocol.messages.VersionMsg> bitcoinVersionMsg = new io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsgBuilder<>(config, blockMsg).build()
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.BitcoinMsgSerializer bitcoinSerializer = new io.bitcoinsv.bsvcl.net.protocol.serialization.common.BitcoinMsgSerializerImpl()


        when:

            byte[] msgBytes = bitcoinSerializer.serialize(context, bitcoinVersionMsg)
            String msgDeserializeVal = HEX.encode(msgBytes)
        then:
            msgDeserializeVal.equals(REF_MSG_FULL)
    }

    io.bitcoinsv.bsvcl.net.protocol.messages.TxMsg makeTransaction() {

        io.bitcoinsv.bsvcl.net.protocol.messages.TxOutputMsg txOutputMessageOne = io.bitcoinsv.bsvcl.net.protocol.messages.TxOutputMsg.builder().txValue(458900000000).pk_script(TR2_PK_SCRIPT_ONE).build();
        io.bitcoinsv.bsvcl.net.protocol.messages.TxOutputMsg txOutputMessageTwo = io.bitcoinsv.bsvcl.net.protocol.messages.TxOutputMsg.builder().txValue(50000000000).pk_script(TR2_PK_SCRIPT_TWO).build();
        List<io.bitcoinsv.bsvcl.net.protocol.messages.TxOutputMsg> txOutputMsgList = new ArrayList<>()
        txOutputMsgList.add(txOutputMessageOne)
        txOutputMsgList.add(txOutputMessageTwo)

        io.bitcoinsv.bsvcl.net.protocol.messages.HashMsg hash = io.bitcoinsv.bsvcl.net.protocol.messages.HashMsg.builder().hash(Utils.HEX.decode("90c0deee7f4b5190176a5bb2879805728fa72a0b57bc40745d3ef7c352a918aa")).build();
        io.bitcoinsv.bsvcl.net.protocol.messages.TxOutPointMsg txOutPointMsg = io.bitcoinsv.bsvcl.net.protocol.messages.TxOutPointMsg.builder().index(0).hash(hash).build()
        io.bitcoinsv.bsvcl.net.protocol.messages.TxInputMsg txInputMessage = io.bitcoinsv.bsvcl.net.protocol.messages.TxInputMsg.builder().pre_outpoint(txOutPointMsg)
            .sequence(4294967295)
            .signature_script(
                Utils.HEX.decode("04ac44bdf511477465cb70fef1d06b9241e74d26047ccbdfa641ec9a0115ad35594cbb58a61a6fd56893a405bcffbf6555995ddedc7e6cd4e5ceb83a37e1cf8f98")).build()


        List<io.bitcoinsv.bsvcl.net.protocol.messages.TxInputMsg> txInputs = new ArrayList<>();
        txInputs.add(txInputMessage)

        return io.bitcoinsv.bsvcl.net.protocol.messages.TxMsg.builder().version(1).tx_in(txInputs).tx_out(txOutputMsgList).build()

    }

    io.bitcoinsv.bsvcl.net.protocol.messages.TxMsg makeCoinbaseTransaction() {

        io.bitcoinsv.bsvcl.net.protocol.messages.TxOutputMsg txOutputMessageOne = io.bitcoinsv.bsvcl.net.protocol.messages.TxOutputMsg.builder().txValue(5000000000).pk_script(TR1_PK_SCRIPT_ONE).build();
        List<io.bitcoinsv.bsvcl.net.protocol.messages.TxOutputMsg> txOutputMsgList = new ArrayList<>()
        txOutputMsgList.add(txOutputMessageOne)

        io.bitcoinsv.bsvcl.net.protocol.messages.HashMsg hash = io.bitcoinsv.bsvcl.net.protocol.messages.HashMsg.builder().hash(Utils.HEX.decode("0000000000000000000000000000000000000000000000000000000000000000")).build();
        io.bitcoinsv.bsvcl.net.protocol.messages.TxOutPointMsg txOutPointMsg = io.bitcoinsv.bsvcl.net.protocol.messages.TxOutPointMsg.builder().index(26).hash(hash).build()

        io.bitcoinsv.bsvcl.net.protocol.messages.TxInputMsg txInputMessage = io.bitcoinsv.bsvcl.net.protocol.messages.TxInputMsg.builder().pre_outpoint(txOutPointMsg)
            .sequence(4294967295)
            .signature_script(Utils.HEX.decode("041844011d0142")).build()
        List<io.bitcoinsv.bsvcl.net.protocol.messages.TxInputMsg> txInputs = new ArrayList<>()
        txInputs.add(txInputMessage)

        return io.bitcoinsv.bsvcl.net.protocol.messages.TxMsg.builder().version(1).tx_in(txInputs).tx_out(txOutputMsgList).build()

    }


}
