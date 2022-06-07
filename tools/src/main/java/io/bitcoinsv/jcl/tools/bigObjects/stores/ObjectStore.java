package io.bitcoinsv.jcl.tools.bigObjects.stores;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An ObjectStore is a simple Store that can save different Objects and it provides very simple operations (save,
 * remove, etc).
 *
 * general Rules:
 * - You need to invoke "start()" before you can use it, and you call "stop()" when you are done.
 * - The "destroy" method releases the resources used by the Store.
 * - If you want to keep using the store after invoking "stop()" or "destroy()" you need to call "start()" again.
 *
 * @param <T> Class of the Object to store
 */
public interface ObjectStore<T> {
    void start();
    void stop();
    void save(String objectId, T object);
    void remove(String objectId);
    T get(String objectId);
    void clear();
    void destroy();
    // for performance/cleaning operations, implementation dependent:
    default void compact() {}
}