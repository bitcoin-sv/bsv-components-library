package com.nchain.jcl.protocol.serialization.largeMsgs;

import com.nchain.jcl.tools.events.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-18 15:14
 *
 * An Event triggered when a part of a Large Message has been deserialized
 */
@Getter
@AllArgsConstructor
public class MsgPartDeserializedEvent<T> extends Event {
    private T data;
}
