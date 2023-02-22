package io.bitcoinsv.jcl.net.unit.network.streams;



import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.network.streams.PeerInputStream;
import io.bitcoinsv.jcl.net.network.streams.PeerInputStreamImpl;
import io.bitcoinsv.jcl.net.network.streams.StreamDataEvent;


import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Definition of a simple PeerInputStream, it takes Strings containing a number within brackets and returns the
 * number in Integer Format
 */

class NumberStringInputStream extends PeerInputStreamImpl<String, Integer> {
    public NumberStringInputStream(PeerAddress peerAddress, PeerInputStream<String> source) {
        super(peerAddress, source);
    }

    @Override
    public List<Integer> transform(String dataEvent) {
        try { Thread.sleep(10);} catch (Exception e) {} // simulate real work
        String data = dataEvent;
        Integer result = Integer.valueOf(data.substring(1, data.length() - 1));
        System.out.println(">> NumberStringInputStream ::Receiving " + dataEvent + ", returning " + result);
        return List.of(result);
    }
}