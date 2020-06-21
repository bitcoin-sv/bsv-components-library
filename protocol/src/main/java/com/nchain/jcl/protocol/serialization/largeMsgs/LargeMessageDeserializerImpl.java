package com.nchain.jcl.protocol.serialization.largeMsgs;

import com.nchain.jcl.protocol.messages.common.Message;
import com.nchain.jcl.tools.events.EventBus;

import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

/**
 * @author j.bloggs@nchain.com
 * Copyright (c) 2009-2010 Satoshi Nakamoto
 * Copyright (c) 2009-2016 The Bitcoin Core developers
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-18 15:09
 */
public abstract class LargeMessageDeserializerImpl implements LargeMessageDeserializer {

    private EventBus eventBus;

    public LargeMessageDeserializerImpl(ExecutorService executor) {
        this.eventBus = EventBus.builder().executor(executor).build();
    }

    public LargeMessageDeserializerImpl() {
        this(null);
    }

    @Override
    public void onDeserialized(Consumer<MsgPartDeserializedEvent> eventHandler) {
        eventBus.subscribe(MsgPartDeserializedEvent.class, eventHandler);
    }

    @Override
    public void onError(Consumer<MsgPartDeserializationErrorEvent> eventHandler) {
        eventBus.subscribe(MsgPartDeserializationErrorEvent.class, eventHandler);
    }

    @Override
    public void onFinish(Consumer<MsgDeserializationFinishedEvent> eventHandler) {
        eventBus.subscribe(MsgDeserializationFinishedEvent.class, eventHandler);
    }

    public void notifyDeserialization(Message message) {
        eventBus.publish(new MsgPartDeserializedEvent<>(message));
    }

    public void notifyError(Exception e) {
        eventBus.publish(new MsgPartDeserializationErrorEvent(e));
    }
}
