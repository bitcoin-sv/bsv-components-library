package com.nchain.jcl.protocol.serialization.largeMsgs;

import com.nchain.jcl.protocol.messages.common.Message;
import com.nchain.jcl.tools.events.EventBus;

import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-18 15:09
 *
 * Base implementation of a Large Message Deserializer. Imlements basic initilization stuff
 * and convenience methods.
 */
public abstract class LargeMessageDeserializerImpl implements LargeMessageDeserializer {

    // USed to trigger the callbacks
    private EventBus eventBus;

    /** Constructor. The ServiceExecutor will be used to trigger the callbacks in a different Thread */
    public LargeMessageDeserializerImpl(ExecutorService executor) {
        this.eventBus = EventBus.builder().executor(executor).build();
    }
    /** Constructor. Since no Executot Service is specified here, al the callbacks are blocking */
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

    // Convenience method to call when we ned to notify something has been deserialized
    public void notifyDeserialization(Message message) {
        eventBus.publish(new MsgPartDeserializedEvent<>(message));
    }
    // Convenience method to call when we need to notify about an error
    public void notifyError(Exception e) {
        eventBus.publish(new MsgPartDeserializationErrorEvent(e));
    }
}
