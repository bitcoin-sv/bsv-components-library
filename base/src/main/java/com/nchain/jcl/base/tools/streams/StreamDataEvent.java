package com.nchain.jcl.base.tools.streams;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This event represent a piece of data received by an InputStream, or sent by an OutputStream.
 * - param T: Data type retrieved/sent
 */
@AllArgsConstructor
public class StreamDataEvent<T> extends StreamEvent {
    @Getter
    T data;
}
