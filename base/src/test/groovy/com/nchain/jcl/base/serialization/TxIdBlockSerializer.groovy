package com.nchain.jcl.base.serialization

import com.nchain.jcl.base.domain.api.base.BlockHeader
import com.nchain.jcl.base.domain.api.extended.TxIdBlock
import com.nchain.jcl.base.domain.bean.extended.TxIdBlockBean
import com.nchain.jcl.base.tools.bytes.HEX
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper
import spock.lang.Specification

class TxIdBlockSerializerSpec extends Specification {

    // A Block Header Serialized and used as a reference:
    private static final byte[] REF_HEADER = HEX.decode("0100000040f11b68435988807d64dff20261f7d9827825fbb37542601fb94d45000000005d0a2717cccfb28565e04baf2708f32068fb80f98765210ce6247b8939ab2012ecd9d24c1844011d00d36105")

    // Block Header values:
    private static final int VERSION = 1
    private static long CREATION_TIME = 1288886764
    private static int DIFFICULTY = 486622232
    private static int NONCE = 90297088

    // HASHes in correct Order:
    private static final byte[] PREV_HASH = HEX.decode("00000000454db91f604275b3fb257882d9f76102f2df647d80885943681bf140")
    private static final byte[] MERKLE_HASH = HEX.decode("1220ab39897b24e60c216587f980fb6820f30827af4be06585b2cfcc17270a5d")


    // Some Txs Hashhes (in Right Order):
    private static final String TX_HASH_1 = "3f8c877dc85e0d2ae98c5e63b0f89183bf162b411531c645b924a35a20ab1343"
    private static final String TX_HASH_2 = "b178307205ced2d9f03f62c258ad24001ea17d5686d909d1f4d50866bee42405"
    private static final String TX_HASH_3 = "0f0c138ef1b76241721bccf519354fbea46f3c4a831659f70000d10bcca182fb"

    // The Whole Object already SERIALIZED:
    private static final String REF_TXIDBLOCK = HEX.encode(REF_HEADER) +
                                "03"  +
                                Sha256Wrapper.wrapReversed(HEX.decode(TX_HASH_1)).toString() +
                                Sha256Wrapper.wrapReversed(HEX.decode(TX_HASH_2)).toString() +
                                Sha256Wrapper.wrapReversed(HEX.decode(TX_HASH_3)).toString()

    def "deserializing TxIdBlock"() {
        when:
            TxIdBlock txIdBlock = BitcoinSerializerFactory.deserialize(TxIdBlock.class, REF_TXIDBLOCK);
        then:
            txIdBlock.header.version == VERSION
            txIdBlock.header.time == CREATION_TIME
            txIdBlock.header.difficultyTarget == DIFFICULTY
            txIdBlock.header.nonce == NONCE
            txIdBlock.txids.size() == 3
            txIdBlock.txids.get(0).bytes == HEX.decode(TX_HASH_1)
            txIdBlock.txids.get(1).bytes == HEX.decode(TX_HASH_2)
            txIdBlock.txids.get(2).bytes == HEX.decode(TX_HASH_3)
    }

    def "serializing TxIdblock"() {
        given:
            TxIdBlock txIdBlock = TxIdBlockBean.builder()
                                .header(BlockHeader.builder()
                                            .version(VERSION)
                                            .time(CREATION_TIME)
                                            .difficultyTarget(DIFFICULTY)
                                            .nonce(NONCE)
                                            .prevBlockHash(Sha256Wrapper.wrap(PREV_HASH))
                                            .merkleRoot(Sha256Wrapper.wrap(MERKLE_HASH))
                                        .build())
                                .txids(Arrays.asList(
                                        Sha256Wrapper.wrap(TX_HASH_1),
                                        Sha256Wrapper.wrap(TX_HASH_2),
                                        Sha256Wrapper.wrap(TX_HASH_3)))
                                .build();
        when:
            String txIdBlockHEX = HEX.encode(BitcoinSerializerFactory.serialize(txIdBlock))
        then:
            txIdBlockHEX.equals(REF_TXIDBLOCK)
    }
}
