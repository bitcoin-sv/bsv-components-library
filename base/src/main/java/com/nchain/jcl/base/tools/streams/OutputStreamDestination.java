package com.nchain.jcl.base.tools.streams;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-04 16:16
 *
 * An OutputStreamDestination is an abstraction that represents the final Destination of the data/events
 * sent by an OutputStream.
 *
 * As a destination of Data, the only capability of an OutputStreamDestination is to Receive the events (Data/Close)
 * that have been sent by an OututStream, so the interface is the same as an InputStream.
 *
 * This might deserve an explanation:
 *
 * if we are developing an application and we are working on the "Sending" side of it, we only have to care
 * about defining the OutputStreams and sending the data/events through it.
 * But when we are testing it, we need to check whether the data is arriving at the destination properly, so we need
 * to LISTEN to those events in the Destination. This "listening" is exactly what an InputStream does, so
 * the OutputStreamDestination also works as an InputStream when we are testing it.
 */
public interface OutputStreamDestination<T> extends InputStream<T>{}
