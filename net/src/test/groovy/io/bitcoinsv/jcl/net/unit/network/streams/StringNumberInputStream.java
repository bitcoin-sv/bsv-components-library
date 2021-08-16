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
 * Definition of a simple PeerInputStream, it takes Integers from the source and returns the same number in
 * String format and within brackets
 */

public class StringNumberInputStream extends PeerInputStreamImpl<Integer, String> {

    public StringNumberInputStream(PeerAddress peerAddress, ExecutorService executor, PeerInputStream<Integer> source) {
        super(peerAddress, executor, source);
    }

    @Override
    public List<StreamDataEvent<String>> transform(StreamDataEvent<Integer> dataEvent) {
        try { Thread.sleep(10);} catch (Exception e) {} // simulate real work
        String result = "[" + String.valueOf(dataEvent.getData()) + "]";
        System.out.println(">> StringNumberInputStream ::Receiving " + dataEvent.getData() + ", returning " + result);
        return Arrays.asList(new StreamDataEvent<String>(result));
    }
}