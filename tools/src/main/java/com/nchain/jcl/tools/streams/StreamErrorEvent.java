package com.nchain.jcl.tools.streams;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-03 10:30
 *
 * This event represent an error thrown by an Stream, which most probably has been thrown during
 * the transformation function.
 */
@AllArgsConstructor
public class StreamErrorEvent extends StreamEvent {
    @Getter
    Throwable exception;
}
