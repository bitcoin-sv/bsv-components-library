package com.nchain.jcl.tools.bytes

import groovy.util.logging.Slf4j
import spock.lang.Specification


@Slf4j
class ByteArrayImplSpec extends Specification {

    /**
     * We test the content of the Byte Array is correct according to the parameters:
     *
     * @param bytesAdded    Bytes to add to the ByteArray
     * @param toExtract     Number of Bytes to extractReader from the ByteArray
     * @param result        Result expected in the extraction
     */
    def "testing adding and extracting"(int byteArraySize, byte[] bytesAdded, int toExtract, byte[] result) {
        given:
            ByteArrayImpl byteArray = new ByteArrayImpl(byteArraySize)
            byteArray.add(bytesAdded)

            byte[] extracted = byteArray.extract(toExtract)
        expect:
            Arrays.equals(extracted, result)
        where:
            byteArraySize                                                                       |   bytesAdded |   toExtract |   result
            ByteArrayMemoryConfiguration.builder().byteArraySize(1).build().getByteArraySize()  |   []         |   0         |   []
            ByteArrayMemoryConfiguration.builder().byteArraySize(2).build().getByteArraySize()  |   [1]        |   0         |   []
            ByteArrayMemoryConfiguration.builder().byteArraySize(3).build().getByteArraySize()  |   [1, 2, 3]  |   2         |   [1, 2]
            ByteArrayMemoryConfiguration.builder().byteArraySize(10).build().getByteArraySize() |   [1, 2, 3]  |   3         |   [1, 2, 3]
    }

    /**
     * We tests that an Exception is thrown when trying to extract more bytes than actually stored in the ByteArray
     */
    def "testing capacity limits"() {
        given:
            ByteArrayMemoryConfiguration memoryConfig = ByteArrayMemoryConfiguration.builder()
                .byteArraySize(2)
                .build()

            ByteArray byteArray = new ByteArrayImpl(memoryConfig.getByteArraySize())
        when:
            byteArray.add([1,2] as byte[])
            byteArray.add([3] as byte[])
        then:
            RuntimeException e = thrown()
            e.getMessage().startsWith("Not enough capacity")
    }

}
