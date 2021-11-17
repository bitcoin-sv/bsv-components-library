package com.nchain.jcl.net.protocol.serialization;


import com.nchain.jcl.net.protocol.messages.SendHeadersMsg;
import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext;
import com.nchain.jcl.net.protocol.serialization.common.MessageSerializer;
import com.nchain.jcl.net.protocol.serialization.common.SerializerContext;
import com.nchain.jcl.tools.bytes.ByteArrayReader;
import com.nchain.jcl.tools.bytes.ByteArrayWriter;

/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */
public class SendHeadersMsgSerializer implements MessageSerializer<SendHeadersMsg> {

    private static SendHeadersMsgSerializer instance;

    /** Returns the instance of this Class (Singleton) */
    public static SendHeadersMsgSerializer getInstance() {
        if (instance == null) {
            synchronized (NetAddressMsgSerializer.class) {
                instance = new SendHeadersMsgSerializer();
            }
        }
        return instance;
    }

    @Override
    public SendHeadersMsg deserialize(DeserializerContext context, ByteArrayReader byteReader) {
        return new SendHeadersMsg.SendHeadersMsgBuilder().build();
    }

    @Override
    public void serialize(SerializerContext context, SendHeadersMsg message, ByteArrayWriter byteWriter) {
    }
}
