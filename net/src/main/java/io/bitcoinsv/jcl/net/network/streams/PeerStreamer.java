package io.bitcoinsv.jcl.net.network.streams;

public interface PeerStreamer<T> {
    void send(T data);
    void close();
}
