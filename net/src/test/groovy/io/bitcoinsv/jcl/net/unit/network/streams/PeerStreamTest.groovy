/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.unit.network.streams

import io.bitcoinsv.jcl.net.network.PeerAddress
import io.bitcoinsv.jcl.net.network.streams.PeerStream
import io.bitcoinsv.jcl.net.network.streams.StreamDataEvent
import spock.lang.Specification

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class PeerStreamTest extends Specification {

    def "testing Pipeline"() {
        given:
            ExecutorService executor = Executors.newSingleThreadExecutor()

            // We create the Pipeline:
            // Peer assigned to this Pipeline:
            PeerAddress peerAddress = PeerAddress.localhost(new Random().nextInt())
            PeerStreamSimulator<Integer> finalStream = new PeerStreamSimulator<>(peerAddress, executor)
            finalStream.init()

            // And now we create the Streams, from the Peer back to the Source:

            PeerStream<String> stringNumberStream = new StringNumberPeerStream(executor, finalStream)
            stringNumberStream.init()
            PeerStream<Integer> numberStringStream = new NumberStringPeerStream(executor, stringNumberStream)
            numberStringStream.init()

            // We keep track of the data coming out from the pipeline after all the transformations,
            // from both ends (data received at the BEGINNING and at the END of the pipeline)
            List<Integer> dataReceivedBeginning = new ArrayList<>()
            List<Integer> dataReceivedEnd = new ArrayList<>()

            // Now we link the callbacks, so the Streams store the data after passing thorugh them:
            numberStringStream.input().onData({e ->
                println("initialStream: Receiving " + e.getData())
                dataReceivedBeginning.add(e.getData())
            })
            finalStream.output().onData({e ->
                println("finalStream: Receiving " + e.getData())
                dataReceivedEnd.add(e.getData())
            })

        when:

            // This is the list of Source data that we are Sending FROM THE START OF THE PIPELINE
            List<Integer> dataFromBegin = new ArrayList<>()
            for (int i = 0; i < 2; i++) dataFromBegin.add(i)

            // This is the list of Source data that we are Sending FROM THE END OF THE PIPELINE
            List<Integer> dataFromEnd = new ArrayList<>()
            for (int i = 0; i < 2; i++) dataFromEnd.add(i*20)

            // Now we send data, from both ends...
            for (int i = 0; i < 2; i++) {
                numberStringStream.output().send(new StreamDataEvent<Integer>(dataFromBegin.get(i)))
                finalStream.input().send(new StreamDataEvent<Integer>(dataFromEnd.get(i)))
            }

            // We do some waiting so the data gets propagated through the pieline...
            Thread.sleep(1000)
        then:
            true
    }
}
