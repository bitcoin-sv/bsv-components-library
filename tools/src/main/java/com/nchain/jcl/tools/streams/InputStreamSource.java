package com.nchain.jcl.tools.streams;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-03 16:50
 *
 * An InputStreamSource is an abstraction that represents the SOURCE of data of an InputStream, that is
 * this is the GENERATOR of the data/events that will eventually reach an InputStream.
 *
 * As a source of Data, the only capability of an InputStreamSource is to Create the events (Data/Close)
 * that will eventually be received by an InputStream, so the interface is the same as an OutputStream.
 *
 * This might deserve an explanation:
 *
 * if we are developing an application and we are working on the "Receiving" side of it, we only have to care
 * about defining the InputStreams and the logic triggered when we receive some data or when the stream is
 * closed (by providing callbacks for those events).
 * But when we are testing it, we need to create and send Data to it, so we can check our InputStream is working
 * as expected. So the InputStreamSource also works as anOutputStream from the "data generator" standpoint.
 */
public interface InputStreamSource<T> extends OutputStream<T>{}
