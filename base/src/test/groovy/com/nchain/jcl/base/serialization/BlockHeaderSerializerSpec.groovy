package com.nchain.jcl.base.serialization

import com.nchain.jcl.base.domain.api.base.BlockHeader
import com.nchain.jcl.base.tools.bytes.HEX
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper
import spock.lang.Specification

class BlockHeaderSerializerSpec extends Specification {

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


    def "Deserializing BlockHeader"() {
        when:
            BlockHeader header = BitcoinSerializerFactory.deserialize(BlockHeader.class, REF_HEADER)
        then:
            header.version == VERSION
            header.prevBlockHash.bytes == PREV_HASH
            header.merkleRoot.bytes == MERKLE_HASH
            header.time == CREATION_TIME
            header.difficultyTarget == DIFFICULTY
            header.nonce == NONCE
    }

    def "Serializing BlockHeader"() {
        when:
            BlockHeader header = BlockHeader.builder()
                                    .version(VERSION)
                                    .prevBlockHash(Sha256Wrapper.wrap(PREV_HASH))
                                    .merkleRoot(Sha256Wrapper.wrap(MERKLE_HASH))
                                    .time(CREATION_TIME)
                                    .difficultyTarget(DIFFICULTY)
                                    .nonce(NONCE)
                                    .build()
            byte[] header_ex = BitcoinSerializerFactory.serialize(header)
        then:
            header_ex == REF_HEADER
    }

    def "Deserializing with Builder"() {
        when:
            BlockHeader header = BlockHeader.builder(REF_HEADER).build()
        then:
            header.version == VERSION
            header.prevBlockHash.bytes == PREV_HASH
            header.merkleRoot.bytes == MERKLE_HASH
            header.time == CREATION_TIME
            header.difficultyTarget == DIFFICULTY
            header.nonce == NONCE
    }

}
