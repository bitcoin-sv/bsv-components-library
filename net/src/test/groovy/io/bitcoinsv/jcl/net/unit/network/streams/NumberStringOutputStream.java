package io.bitcoinsv.jcl.net.unit.network.streams;



import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.network.streams.PeerOutputStream;
import io.bitcoinsv.jcl.net.network.streams.PeerOutputStreamImpl;

import java.util.Arrays;
import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * We define an outputStream that takes an Integer, and transforms it into a String within brackets
 * before sending it to its Destination
 */
public class NumberStringOutputStream extends PeerOutputStreamImpl<Integer, String> {
    public NumberStringOutputStream(PeerAddress peerAddress, PeerOutputStream<String> destination) {
        super(peerAddress, destination);
    }
    @Override
    public List<String> transform(Integer dataEvent) {
        try { Thread.sleep(10);} catch (Exception e) {} // simulate real work
        String result = "[" + dataEvent + "]";
        System.out.println(">> NumberStringOutputStream ::Receiving " + dataEvent + ", sending " + result);
        return List.of(result);
    }
}