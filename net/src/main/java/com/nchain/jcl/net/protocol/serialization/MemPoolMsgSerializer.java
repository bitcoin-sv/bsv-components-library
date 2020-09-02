package com.nchain.jcl.net.protocol.serialization;

import com.nchain.jcl.base.tools.bytes.ByteArrayReader;
import com.nchain.jcl.base.tools.bytes.ByteArrayWriter;
import com.nchain.jcl.net.protocol.messages.MemPoolMsg;
import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext;
import com.nchain.jcl.net.protocol.serialization.common.MessageSerializer;
import com.nchain.jcl.net.protocol.serialization.common.SerializerContext;

/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 30/07/2020
 */
public class MemPoolMsgSerializer implements MessageSerializer<MemPoolMsg> {

    private static MemPoolMsgSerializer instance;

    /** Returns the instance of this Class (Singleton) */
    public static MemPoolMsgSerializer getInstance() {
        if (instance == null) {
            synchronized (NetAddressMsgSerializer.class) {
                instance = new MemPoolMsgSerializer();
            }
        }
        return instance;
    }

    @Override
    public MemPoolMsg deserialize(DeserializerContext context, ByteArrayReader byteReader) {
        return new MemPoolMsg().builder().build();
    }

    @Override
    public void serialize(SerializerContext context, MemPoolMsg message, ByteArrayWriter byteWriter) {
    }
}
