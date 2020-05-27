package com.nchain.jcl.tools.bytes

import spock.lang.Specification

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer

class ByteArrayReaderWriterSpec extends Specification {

    /**
     * We test that when a ByteArrayReader and a ByteArrayWriter are linked together, the data we write and read is
     * consistent.
     */
    def "Testing reading and writing"() {
        given:
             ByteArrayWriter writer = new ByteArrayWriter();
            // Values to test:
            byte w_aByte = (byte)1
            byte[] w_vector = [2,3,4,5,6,7,6,5,4] as byte[]
            boolean w_bool = true;
            String w_str = "example"
            int w_lengthStr = 5
            String w_strTrim = w_str.substring(0, w_lengthStr).trim()
            long w_uint32 = 22
            long w_uint64 = 33

            // We also keep track of the internal Buffers in both the Writer and the Reader as they read and write...
            int numBuffersWriterAfterWriting
            int numBuffersWriterAfterReading
            int numBuffersReaderBeforeReading
            int numBuffersReaderAfterReading

        when:
            // We write data...
            writer.write(w_aByte)
            writer.write(w_vector)
            writer.writeBoolean(w_bool)
            writer.writeStr(w_str)
            writer.writeStr(w_str, w_lengthStr)
            writer.writeUint32LE(w_uint32)
            writer.writeUint64LE(w_uint64)

            numBuffersWriterAfterWriting = writer.builder.buffers.size()

            // Now we read the data
            ByteArrayReader reader = writer.reader()
            numBuffersReaderBeforeReading = reader.builder.buffers.size()

            byte r_aByte = reader.read()
            byte[] r_vector = reader.read(w_vector.length)

            boolean r_bool = reader.readBoolean()
            String r_str = reader.readString(w_str.length(), "UTF-8")
            String r_strTrim = reader.readString(w_lengthStr, "UTF-8")
            long r_uint32 = reader.readUint32()
            long r_uint64 = reader.readInt64LE()

            numBuffersWriterAfterReading = writer.builder.buffers.size()
            numBuffersReaderAfterReading = reader.builder.buffers.size()

        then:
            // We check that both sets of data (read wnd writen) are the same
            r_aByte == w_aByte
            Arrays.equals(r_vector, w_vector)
            r_bool == w_bool
            r_str.equals(w_str)
            r_strTrim.equals(w_strTrim)
            r_uint32 == w_uint32
            r_uint64 == w_uint64

            // We also test that, after the reading, al the internal buffers in the Writer are gone, and now we have
            // the same number of internal buffers within the reader (since we have read the same information that
            // was written before)

            numBuffersWriterAfterWriting > 0
            numBuffersReaderBeforeReading == numBuffersWriterAfterWriting
            numBuffersReaderAfterReading == 0
            numBuffersWriterAfterReading == 0
    }

    /**
     * In this example, we use a ByteArrayWriter and a ByteArrayReader, linked together, so what the Writer writes
     * can be read by the Reader. The point here is that they run in different Threads.
     * We check the data read by the Reader is the same as the one written by the Writer
     */
    def "Reading and Writing in multiThread"() {
        given:

            // These flags how if the callbacks have been triggered:

            // We create the memory Manager and link the callbacks
            ByteArrayMemoryConfiguration memoryConfig = ByteArrayMemoryConfiguration.builder()
                .byteArraySize(3)
                .build();


            // We configure the data to be used for the Tests: We use here Strings...
            int NUM_ITEMS_TO_WRITE = 20;
            String[] DATA = new String[NUM_ITEMS_TO_WRITE];
            for (int i = 0; i < DATA.length; i++) DATA[i] = "Testing value number " + i;

        when:
            try {
                // We define 2 Workers, each one will write content , and the other one will read the same content, but
                // at different rate....
                ByteArrayWriter writer = new ByteArrayWriter()
                ByteArrayReader reader = new ByteArrayReader(writer)

                Runnable writerWorker = {
                    try {
                        for (int i = 0 ; i < DATA.length; i++) {
                            writer.writeStr(DATA[i])
                            println("writing data, buffer size:" + writer.builder.size())
                            Thread.sleep(500)
                        }
                    } catch (InterruptedException e) {}}

                Runnable readerWorker = {
                    try {
                        for (int i = 0 ; i < DATA.length; i++) {
                            // In order to make sure that the readerWorker starts AFTER the writerWorker, we
                            // put the Thread.sleep at the beginning of the loop...
                            Thread.sleep(1000)
                            String value = reader.readString(DATA[i].length(), "UTF-8")
                            println("reading data ('" + value + "'), buffer size:" + reader.builder.size())
                            if (!value.equals(DATA[i])) throw new Exception("Value read is wrong!")
                        }
                        Thread.sleep(1000)
                    } catch (InterruptedException e) {}
                }

                ExecutorService executor = Executors.newFixedThreadPool(2)
                executor.submit(writerWorker)
                executor.submit(readerWorker)
                executor.awaitTermination(NUM_ITEMS_TO_WRITE * 1000 + 5000, TimeUnit.MILLISECONDS)
                executor.shutdownNow()
            } catch (InterruptedException ie) {}
            ByteArrayTestUtils.forceGC()
        then:
            noExceptionThrown()
    }
}
