package com.nchain.jcl.protocol.unit.serialization.streams

import com.nchain.jcl.protocol.config.ProtocolConfig
import com.nchain.jcl.protocol.config.RuntimeConfig
import com.nchain.jcl.protocol.config.provided.ProtocolBSVMainConfig
import com.nchain.jcl.protocol.config.provided.RuntimeConfigDefault
import com.nchain.jcl.protocol.serialization.streams.DeserializerStream
import com.nchain.jcl.protocol.unit.tools.ByteReaderDelaySource
import com.nchain.jcl.tools.bytes.ByteArrayReader
import com.nchain.jcl.tools.bytes.HEX
import com.nchain.jcl.tools.streams.StreamDataEvent
import spock.lang.Specification

import java.time.Duration
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class DeserializerStreamTest extends Specification {

    // Definition of some Messages:
    final static String PING_MSG   = "e3e1f3e870696e6700000000000000000800000032ab095c3d9a9cb22d32b40b"
    final static String IGNORE_MSG = "e3e1f3e870696e670aa00000000000000800000032ab095c3d9a9cb22d32b40b"
    final static String INV_MSG    = "e3e1f3e8696e7600000000000000000025000000e27152ce0101000000a69d45e7abc3b8fc363d13b88aaa2f2ec62bf77b6881e8bd7bd1012fd81d802b"

    /**
     * We test multiple Scenarios:
     * - Different Message Size: "Normal", "Big"
     * - Errors thrown when the message is "Big" but the Stream is NOT allowed to do Real-time processing.
     * - Errors when performing Real-time processing and simulating slow network
     */
    def "Testing Real-Time Deserialization"() {
        given:

            // We configure the Threshold for a Message to be considered "Big":
            RuntimeConfig runtimeConfig = new RuntimeConfigDefault().toBuilder()
                    .msgSizeInBytesForRealTimeProcessing(bigMsgsThreshold)
                    .build()

            // We set the protocol configuration (the network does not matter now, so we chooese BSV for example)
            ProtocolConfig protocolConfig = new ProtocolBSVMainConfig()

            // The executor to manage the events:
            ExecutorService executor = Executors.newSingleThreadExecutor()

            // The source used to send Bytes to our Stream. This specific implementation will add artificial
            // Delays to the data, to simulate real network activity:
            ByteReaderDelaySource delaySource = new ByteReaderDelaySource(executor, speedBytesPerSec)

            // Our Deserialized Stream: We check the number of Messages Deserialized, to check everything is fine
            AtomicInteger numMessages = new AtomicInteger()
            AtomicInteger numErrors = new AtomicInteger()
            DeserializerStream stream = new DeserializerStream(executor, delaySource, runtimeConfig, protocolConfig)
            stream.onData({e ->
                println("Msg received")
                numMessages.incrementAndGet()
            })
            stream.onError({e ->
                println("Error received")
                numErrors.incrementAndGet()
            })

            // We activate the Stream, to allow it to process Big Messages:
            stream.setRealTimeProcessingEnabled(allowedRealTimeProcessing)

        when:
            println("\nTesting " + title + " ...")
            for (int i = 0; i < messages.size(); i++) {
                delaySource.send(new StreamDataEvent<ByteArrayReader>(new ByteArrayReader(HEX.decode(messages.get(i)))))
            }
            //  Since we are feeding our Stream using artificial delays, we need to do some waiting here to make sure all
            // the bytes have been sent...

            executor.awaitTermination(1, TimeUnit.SECONDS)
            executor.shutdownNow()

        then:
            numMessages.get() == numMsgsOK
            numErrorsThrown == numErrors.get()
        where:

            // bigMsgsThreadhold            : size in Bytes. If a Message body is bigger, then its considered a BIG message and candidate for Real-Time Processing
            // allowedRealTimeProcessing    : If FALSE, the Stream will throw an exception if a Big Message is found
            // speedBytesPerSec             : Speed we are feeding the bytes into the Stream
            // numMsgssOK                   : expected of Messages deserialized properly
            // numErrorsThrown              : expected num of Errors triggered

            title         |   messages                  |   bigMsgsThreshold  |   allowedRealTimeProcessing   |    speedBytesPerSec | numMsgsOK |   numErrorsThrown

            // A Valid "short" Message
            "Test 1"      |   [PING_MSG]                |   20                |   true                        |     500             |   1         |   0

            // A valid "short" message, and the Strema is not allowed to do Real-time processing, and the network is slow (both should not matter)
            "Test 1b"     |   [PING_MSG]                |   20                |   false                       |     100             |   1         |   0

            // A Valid "Big" Message
            "Test 2"      |   [INV_MSG]                 |   20                |   true                        |     500             |   1         |   0

            // A Valid "Big" message, but the Stream is NOT allowed to process it.
            "Test 2a"      |   [INV_MSG]                |   20                |   false                       |     500             |   0         |   1

            // A valid "Big" message, but the speed is too low
            "Test 2b"      |   [INV_MSG]                |   20                |   true                        |     100             |   0         |   1

            // An "unknown" Message
            "Test 3"      |   [IGNORE_MSG]              |   20                |   false                       |     500             |   0         |   0

            // An "Unknown message and a "big" message after
            "Test 3b"      |   [IGNORE_MSG, INV_MSG]    |   20                  | true                        |     500             |   1         |   0

            // 1 "Small" and 1 "Big" message, the Stream IS allowed to process "Big" messages
            "Test 4"      |   [PING_MSG, INV_MSG]       |   20                |   true                        |     500             |   2         |   0

            // 1 "small" and 1 "Big" messags, the Stream is NOT allowed to process "Big" messages.
            "Test 4a"      |   [PING_MSG, INV_MSG]      |   20                |   false                       |     500             |   1         |   1

            // 1 "big" and 1 "small" mesages. The Stream is NOT allowed to process Big Messages. Since the Big messages now comes first, it
            // generates an error, so the second message is not even processed (after the first error, the Stream stops processing incoming bytes)
            "Test 4b"      |   [INV_MSG, PING_MSG]      |   20                |   false                       |     500             |   0         |   1

    }
}
