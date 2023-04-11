package io.bitcoinsv.bsvcl.net.network.streams;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This event represents a piece of data received by an InputStream, or sent by an OutputStream.
 * - param T: Data type retrieved/sent
 */
public class StreamDataEvent<T> extends StreamEvent {
    T data;
    public StreamDataEvent(T data)  { this.data = data; }
    public T getData()              { return this.data; }
}
