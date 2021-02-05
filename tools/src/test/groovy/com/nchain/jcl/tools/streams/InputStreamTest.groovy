package com.nchain.jcl.tools.streams

import spock.lang.Specification

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class InputStreamTest extends Specification {

    /**
     * Definition of a simple InputStream, it takes Integers from a source and returns te same number in
     * String format
     */

    class NumberToStringConverter extends InputStreamImpl<Integer, String> {
        NumberToStringConverter(ExecutorService executor, InputStream<Integer> source) {
            super(executor, source)
        }
        @Override
        List<StreamDataEvent<String>> transform(StreamDataEvent<Integer> dataEvent) {
            try { Thread.sleep(10);} catch (Exception e) {} // simulate real work
            return Arrays.asList(new StreamDataEvent<String>(String.valueOf(dataEvent.getData())))
        }
    }

    /**
     * We test that the transformation function works fine, and for each input we get the right output.
     * NOTE: For the Stream to work property we need to provide an ExecutorService with just one Thread
     * (single Thread), otherwise we cannot guarantee that the results returned by the Input Stream are
     * coming in the same order as the source is producing them)
     */
    def "Testing Transformation Function"() {
        given:
            ExecutorService executor = Executors.newSingleThreadExecutor()

            // In one list we store the numbers we are sending through the source, in the other we store the results:
            List<Integer> sourceData = new ArrayList<>()
            List<String> resultData = new ArrayList<>()

            // We create our Source:
            InputStreamSource<Integer> source = new InputStreamSourceImpl<>(executor)

            // We create our Input Stream. We add some callbacks to store the results:
            InputStream<String> myInputStream = new NumberToStringConverter(executor, source)
            myInputStream.onData({e -> resultData.add(e.getData())})

        when:
            // We send data...
            for (int i = 0; i < 20; i++) {
                sourceData.add(i)
                source.send(new StreamDataEvent<Integer>(i))
            }

            // We wait a little bit until all te data has passed through the InputStream:
            Thread.sleep(1000)

            // Now we compare the data sent and the data returned...
            boolean resultsMatch = sourceData.size() == resultData.size()
            if (resultsMatch) {
                for (int i = 0; i < sourceData.size(); i++)
                    resultsMatch = resultsMatch && sourceData.get(i).equals(Integer.valueOf(resultData.get(i)))
            }

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
    def "Testing InputStream Order"() {

        given:

            // In one list we store the numbers we are sending through the source, in the other we store the results:
            List<Integer> sourceData = new ArrayList<>()
            List<String> resultData = new ArrayList<>()

            // We create our Source:
            InputStreamSource<Integer> source = new InputStreamSourceImpl<>(executor)

            // We create our Input Stream. We add some callbacks to store the results:
            InputStream<String> myInputStream = new NumberToStringConverter(executor, source)
            myInputStream.onData({e -> resultData.add(e.getData())})

        when:
            int NUM_DATA = 20;
            println("Testing " + title + " ...")
            // We send data...
            for (int i = 0; i < NUM_DATA; i++) {
                sourceData.add(i)
                source.send(new StreamDataEvent<Integer>(i))
            }

            // we print the data sent:
            println("Data sent to the Stream:")
            sourceData.forEach({e -> print(e + " ")})
            println("")

            // We wait a little bit until all te data has passed through the InputStream:
            Thread.sleep(1000)

            // Now we compare the data sent and the data returned...
            println("Data Received after being transformed by the Stream:")
            resultData.forEach({e -> print(e + " ")})
            println("")

            boolean resultsMatch = sourceData.size() == resultData.size()
            if (resultsMatch) {
                for (int i = 0; i < sourceData.size(); i++)
                    resultsMatch = resultsMatch && sourceData.get(i).equals(Integer.valueOf(resultData.get(i)))
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
