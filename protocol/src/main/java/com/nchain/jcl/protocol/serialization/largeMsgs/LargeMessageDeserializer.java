package com.nchain.jcl.protocol.serialization.largeMsgs;

import com.nchain.jcl.protocol.serialization.common.DeserializerContext;
import com.nchain.jcl.tools.bytes.ByteArrayReader;

import java.util.function.Consumer;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-18 14:57
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
     * It starts the Deserialization. The diffrent Results will be notified through the callbacks fed in the
     * "onDeserialized" methods.
     */
    void deserialize(DeserializerContext context, ByteArrayReader byteReader);

    /** It provides a callback that wil be triggered when a new part is deserialized */
    void onDeserialized(Consumer<MsgPartDeserializedEvent> eventHandler);

    /** It provides a callback that wil be triggered when an error is triggered during the process */
    void onError(Consumer<MsgPartDeserializationErrorEvent> eventHandler);
}
