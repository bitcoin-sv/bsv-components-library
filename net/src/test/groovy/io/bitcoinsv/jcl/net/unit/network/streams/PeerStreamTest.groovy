package io.bitcoinsv.jcl.net.unit.network.streams

import io.bitcoinsv.jcl.net.network.PeerAddress
import io.bitcoinsv.jcl.net.network.streams.PeerStream
import spock.lang.Specification

class PeerStreamTest extends Specification {

    def "testing Pipeline"() {
        given:
        // We create the Pipeline:
        // Peer assigned to this Pipeline:
        PeerAddress peerAddress = PeerAddress.localhost(new Random().nextInt())
        PeerStreamSimulator<Integer> finalStream = new PeerStreamSimulator<>(peerAddress)
        finalStream.init()

        // And now we create the Streams, from the Peer back to the Source:

        PeerStream<String> stringNumberStream = new StringNumberPeerStream(finalStream)
        stringNumberStream.init()
        PeerStream<Integer> numberStringStream = new NumberStringPeerStream(stringNumberStream)
        numberStringStream.init()

        // We keep track of the data coming out from the pipeline after all the transformations,
        // from both ends (data received at the BEGINNING and at the END of the pipeline)
        List<Integer> dataReceivedBeginning = new ArrayList<>()
        List<Integer> dataReceivedEnd = new ArrayList<>()

        // Now we link the callbacks, so the Streams store the data after passing thorugh them:
        numberStringStream.input().onData({data ->
            println("initialStream: Receiving " + data)
            dataReceivedBeginning.add(data)
        })
        finalStream.output().onData({data ->
            println("finalStream: Receiving " + data)
            dataReceivedEnd.add(data)
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
            numberStringStream.output().send(dataFromBegin.get(i))
            finalStream.input().send(dataFromEnd.get(i))
        }

        // We do some waiting so the data gets propagated through the pieline...
        Thread.sleep(1000)
        then:
        true
    }
}