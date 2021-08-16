/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.protocol.serialization.largeMsgs;


import io.bitcoinsv.jcl.tools.events.Event;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when a part of a Large Message has been deserialized
 */
public class MsgPartDeserializedEvent<T> extends Event {
    private T data;

    public MsgPartDeserializedEvent(T data) {
        this.data = data;
    }

    public T getData() {
        return this.data;
    }
}
