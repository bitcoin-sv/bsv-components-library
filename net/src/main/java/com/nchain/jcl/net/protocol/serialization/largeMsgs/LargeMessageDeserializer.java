package com.nchain.jcl.net.protocol.serialization.largeMsgs;

import com.nchain.jcl.net.protocol.messages.HeaderMsg;
import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext;
import com.nchain.jcl.tools.bytes.ByteArrayReader;

import java.util.function.Consumer;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Operatisn implemented by a Deserializer (Only Deserializer) of Large Messages. A Large message is a
 * message that, due to its length, cannot be contained fully in memory. So the Deserialization in this
 * case will never produce a single Java Object, but smaller parts of it. The original Message is
 * deserialized into small Java objects, and those objects are returned in sequence. Since multiple objects are
 * returned and we cannot hold them together in a single return, we need a callback mechanism so the different
 * parts are returned as they are generated.
 */
public interface LargeMessageDeserializer {

    /**
     * It specifies the size of each chunk or partial message returned by the Large Deserializer.
     * Each specific Deserializer might use this property in a different way
     */
    void setPartialMsgSize(int partialMsgSize);

    /**
     * After calling this method, the Deserializer will trigger an error if the bytes coming from the remote Peer
     * come in a slower rate than this (in bytes per Sec)
     */
    void setMinSpeedBytesPerSec(int minSpeedBytesPerSec);

    /**
     * It starts the Deserialization. The different Results will be notified through the callbacks fed in the
     * "onDeserialized" methods.
     * @param context    Deserialization Context
     * @param headerMsg  Header of the message to Deserialize
     * @param byteReader Reader containing the BODY of the message
     */
    void deserializeBody(DeserializerContext context, HeaderMsg headerMsg, ByteArrayReader byteReader);

    /** It provides a callback that wil be triggered when a new part is deserialized */
    void onDeserialized(Consumer<MsgPartDeserializedEvent> eventHandler);

    /** It provides a callback that wil be triggered when an error is triggered during the process */
    void onError(Consumer<MsgPartDeserializationErrorEvent> eventHandler);
}
