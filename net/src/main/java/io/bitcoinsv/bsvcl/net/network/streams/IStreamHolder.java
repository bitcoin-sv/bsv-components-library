package io.bitcoinsv.bsvcl.net.network.streams;

/**
 * Stream holder is just a wrapper around object being written to provide easier write locking around it.
 *
 * @param <T> data type being sent using stream holder
 */
public interface IStreamHolder<T> {
    void send(T data);
    void close();
}
