package com.nchain.jcl.net.protocol.serialization.largeMsgs;


import com.nchain.jcl.net.protocol.messages.common.Message;
import com.nchain.jcl.tools.bytes.ByteArrayReader;
import com.nchain.jcl.tools.bytes.ByteArrayReaderRealTime;
import com.nchain.jcl.tools.events.EventBus;

import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Base implementation of a Large Message Deserializer. Imlements basic initilization stuff
 * and convenience methods.
 */
public abstract class LargeMessageDeserializerImpl implements LargeMessageDeserializer {

    // USed to trigger the callbacks
    private EventBus eventBus;

    // If this property is specified, the reader will be upgraded to expect this speed when reading bytes, or an
    // exception will nbe thrown.
    private Integer minSpeedBytesPerSec = ByteArrayReaderRealTime.DEFAULT_SPEED_BYTES_PER_SECOND;

    /** Constructor. The ServiceExecutor will be used to trigger the callbacks in a different Thread */
    public LargeMessageDeserializerImpl(ExecutorService executor) {
        this.eventBus = EventBus.builder().executor(executor).build();
    }
    /** Constructor. Since no Executor Service is specified here, all the callbacks are blocking */
    public LargeMessageDeserializerImpl() {
        this(null);
    }

    @Override
    public void setMinSpeedBytesPerSec(int minSpeedBytesPerSec) {
        this.minSpeedBytesPerSec = minSpeedBytesPerSec;
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

    // Returns the minimum Speed (bytes/Sec), if specified
    public Integer getMinSpeedBytesPerSec() {
        return this.minSpeedBytesPerSec;
    }

    // It adjusts the Reader to the internal Speed
    protected void adjustReaderSpeed(ByteArrayReader byteReader) {
        if (byteReader instanceof ByteArrayReaderRealTime) {
            ((ByteArrayReaderRealTime) byteReader).updateReaderSpeed(getMinSpeedBytesPerSec());
        }
    }

    // It resets the reader speed:
    protected void resetReaderSpeed(ByteArrayReader byteReader) {
        if (byteReader instanceof ByteArrayReaderRealTime) {
            ((ByteArrayReaderRealTime) byteReader).resetReaderSpeed();
        }
    }
}