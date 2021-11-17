package com.nchain.jcl.net.protocol.serialization;

import com.nchain.jcl.net.protocol.messages.MemPoolMsg;
import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext;
import com.nchain.jcl.net.protocol.serialization.common.MessageSerializer;
import com.nchain.jcl.net.protocol.serialization.common.SerializerContext;
import com.nchain.jcl.tools.bytes.ByteArrayReader;
import com.nchain.jcl.tools.bytes.ByteArrayWriter;

/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
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
        return new MemPoolMsg.MemPoolMsgBuilder().build();
    }

    @Override
    public void serialize(SerializerContext context, MemPoolMsg message, ByteArrayWriter byteWriter) {
    }
}
