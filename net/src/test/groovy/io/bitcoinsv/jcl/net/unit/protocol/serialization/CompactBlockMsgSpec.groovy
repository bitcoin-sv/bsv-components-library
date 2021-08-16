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
import io.bitcoinsv.jcl.net.protocol.messages.CompactBlockHeaderMsg
import io.bitcoinsv.jcl.net.protocol.messages.CompactBlockMsg
import io.bitcoinsv.jcl.net.protocol.messages.HashMsg
import io.bitcoinsv.jcl.net.protocol.serialization.CompactBlockMsgSerializer
import io.bitcoinsv.jcl.net.protocol.serialization.common.DeserializerContext
import io.bitcoinsv.jcl.net.protocol.serialization.common.SerializerContext
import io.bitcoinsv.jcl.net.unit.protocol.tools.ByteArrayArtificalStreamProducer
import spock.lang.Specification

class CompactBlockMsgSpec extends Specification {
    private static final String BLOCK_HEADER_BYTES = "0100000040f11b68435988807d64dff20261f7d9827825fbb37542601fb94d45000000005d0a2717cccfb28565e04baf2708f32068fb80f98765210ce6247b8939ab2012ecd9d24c1844011d00d36105"

    private static final String NONCE = "8DEC1AA458091500".toLowerCase()

    private static final String NUMBER_OF_SHORT_IDS_BYTES = "03"

    private static final List<String> SHORT_TX_IDS = Arrays.asList(
        "492C03224042".toLowerCase(), // 72843215973449
        "492E0102C0A2".toLowerCase(), // 178945551052361
        "696A05208032".toLowerCase() // 55525874428521
    )

    private static final String SHORT_TX_IDS_BYTES = SHORT_TX_IDS.join()

    private static final String NUMBER_OF_PREFILLED_TX_BYTES = "00"

    private static final String COMPACT_BLOCK_BYTES = BLOCK_HEADER_BYTES + NONCE + NUMBER_OF_SHORT_IDS_BYTES + SHORT_TX_IDS_BYTES + NUMBER_OF_PREFILLED_TX_BYTES

