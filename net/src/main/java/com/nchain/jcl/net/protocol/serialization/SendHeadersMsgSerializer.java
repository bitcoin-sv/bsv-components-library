package com.nchain.jcl.net.protocol.serialization;

import com.nchain.jcl.base.tools.bytes.ByteArrayReader;
import com.nchain.jcl.base.tools.bytes.ByteArrayWriter;
import com.nchain.jcl.net.protocol.messages.SendHeadersMsg;
import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext;
import com.nchain.jcl.net.protocol.serialization.common.MessageSerializer;
import com.nchain.jcl.net.protocol.serialization.common.SerializerContext;

/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 30/07/2020
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
        return new SendHeadersMsg().builder().build();
    }

    @Override
    public void serialize(SerializerContext context, SendHeadersMsg message, ByteArrayWriter byteWriter) {
    }
}
