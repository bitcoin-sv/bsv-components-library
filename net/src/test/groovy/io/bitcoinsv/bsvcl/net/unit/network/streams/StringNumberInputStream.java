package io.bitcoinsv.bsvcl.net.unit.network.streams;

import io.bitcoinsv.bsvcl.net.network.PeerAddress;
import io.bitcoinsv.bsvcl.net.network.streams.PeerInputStream;
import io.bitcoinsv.bsvcl.net.network.streams.PeerInputStreamImpl;


import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Definition of a simple PeerInputStream, it takes Integers from the source and returns the same number in
 * String format and within brackets
 */

public class StringNumberInputStream extends PeerInputStreamImpl<Integer, String> {

    public StringNumberInputStream(PeerAddress peerAddress, PeerInputStream<Integer> source) {
        super(peerAddress, source);
    }

    @Override
    public List<String> transform(Integer dataEvent) {
        try { Thread.sleep(10);} catch (Exception e) {} // simulate real work
        String result = "[" + String.valueOf(dataEvent) + "]";
        System.out.println(">> StringNumberInputStream ::Receiving " + dataEvent + ", returning " + result);
        return List.of(result);
    }
}