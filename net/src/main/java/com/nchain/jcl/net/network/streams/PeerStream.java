package com.nchain.jcl.net.network.streams;

import com.nchain.jcl.base.tools.streams.Stream;


/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A PeerStream is similar to a "regular" Stream, but including the methods from "PeerStreamInfo":
 * - getPeerAdress()
 * - getState().
 *
 * Also, the input/output of a PeerStream now also contain those methods mentioned above, so instead of
 * "inputStream" and "OutputStream", we now have "PeerInputStream" and "PeerOutputStream"
 */
public interface PeerStream<T> extends Stream<T>, PeerStreamInfo {
    /** Returns the InputStream that allows us to react to data/events received */
    PeerInputStream<T> input();
    /** Returns the OutputStream that allows us to send data/events */
    PeerOutputStream<T> output();
}
