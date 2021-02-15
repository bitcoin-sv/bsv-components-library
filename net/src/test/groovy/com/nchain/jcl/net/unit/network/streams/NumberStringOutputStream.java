package com.nchain.jcl.net.unit.network.streams;



import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.net.network.streams.PeerOutputStream;
import com.nchain.jcl.net.network.streams.PeerOutputStreamImpl;
import com.nchain.jcl.net.network.streams.StreamDataEvent;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * We define an outputStream that takes an Integer, and transforms it into a String within brackets
 * before sending it to its Destination
 */
public class NumberStringOutputStream extends PeerOutputStreamImpl<Integer, String> {
    public NumberStringOutputStream(PeerAddress peerAddress, ExecutorService executor, PeerOutputStream<String> destination) {
        super(peerAddress, executor, destination);
    }
    @Override
    public List<StreamDataEvent<String>> transform(StreamDataEvent<Integer> dataEvent) {
        try { Thread.sleep(10);} catch (Exception e) {} // simulate real work
        String result = "[" + String.valueOf(dataEvent.getData()) + "]";
        System.out.println(">> NumberStringOutputStream ::Receiving " + dataEvent.getData() + ", sending " + result);
        return Arrays.asList(new StreamDataEvent<>(result));
    }
}