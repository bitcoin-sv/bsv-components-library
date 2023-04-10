package io.bitcoinsv.bsvcl.tools.unit.bytes


import groovy.util.logging.Slf4j
import io.bitcoinsv.bsvcl.tools.bytes.ByteArray
import io.bitcoinsv.bsvcl.tools.bytes.ByteArrayConfig
import io.bitcoinsv.bsvcl.tools.bytes.ByteArrayNIO
import spock.lang.Specification


@Slf4j
class ByteArrayImplSpec extends Specification {

    /**
     * We test the content of the Byte Array is correct according to the parameters:
     *
     * @param bytesAdded    Bytes to add to the ByteArray
     * @param toExtract     Number of Bytes to extract from the ByteArray
     * @param result        Result expected in the extraction
     */
    def "testing adding and extracting"(int byteArraySize, byte[] bytesAdded, int toExtract, byte[] result) {
        given:
        ByteArrayNIO byteArray = new ByteArrayNIO(byteArraySize)
            byteArray.add(bytesAdded)

            byte[] extracted = byteArray.extract(toExtract)
        expect:
            Arrays.equals(extracted, result)
        where:
            byteArraySize                                                                       |   bytesAdded |   toExtract |   result
            1  |   []         |   0         |   []
            2  |   [1]        |   0         |   []
            3  |   [1, 2, 3]  |   2         |   [1, 2]
            10 |   [1, 2, 3]  |   3         |   [1, 2, 3]
    }

    /**
     * We tests that an Exception is thrown when trying to extract more bytes than actually stored in the ByteArray
     */
    def "testing capacity limits"() {
        given:
        ByteArrayConfig memoryConfig = new ByteArrayConfig(2)

        ByteArray byteArray = new ByteArrayNIO(memoryConfig.getByteArraySize())
        when:
            byteArray.add([1,2] as byte[])
            byteArray.add([3] as byte[])
        then:
            RuntimeException e = thrown()
            e.getMessage().startsWith("Not enough capacity")
    }

}
