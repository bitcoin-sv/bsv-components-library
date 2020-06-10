package com.nchain.jcl.tools.streams

import spock.lang.Specification

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class OutputStreamTest extends Specification {

    /**
     * We define an outputStream that takes an Integer, and transforms it into a String before sending it
     * to its Destination
     */

    class NumberToStringConverter extends OutputStreamImpl<Integer, String> {
        NumberToStringConverter(ExecutorService executor, OutputStream<String> destination) {
            super(executor, destination)
        }
        @Override
        List<StreamDataEvent<String>> transform(StreamDataEvent<Integer> data) {
            try { Thread.sleep(10);} catch (Exception e) {} // simulate real work
            return Arrays.asList(new StreamDataEvent<>(data.getData()))
        }
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
            ExecutorService executor = Executors.newSingleThreadExecutor()

            // In one list we store the numbers we are sending through the source, in the other we store the results:
            List<Integer> sentData = new ArrayList<>()
            List<String> destinationData = new ArrayList<>()

            // We crate our Destination,and some callbacks to store the result that arrive:
            OutputStreamDestination<String> destination = new OutputStreamDestinationImpl<>(executor)
            destination.onData({e -> destinationData.add(e.getData())})

            // We create our Output Stream:
            OutputStream<String> myOutputStream = new NumberToStringConverter(executor, destination)

        when:
            // We send data...
            for (int i = 0; i < 20; i++) {
                sentData.add(i)
                myOutputStream.send(new StreamDataEvent<Integer>(i))
            }

            // We wait a little bit until all te data has passed through the InputStream:
            Thread.sleep(1000)

            // Now we compare the data sent and the data returned...
            boolean resultsMatch = sentData.size() == destinationData.size()
            if (resultsMatch) {
                for (int i = 0; i < sentData.size(); i++)
                    resultsMatch = resultsMatch && sentData.get(i).equals(Integer.valueOf(destinationData.get(i)))
            }

        then:
            resultsMatch
    }

    /**
     * We test here how the results change when you use different ExecutorServices in the OutputStream.
     * When you use a single Thread, all the data is coming out of the OutputStream in the same order as it was sent.
     * When you use multiple threads, then the data might come out in different order, since every event triggered
     * by the outputStream might be triggered by a different Thread.
     *
     * Every time we send data to the OutputStream, it uses the ServiceExecutor to trigger an event that will
     * be sent to its destination, along with the data. Depending on the way the ServiceExecutor is defined, the
     * order of the outcoming events might be different.
     *
     * For example, when you send [1,2,3] using a Single Thread executor, the OutputStreamwill process them in sequence,
     * it will transform them, and then it will send the results to its destination in sequence as well. That's because
     * the OutputStream uses the Executor to trigger an Event for each transformed data and since the Executor is
     * mono-thread, all the events are triggered in sequence, so they arrived at the Destination in sequence too.
     *
     * But when you run the OutputStream using a Multi-thread Executor, each event is triggered in a Different Thread, so
     * since the order in which those events reach the Destination is not defined, so they might arrived at different order.
     *
     * THE GOAL OF THIS TEST IS TO ACTUALLY PROVE THAT WITH MULTI_THREAD EXECUTORS, WE CANNOT TRUST THE BEHAVIOUR OF THE
     * OUTPUTSTREAM.
     *
     */
    def "Testing OutputStream Order"() {

        given:

            // In one list we store the numbers we are sending through the source, in the other we store the results:
            List<Integer> sentData = new ArrayList<>()
            List<String> destinationData = new ArrayList<>()

            // We crate our Destination,and some callbacks to store the result that arrive:
            OutputStreamDestination<String> destination = new OutputStreamDestinationImpl<>(executor)
            destination.onData({e -> destinationData.add(e.getData())})

            // We create our Output Stream:
            OutputStream<String> myOutputStream = new NumberToStringConverter(executor, destination)

        when:
            // We send data...
            for (int i = 0; i < 20; i++) {
                sentData.add(i)
                myOutputStream.send(new StreamDataEvent<Integer>(i))
            }

            // We wait a little bit until all te data has passed through the InputStream:
            Thread.sleep(1000)

            // Now we compare the data sent and the data returned...
            boolean resultsMatch = sentData.size() == destinationData.size()
            if (resultsMatch) {
                for (int i = 0; i < sentData.size(); i++)
                    resultsMatch = resultsMatch && sentData.get(i).equals(Integer.valueOf(destinationData.get(i)))
            }

        then:
            resultsMatch.equals(allInOrder)

        where:

            title                   | executor                                          |   allInOrder

            "Single Thread"         |   Executors.newSingleThreadExecutor()             |   true
            "2 Threads"             |   Executors.newFixedThreadPool(2)        |   false
            "Multiple Threads"      |   Executors.newCachedThreadPool()                 |   false
    }


}
