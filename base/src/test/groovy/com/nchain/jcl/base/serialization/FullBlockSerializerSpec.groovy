package com.nchain.jcl.base.serialization

import com.nchain.jcl.base.domain.api.base.FullBlock
import com.nchain.jcl.base.tools.bytes.ByteArrayReader
import com.nchain.jcl.base.tools.bytes.HEX
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper
import spock.lang.Specification

class FullBlockSerializerSpec extends Specification {

    // A whole Block Serialized, used as a reference:
    private static final String BLOCK_BYTES = "0100000040f11b68435988807d64dff20261f7d9827825fbb37542601fb94d45000000005d0a2717cccfb28565e04baf2708f32068fb80f98765210ce6247b8939ab2012ecd9d24c1844011d00d3610502010000000100000000000000000000000000000000000000000000000000000000000000001a00000007041844011d0142ffffffff0100f2052a010000004104a313febd5f91b6a13bd9c5317030518fee96d1319a0eb10076917294933d09c17dc1588a06953a264738f2acea0c66b99e796caa4f28158e0dd5f6fed69a185b000000000100000001aa18a952c3f73e5d7440bc570b2aa78f72059887b25b6a1790514b7feedec090000000004104ac44bdf511477465cb70fef1d06b9241e74d26047ccbdfa641ec9a0115ad35594cbb58a61a6fd56893a405bcffbf6555995ddedc7e6cd4e5ceb83a37e1cf8f98ffffffff02004d92d86a00000014b8083945473bc8289efb681f94de7b07a5b851ad00743ba40b00000014ef01911c9efec6799d1ee5f7c6fb072d9669da8000000000"

    // Some data about the Block Header:
    public static final byte[] PREV_BLOCK_HASH = Sha256Wrapper.wrap("00000000454db91f604275b3fb257882d9f76102f2df647d80885943681bf140").bytes
    public static final byte[] MERKLE_ROOT = Sha256Wrapper.wrap("1220ab39897b24e60c216587f980fb6820f30827af4be06585b2cfcc17270a5d").bytes

    def "serializing Full Block"() {
        // PENDING... IT might not be necessary Soon (support for Fullblocks will get Deprecated)
    }

    def "deserializing Full Block"() {
        when:
            ByteArrayReader reader = new ByteArrayReader(HEX.decode(BLOCK_BYTES))
            FullBlock fullBlock = FullBlockSerializer.getInstance().deserialize(reader)
        then:
            fullBlock != null
            fullBlock.header.prevBlockHash.bytes == PREV_BLOCK_HASH
            fullBlock.header.merkleRoot.bytes == MERKLE_ROOT
    }
}
