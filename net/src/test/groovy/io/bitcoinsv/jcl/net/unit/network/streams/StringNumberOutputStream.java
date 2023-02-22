package io.bitcoinsv.jcl.net.unit.network.streams;



import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.network.streams.PeerOutputStream;
import io.bitcoinsv.jcl.net.network.streams.PeerOutputStreamImpl;
import io.bitcoinsv.jcl.net.network.streams.StreamDataEvent;


import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * We define an outputStream that takes an String containing a number within brackets and transforms it into its
 * Number representation before sending it to its Destination
 */
class StringNumberOutputStream extends PeerOutputStreamImpl<String, Integer> {
    public StringNumberOutputStream(PeerAddress peerAddress, PeerOutputStream<Integer> destination) {
        super(peerAddress, destination);
    }
    @Override
    public List<Integer> transform(String dataEvent) {
        try { Thread.sleep(10);} catch (Exception e) {} // simulate real work
        String data = dataEvent;
        Integer result = Integer.valueOf(data.substring(1, data.length() - 1));
        System.out.println(">> StringNumberOutputStream ::Receiving " + dataEvent + ", sending " + result);
        return List.of(result);
    }
}