package com.nchain.jcl.base.tools.streams;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This event represent an error thrown by an Stream, which most probably has been thrown during
 * the transformation function.
 */
@AllArgsConstructor
public class StreamErrorEvent extends StreamEvent {
    @Getter
    Throwable exception;
}
