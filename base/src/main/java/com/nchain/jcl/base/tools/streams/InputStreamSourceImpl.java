package com.nchain.jcl.base.tools.streams;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This is an implementation of an InputStreamSource.
 * It also implements the InputStream. This means that this class can be used as an "inputStream", and can be linked
 * to another InputStream forming a chain that will end/start with this InputStreamSource.
 *
 * - param T: Data type produced by this SOURCE.
 */
public class InputStreamSourceImpl<T> extends InputStreamImpl<T,T> implements InputStream<T>, InputStreamSource<T> {

    public InputStreamSourceImpl(ExecutorService executorService) {
        super(executorService, null); // no source
    }
    @Override
    protected void linkSource(InputStream<T> source) {
        // In the parent InputStreamImpl class, this method links the "source" to this instance, so this instance gets
        // notified when the "oruce" generates en Event (onData/onClose).
        // In this case there is no further source here, so we don´´ link anything
    }

    @Override
    public List<StreamDataEvent<T>> transform(StreamDataEvent<T> data) {throw new UnsupportedOperationException();}

    @Override
    public void send(StreamDataEvent<T> event) {
        eventBus.publish(event);
    }
    @Override
    public void close(StreamCloseEvent event) {
        eventBus.publish(event);
    }

}
