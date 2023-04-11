package io.bitcoinsv.bsvcl.net.unit.network.streams;



import io.bitcoinsv.bsvcl.net.network.PeerAddress;
import io.bitcoinsv.bsvcl.net.network.streams.PeerInputStream;
import io.bitcoinsv.bsvcl.net.network.streams.PeerInputStreamImpl;


import java.util.List;

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