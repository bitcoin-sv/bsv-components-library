/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.unit.network.streams

import io.bitcoinsv.jcl.net.network.PeerAddress
import io.bitcoinsv.jcl.net.network.streams.PeerOutputStream
import io.bitcoinsv.jcl.net.network.streams.StreamDataEvent
import spock.lang.Specification

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class PeerOutputStreamTest extends Specification {

    /**
     * Convenience method. It creates a Pipeline of 2 InputStreams: NumberToStringStream and StringToNumberStream
     * It sends the source data through them. As the data passes through the Streams it gets transformed, and the
     * final result is returned.
     */
    private List<Integer> runPipeline(ExecutorService executor, List<Integer> sourceData) {
        List<Integer> resultData = new ArrayList<>()

        // The Peer assigned to this Stream:
        PeerAddress peerAddress = PeerAddress.localhost(5050)

        // We create The Destination of this pipeline of PeerOutputsStream:
        PeerStreamInOutSimulator<Integer> destination = new PeerStreamInOutSimulator<>(peerAddress, executor)

        // We create our Pipeline of InputStreams:
        PeerOutputStream<Integer> stringToNumberStream = new StringNumberOutputStream(peerAddress, executor, destination)
        PeerOutputStream<String> numberToStringStream = new NumberStringOutputStream(peerAddress, executor, stringToNumberStream)

        destination.onData({e ->
            println("Adding " + e.getData() + " to the result list...")
            resultData.add(e.getData())
        })

        // We send data...
        for (Integer data : sourceData) { numberToStringStream.send(new StreamDataEvent<Integer>(data)) }

        // We wait a little bit until all te data has passed through the InputStream:
        Thread.sleep(1000)

        // And we return the result:
        return resultData
    }


    /**
     * We test that the transformation function works fine, and for each data sent, we receive the right result at
     * the destination.
     * NOTE: For the Stream to work property we need to provide an ExecutorService with just one Thread
     * (single Thread), otherwise we cannot guarantee that the results sent by the Output Stream are
     * coming out in the same order as we send them)
     */
    def "Testing Transformation Function"() {
        given:
            // A single-Thread executor
            ExecutorService executor = Executors.newSingleThreadExecutor()

            // We configure the source data...
            List<Integer> sourceData = new ArrayList<>()
            for (int i = 0; i < 20; i++) { sourceData.add(i) }
        when:
            List<Integer> resultData = runPipeline(executor, sourceData)
            // Now we compare the data sent and the data returned...
            boolean resultsMatch = sourceData.equals(resultData)
        then:
            resultsMatch
    }

    /**
     * We test here how the results change when you use different ExecutorServices in the InputStream.
     * When you use a single Thread, all the data is coming to the InputStream in the same order as its produced.
     * When you use multiple threads, then the data might come in different order, since every event received
     * by the input stream might be triggered by a different Thread.
     *
     * Every time the Source send data to the InputStream, it uses the ServiceExecutor to trigger an event that will
     * be received by the InputStream, along with the data. Depending on the way the ServiceExecutor is defined, the
     * order of the incoming events might be different.
     *
     * For example, when you send [1,2,3] using a Single Thread executor, The Source will send the numbers to the
     * InputStream in sequence, one after another. That's because the Source uses the Executor to trigger an Event
     * for each data and since the Executor is mono-thread, all the events are triggered in sequence, so they arrived
     * at the InputStream in sequence too.
     *
     * but when you send the same data using a Multi-thread Executor, each event is triggered in a Different Thread, so
     * since the order in which those events reach the InputStream is not defined, so they might arrived at different order.
     *
     * THE GOAL OF THIS TEST IS TO ACTUALLY PROVE THAT WITH MULTI_THREAD EXECUTORS, WE CANNOT TRUST THE BEHAVIOUR OF THE
     * INPUTSTREAM.
     *
     */

    def "Testing OutputStream Order"() {
        given:
            // We configure the source data...
            List<Integer> sourceData = new ArrayList<>()
            for (int i = 0; i < 20; i++) { sourceData.add(i) }
        when:
            println("Testing " + title + " ...")
            List<Integer> resultData = runPipeline(executor, sourceData)
            // Now we compare the data sent and the data returned...
            boolean resultsMatch = sourceData.equals(resultData)
        then:
            resultsMatch.equals(allInOrder)
        where:

            title                   | executor                                          |   allInOrder

            "Single Thread"         |   Executors.newSingleThreadExecutor()             |   true
            "2 Threads"             |   Executors.newFixedThreadPool(2)        |   false
            "Multiple Threads"      |   Executors.newCachedThreadPool()                 |   false
    }
}
