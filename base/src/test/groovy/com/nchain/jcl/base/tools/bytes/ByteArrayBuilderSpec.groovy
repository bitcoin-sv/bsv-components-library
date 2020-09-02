package com.nchain.jcl.base.tools.bytes

import spock.lang.Specification

class ByteArrayBuilderSpec extends Specification {

    /**
     * We check the complete behaviour of the Builder. Given some parameters, like the size of each
     * ByteArray etc, we add and extractReader some data, and we check that the result is right, and also that the internal
     * Buffers created by the Builder are correct in terms of the number of them and that their imlementations are also
     * correct (based on NIO)
     *
     * @param byteArraySize     Capacity of each ByteArray
     * @param toAdd             Byte of data to add
     * @param toExtract         Number of bytes to extractReader
     * @param _result           Expected result content of the extraction
     * @param _bufferSize       Expected internal buffer size of the builder after the extraction
     */
    def "adding and extracting data"(int byteArraySize,
                                     byte[] toAdd,
                                     int toExtract,
                                     byte[] _result,
                                     int _bufferSize) {
        given:
            ByteArrayMemoryConfiguration memoryConfig = ByteArrayMemoryConfiguration.builder()
                .byteArraySize(byteArraySize)
                .build();

            ByteArrayBuilder builder = new ByteArrayBuilder(memoryConfig);
            builder.add(toAdd)

            int bufferSize = builder.buffers.size()
            byte[] result = builder.extractReader(toExtract).getFullContent()

            builder.clear()
        expect:
            Arrays.equals(result, _result)
            bufferSize == _bufferSize
        where:
            byteArraySize | toAdd       | toExtract |   _result     | _bufferSize
            3             | [1,2,3,4,5] | 3         | [1,2,3]       | 2
            3             | [1,2,3,4,5] | 0         | []            | 2
            1             | [1,2,3,4,5] | 5         | [1,2,3,4,5]   | 5
    }

}
