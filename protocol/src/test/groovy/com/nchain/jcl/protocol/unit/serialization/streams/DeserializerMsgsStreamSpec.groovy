package com.nchain.jcl.protocol.unit.serialization.streams

import com.nchain.jcl.network.streams.PeerInputStream
import com.nchain.jcl.protocol.config.ProtocolConfig
import com.nchain.jcl.protocol.config.provided.ProtocolBSVMainConfig
import com.nchain.jcl.tools.config.RuntimeConfig

import com.nchain.jcl.tools.config.provided.RuntimeConfigDefault
import com.nchain.jcl.protocol.streams.DeserializerStream
import com.nchain.jcl.protocol.unit.tools.MsgTest
import com.nchain.jcl.tools.bytes.ByteArrayReader
import com.nchain.jcl.tools.bytes.HEX
import com.nchain.jcl.tools.streams.StreamDataEvent
import spock.lang.Specification

import java.time.Duration
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class DeserializerMsgsStreamSpec extends Specification {

    // We define some messages that wil be used in the Tests:

    final static String INV_MSG     = MsgTest.INV_MSG_HEX;
    final static String BIG_BLOCK   = MsgTest.BLOCK_MSG_HEX
    final static String IGNORE_MSG  = MsgTest.IGNORE_MSG_HEX
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
                    .maxWaitingTimeForBytesInRealTime(Duration.ofMillis(1000))
                    .build()

            // We set the protocol configuration (the network does not matter now, so we choose BSV for example)
            ProtocolConfig protocolConfig = new ProtocolBSVMainConfig()

            // The executor to manage the events:
            ExecutorService executor = Executors.newSingleThreadExecutor()

            // The source used to send Bytes to our Stream. This specific implementation will add artificial
            // Delays to the data, to simulate real network activity:
            PeerInputStream<ByteArrayReader> delaySource = MsgTest.getDummyDelayStreamSource(speedBytesPerSec);

            // Our Deserialized Stream: We check the number of Messages Deserialized, to check everything is fine
            AtomicInteger numMessages = new AtomicInteger()
            AtomicInteger numErrors = new AtomicInteger()
            DeserializerStream stream = new DeserializerStream(executor, delaySource, runtimeConfig, protocolConfig.getBasicConfig())
            stream.onData({e ->
                println( e.getData().getBody().getClass().simpleName + " received")
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

            Thread.sleep(5_000)
            println("\nnumMesages: " + numMessages.get() + ", numErrors: " + numErrors.get() + "(" + numErrorsThrown + " expected)")

        then:
            numMessages.get() == numMsgsOK
            numErrorsThrown == numErrors.get()
        where:

            // bigMsgsThreadhold            : size in Bytes. If a Message body is bigger, then its considered a BIG message and candidate for Real-Time Processing
            // allowedRealTimeProcessing    : If FALSE, the Stream will throw an exception if a Big Message is found
            // speedBytesPerSec             : Speed we are feeding the bytes into the Stream
            // numMsgssOK                   : expected of Messages deserialized properly
            // numErrorsThrown              : expected num of Errors triggered

            title         |   messages                                              |   bigMsgsThreshold    |   allowedRealTimeProcessing |    speedBytesPerSec | numMsgsOK |   numErrorsThrown

            // A Valid "short" Message. It will be process in normal mode.
            "Test 1"        |   [INV_MSG]                                           |   1000                |   true                      |     500             |   1       |   0

            // A valid "Big" Message, and the Stream is not allowed to do Real-time processing.
            "Test 2"        |   [BIG_BLOCK]                                         |   100                 |   false                     |     100             |   0       |   1

            // A Valid "Big" Message, and the Steam IS allowed to process it. The Big block will be processed in real-time and its content will be returned in batches. So here the
            // value of "numMsgsOK" will be the number of "Partial" messages returned, in this case 2 (1 block Header + 1 Batch of 2 TXs).
            "Test 2b"       |   [BIG_BLOCK]                                         |   100                 |   true                      |     100             |   2       |   0

            // A Valid "Big" and Slow message, but the Stream is NOT allowed to process it.
            "Test 2c"      |   [BIG_BLOCK]                                           |   100                |   false                     |     500             |   0       |   1

            // A valid "Big" message, but the speed is too low (timeout will be triggered)
            "Test 2d"      |   [BIG_BLOCK]                                           |   100                |   true                      |     10              |   0       |   1

            // An "unknown" "short" Message
            "Test 3"      |   [IGNORE_MSG]                                           |   1000               |   false                     |     500             |   0       |   0

            // An "unknown" "big" Message
            "Test 3b"      |   [IGNORE_MSG]                                          |   10                 |   false                     |     500             |   0       |   0

            // An "Unknown message and a "big" message after
            "Test 3c"      |   [IGNORE_MSG, BIG_BLOCK]                               |   100                | true                        |     500             |   2       |   0

            // A series of Unknonw, Big, Normal, Big
            "Test 4"      |   [IGNORE_MSG, BIG_BLOCK, INV_MSG, BIG_BLOCK]            |   100                | true                        |     500             |   5       |   0



    }
}
