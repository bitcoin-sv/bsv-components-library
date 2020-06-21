package com.nchain.jcl.protocol.serialization.largeMsgs;

import com.nchain.jcl.protocol.messages.common.Message;
import com.nchain.jcl.protocol.serialization.common.DeserializerContext;
import com.nchain.jcl.tools.bytes.ByteArrayReader;

import java.util.function.Consumer;

/**
 * @author j.bloggs@nchain.com
 * Copyright (c) 2009-2010 Satoshi Nakamoto
 * Copyright (c) 2009-2016 The Bitcoin Core developers
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-18 14:57
 */
public interface LargeMessageDeserializer {

    void deserialize(DeserializerContext context, ByteArrayReader byteReader);

    void onDeserialized(Consumer<MsgPartDeserializedEvent> eventHandler);
    void onError(Consumer<MsgPartDeserializationErrorEvent> eventHandler);
    void onFinish(Consumer<MsgDeserializationFinishedEvent> eventHandler);
}
