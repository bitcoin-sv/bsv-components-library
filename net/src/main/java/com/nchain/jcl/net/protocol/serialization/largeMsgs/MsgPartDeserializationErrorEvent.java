package com.nchain.jcl.net.protocol.serialization.largeMsgs;

import com.nchain.jcl.base.tools.events.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-18 15:14
 *
 * Event triggered when an Error has been triggered during a Large Message Deserialization
 */
@Getter
@AllArgsConstructor
public class MsgPartDeserializationErrorEvent extends Event {
    private Exception exception;
}
