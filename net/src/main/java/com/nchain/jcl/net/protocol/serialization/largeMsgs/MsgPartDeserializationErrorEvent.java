package com.nchain.jcl.net.protocol.serialization.largeMsgs;


import com.nchain.jcl.tools.events.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * Event triggered when an Error has been triggered during a Large Message Deserialization
 */
@Getter
@AllArgsConstructor
public class MsgPartDeserializationErrorEvent extends Event {
    private Exception exception;
}
