package com.nchain.jcl.tools.streams;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This is an implementation of an OutputStreamDestination.
 * It also implements the OutputStream. This means that this class can be used as an "outputStream", and can be linked
 * to another OutputStream forming a chain that will end/start with this OutputStreamDestination.
 *
 * - param T: Data type received by this DESTINATION.
 */
public class OutputStreamDestinationImpl<T> extends InputStreamImpl<T,T>
        implements OutputStream<T>, OutputStreamDestination<T> {
    public OutputStreamDestinationImpl(ExecutorService executor) {
        super(executor, null);
    }
    public List<StreamDataEvent<T>> transform(StreamDataEvent<T> data) {return Arrays.asList(data);}

    @Override
    public void send(StreamDataEvent<T> event)  {
        eventBus.publish(event);
    }
    @Override
    public void close(StreamCloseEvent event) {
        eventBus.publish(event);
    }

}