    private static final String FULL_FROM_NETWORK = "00e0ff27190260aa9d920fb89fdd5ab97c9d32319caef70523874c0200000000000000005f88fbe7cb81954b8093cc7aaf93295257aead88b2edabc903536731ed4350eac36a7d6055d30a18e1d9b35c" +
        "0aa8b56d8f223450" +
        "fdb50125fa130a64596a00e7df0cf470f6ebf40a75a6bdf04a16d7ff0f85d97f7bdb254ab1042aaf3a90e55b2b123d3fa3b6705d84de885dc0aed38e921e372f61df67dc089ac79bd64c2b71b40ac6ace6d3f50112a913667542d0fea2d5a2fa168dc8028da086ac133927cfcc9849c19ea1c4a0fccab0ebca52db70d938a7190744d0496975843f577f595f92a7892ff6324e11c19b58e43e06356399f347dfed6d78eb96c3becd1c62efa294c6ed3aab2e457dacb115f603149b268a497e185e7d0d917c5163b27528b72e11365de11542f8cbae1330e736d8bb2fb770edd828437bfc739a00be9e91181d2e404e88c19e2bbeb458c000bf6de18357f13e0acfa62667262536c4639c88c8f2327701c83f342414fe3e77aa370286a4826ca9705d7894872677c8f4c14c96737556853425572e0bdaa7ffc653aa0742bc5d88c37fc611b64671604aff70c0210119d0edefe77b39c66a2e503920d0d72098a6a5ccd42289ef248907be1fece87164749c3016c76d44fa95be260385ef29209b4acbb8623aa0ee61cebcb2ca195df7ae1ee55417dd80f41b84c0cae9ee4bf1e227ebab89c5433e90b4229763dfe2bb9b440e9fe02f6e85d0b59d711c6bdb303ffe3446360f900d511ac5163ff44b4d25f73cc5d0efe0bd1aa14c5dffc6f56367ce1723131175d386becd95e069390095310a974f36272b641f95c0a6f9fc6c60157d19e31ccc60cca5e3a568f3f2d2719f3954b387b326a70d3e0b2f63714dee07f689a84a4c7e71b69f5d6e2b08cc7979b336d25a791b2d9d055102b771bf68676945d4004762234f7d05b982280c1ffe9b99c667c2aca65fa360a4639f046a9993369a9379a3841add64077baf9e3969b6a97318ee2a5d2a68198bba39e17a7318905f10b12c3e867b21b51f658e568fc1eec4d43052af1b8eb4a6607ff5952d7269a2cfeede6cba8b34c4c6d29e200992805216f2c026bd21e17210fbae2283bee2e66434e625691d5bd268455be12cb475b8c7423f79c220115cb13f003c283d6c50ba874d90e2ac2a6f21e4b8e12289ce3a8d66f8d74bced7a30f0c91e707cedf74e8679fd4a3772f65808a3c9d6bb8ecf6fc59097cd801d286cc5a071eecdb258f8fc9d3a40751225daeb13403d0e6322ef7fb4fe73a2d63852205f158fda69e02405b0735ca3ff6c969030dfb0ce288090a1ac0af10d34f83cf887c74238111ed036ab792e70af44f377854cb8b38d635e48ec1ac9968cd69655952413f149289303f2b6340e003b0d41ab295cb1ea663a3bb1efd8a97ecc954e38ce66863604edb8c4b0b9010f6c2647bb52e3e87ab6521d92e7100d79121c60f7d1df5bd3eb9eea1242f249434d613a34c1dd281751178cbc573f303b99d39a1d66fb9367cf849f6e6012e410dcc8764eb25558d8c65fa4bab2a70994293c4fbc5ac567c73b52421ea8af29083a320f2384568a4dab9a8fe32d5c601f5199a2f5d2790a14b1c6a9e11660a50413e474087a73fdc6b970b42795c95bc3217244f946732acf56923bd34d2841cc33f77d20ead4c49940f02448c0f2e31ddf302670ff30bd8e3b986af536f3edf351cb8a1678fece9af6ee8044fb5d15807e0f07bc6778bc0c45c03d5dbab7eaf5e04a005ac44e996a4ddd0103bbddedbaf7c90e67305a2c7082f8c21ab4eeb55fa9bb1527e963737e582f99d89b54abd8445676fbea5bbdf77f24d449705d6c2fa759a03559d863e9e7db4eb92235e027e506c6b161628c29a4844def183d4c4d4e0ddd21acbc330851236340e1dd65666b3086cae1425cce2e3c63e1c85a32ed4d4045f0e7a0319d70da856f5a1cb8b0a9909ff86488f24687414a0b31c46f947bcd4f3e27cd1b5c30712f50afdcfc88f2f8606493f18644576644e78cd7549c8c05cd9035cbe921c96e5f138a69532bad15cf60d765406f3a127abc9d87c638e2bef00186055961db5eb477d0740d6ef135c28c2ac33d4363f052befcc53a54f5bdc2d79a8c66a1ff34654f878993d02289f3ecc7c2a6310ce218493539d167077fa0dfaee62e736651d37733231619248968ed80fda0b724ff0ae002d990dea0b3a277f28e03f9e380271d1f60e81ab106a86ae394c3d49df838b76607c46ad36c1e6edae23a5204c82d57ff2a86db100b6598d114d4401734e4e517522d24461ac7d41c07baf41ea2635a63b80ccc1558bf6bedb21c3804d392bc3a6f65d33fb16c71205643d879cac6b6159119afb65d8631c50edae008b1bb4e7bd9aa2548ace055c23dd6b2e33689f34a71089ef3da800320377b923ec6308a3bc1abc4861e859f4181b3736bcbec962db3f8c367e578efc861699358faf44ddba831dfd92a2ac4d82b1269932f1626e431e133f5a011ddf4c74e1c66948998f1d7fc6a88775387fcd6f06b3dbc48af8cc5e4593f8676d4795226923b55068d0517fc4e7041181167b658d5461cf67b32cf70687711abb72a4d6c16b54941d9be0aca5ad9572429207ccaa6b2c4560939ba4d46a0e23198147602eab2105eef98d260425590e032195ea451be888c676bba94039d44c0834575ac20fd4b8d5b6da4c99711411709254ae46818f9c8a4e3b7e0f04f8712f40230bb3c72837af87282e404c81c992d46860163e9221cd594fd5d1dd85f0224378186d3db6313565a115a64c064fe232a1db32190292929ac3c2e525f4074b36c0c2efb174743d27a02fbbfff1217dbf8a75d4eb6107423ae10666208ce4ef730ab7573d64fc77a13f0363a43e4edab72cc1970b61e8b3c2f58454a9b132bccde7236c8aeeafb51e37f78b71762e60058d512e42735a0cd601b3a36e57deb5db652b20ed0e31d6ba4552661ae03b83d78abd287e2348b8a001aa4e2ae176847fdd1531fbe7930c1f40c5cbfbd46c9b36584ea0927f7bbde3fa9f37280f00dc641d9366d43536c5fd78dc283f2195c2d9a109c0aeb9f1e800508b1bb2f85122132b4f4f970ae871b6444234bafdb99f8ecb628ea7fedfcec5ec7da9d45359ea509fc34fc8de62a5375e463d8df3fd4932ede54a5e8238387831422c89e96a0bab49f2f815d340ae4dcd35bbec843d93596e487a1082144b043c7c6bdf3ee52870b0d44013b487f61a329bd9e568b3ed15e0f316e045cda25c8e5025ba17669cf7bacb4fec714e205b89163b36e36b6e54c6b8863d226aba6e45bf85b7dfb879ccf5274a0fc95af353da50d1ca9596a7e7adf71690e31aaf4e0e9aeebefbd2d4e95f4ef45104eeb441fc0ae96f866bb75b26564b1651a1b8cdab3e0015215b77c6ca8ff58aba2034a5af9493e1c5b55c255752179c0543ae461960734e34462f8a66b1bbd2dd683998a6a2fb1f7200af2f06bbf9e529f4d24adc127ab3f791c9cd53b73035acb3243c3cc1801bc890b5d06947d3abe2027648640d45d5772a040d341a314f912cf0f4e0eb02168bcfd731d5bf1eaed9a63a0fcc9ddfdff9ca22604c435128e4e5635ec347ba09ee2e2367fcbeffeec2b8cc4c459ed48fab45cbd71a6303edb5a0c0abd58a491dd42ab4e67b4e0a4407a4a4da946685c5b59196821d9ff21447a7d931a7e416247a1f2abc6839e9ae44e2a65772da2a63dea632dc6cdd0003a2e668fb5f2ee5be68d42fe7a39d4a88025f68df197649d30424905aa7d6efdff83429faef72bfe5a8e19226e68b27b7d20d9493882b1a07c977ba5dbbae010001000000010000000000000000000000000000000000000000000000000000000000000000ffffffff41033f6e0a2f7461616c2e636f6d2f506c656173652070617920302e3520736174732f627974652c20696e666f407461616c2e636f6d226f856710451085e98a0900ffffffff029b8a4125000000001976a9148e9170be3f733a9773c907517fb9b786f1c884c688ac0000000000000000fda502006a04ac1eed884d53027b2276657273696f6e223a22302e31222c22686569676874223a3638333538332c22707265764d696e65724964223a22303365393264336535633366376264393435646662663438653761393933393362316266623366313166333830616533306432383665376666326165633561323730222c22707265764d696e65724964536967223a2233303435303232313030643736333630653464323133333163613836663031386330343665353763393338663139373735303734373333333533363062653337303438636165316166333032323030626536363034353430323162663934363465393966356139353831613938633963663439353430373539386335396234373334623266646234383262663937222c226d696e65724964223a22303365393264336535633366376264393435646662663438653761393933393362316266623366313166333830616533306432383665376666326165633561323730222c2276637478223a7b2274784964223a2235373962343335393235613930656533396133376265336230306239303631653734633330633832343133663664306132303938653162656137613235313566222c22766f7574223a307d2c226d696e6572436f6e74616374223a7b22656d61696c223a22696e666f407461616c2e636f6d222c226e616d65223a225441414c20446973747269627574656420496e666f726d6174696f6e20546563686e6f6c6f67696573222c226d65726368616e74415049456e64506f696e74223a2268747470733a2f2f6d65726368616e746170692e7461616c2e636f6d2f227d7d473045022100b04788cc92c434c1ee3f7df64b0e51ed1019d23c925f6709920b9f724e78579a02206de016b06e1f3d34dd604cbfe90ac7cc7a2e464ac24982e6a2ab6bb708e8fe7d00000000"

