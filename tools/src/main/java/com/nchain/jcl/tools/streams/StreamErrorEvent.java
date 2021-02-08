package com.nchain.jcl.tools.streams;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This event represent an error thrown by an Stream, which most probably has been thrown during
 * the transformation function.
 */
public class StreamErrorEvent extends StreamEvent {
    Throwable exception;
    public StreamErrorEvent(Throwable exception)    { this.exception = exception; }
    public Throwable getException()                 { return this.exception; }
}
