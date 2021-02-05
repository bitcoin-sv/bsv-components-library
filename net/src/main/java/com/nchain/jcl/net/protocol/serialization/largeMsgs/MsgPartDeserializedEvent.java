package com.nchain.jcl.net.protocol.serialization.largeMsgs;


import com.nchain.jcl.tools.events.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when a part of a Large Message has been deserialized
 */
@Getter
@AllArgsConstructor
public class MsgPartDeserializedEvent<T> extends Event {
    private T data;
}
