/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
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
    public NumberStringInputStream(PeerAddress peerAddress, ExecutorService executor, PeerInputStream<String> source) {
        super(peerAddress, executor, source);
    }

    @Override
    public List<StreamDataEvent<Integer>> transform(StreamDataEvent<String> dataEvent) {
        try { Thread.sleep(10);} catch (Exception e) {} // simulate real work
        String data = dataEvent.getData();
        Integer result = Integer.valueOf(data.substring(1, data.length() - 1));
        System.out.println(">> NumberStringInputStream ::Receiving " + dataEvent.getData() + ", returning " + result);
        return Arrays.asList(new StreamDataEvent<Integer>(result));
    }
}