    def "Testing CompactBlockMsg Deserialize"(int byteInterval, int delayMs) {
        given:
        ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
        DeserializerContext context = DeserializerContext.builder()
                .protocolBasicConfig(config.getBasicConfig())
                .build()

            byte[] bytes = Utils.HEX.decode(COMPACT_BLOCK_BYTES)

        CompactBlockMsgSerializer serializer = CompactBlockMsgSerializer.getInstance()
        CompactBlockMsg message

        when:
            ByteArrayReader byteReader = ByteArrayArtificalStreamProducer.stream(bytes, byteInterval, delayMs)
            message = serializer.deserialize(context, byteReader)

        then:
            message.messageType == CompactBlockMsg.MESSAGE_TYPE
            message.getNonce() == 5921250825923725
            message.getShortTxIds().size() == 3
            message.getShortTxIds()[0] == 72843215973449
            message.getShortTxIds()[1] == 178945551052361
            message.getShortTxIds()[2] == 55525874428521

        where:
            byteInterval | delayMs
            10           | 5
    }

    def "Testing CompactBlockMsg Serializing"() {
        given:
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
        SerializerContext context = SerializerContext.builder()
                .protocolBasicConfig(config.getBasicConfig())
                .build()

            CompactBlockMsgSerializer serializer = CompactBlockMsgSerializer.getInstance()

        CompactBlockHeaderMsg blockHeaderMsg = CompactBlockHeaderMsg.builder()
                .hash(HashMsg.builder().hash(Utils.HEX.decode("2f6428543c10f8b30e76f9519b597259e4839e0c91ed278a7eee27f8c014a525")).build())
                .version(1)
                .prevBlockHash(HashMsg.builder().hash(Utils.HEX.decode("40f11b68435988807d64dff20261f7d9827825fbb37542601fb94d4500000000")).build())
                .merkleRoot(HashMsg.builder().hash(Utils.HEX.decode("5d0a2717cccfb28565e04baf2708f32068fb80f98765210ce6247b8939ab2012")).build())
                .creationTimestamp(1288886764)
                .difficultyTarget(486622232)
                .nonce(90297088)
                .build()

            List<Long> shortTxIds = Arrays.asList(
                72843215973449,
                178945551052361,
                55525874428521
            )

            CompactBlockMsg compactBlockMsg = CompactBlockMsg.builder()
                .header(blockHeaderMsg)
                .nonce(5921250825923725)
                .shortTxIds(shortTxIds)
                .prefilledTransactions(Collections.emptyList())
                .build()

            String messageSerializedBytes
        when:
            ByteArrayWriter byteWriter = new ByteArrayWriter()
            serializer.serialize(context, compactBlockMsg, byteWriter)
            messageSerializedBytes = Utils.HEX.encode(byteWriter.reader().getFullContent())
            byteWriter.reader()

        then:
            messageSerializedBytes == COMPACT_BLOCK_BYTES
    }

    def "Testing FULL CompactBlockMsg Serialization"() {
        given:
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            SerializerContext serializerContext = SerializerContext.builder()
                .protocolBasicConfig(config.getBasicConfig())
                .build()
            DeserializerContext deserializerContext = DeserializerContext.builder()
                .protocolBasicConfig(config.getBasicConfig())
                .build()

            CompactBlockMsgSerializer serializer = CompactBlockMsgSerializer.getInstance()

            byte[] bytes = Utils.HEX.decode(FULL_FROM_NETWORK)

        when:
            ByteArrayReader byteReader = ByteArrayArtificalStreamProducer.stream(bytes, byteInterval, delayMs)
            CompactBlockMsg msg = serializer.deserialize(deserializerContext, byteReader)

            ByteArrayWriter byteWriter = new ByteArrayWriter()
            serializer.serialize(serializerContext, msg, byteWriter)
            byte[] result = byteWriter.reader().getFullContent()

        then:
            Arrays.equals(bytes, result)

        where:
            byteInterval | delayMs
            10           | 5
    }
}
