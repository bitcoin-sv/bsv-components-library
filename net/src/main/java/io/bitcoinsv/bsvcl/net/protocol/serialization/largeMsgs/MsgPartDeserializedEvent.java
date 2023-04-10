package io.bitcoinsv.bsvcl.net.protocol.serialization.largeMsgs;


import io.bitcoinsv.bsvcl.tools.events.Event;

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
