package io.bitcoinsv.bsvcl.common.unit.bytes

import com.google.common.collect.Lists
import io.bitcoinsv.bitcoinjsv.core.Sha256Hash
import io.bitcoinsv.bsvcl.common.bytes.Sha256HashIncremental
import spock.lang.Specification

/**
 * Testing class for Sha256HashIncremental
 */
class Sha256HashIncrementalSpec extends Specification {

    /**
     * We calculate the hash of a bunch of bytes using both the regular Sha256Hash class and the incremental one.
     * Thr results should be the same
     */
    def "hash and compare with regular Hash"() {
        given:
            byte[] data = "This is an example of the data".getBytes()
        when:
            // We hash it using the regular Hash:
            Sha256Hash regularHashResult = Sha256Hash.wrap(Sha256Hash.hashTwice(data))
            // We hash it using the incremental hash
        Sha256HashIncremental incrementalHash = new Sha256HashIncremental()
            List partialBytes = Lists.partition(Arrays.asList(data), 5)
            partialBytes.forEach({bytes -> incrementalHash.add(bytes.toArray(new byte[bytes.size()]))})
            Sha256Hash incrementalHashResult = Sha256Hash.wrap(incrementalHash.hashTwice())
        then:
            regularHashResult.equals(incrementalHashResult)
    }
}
