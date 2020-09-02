package com.nchain.jcl.net.protocol.serialization;


import com.nchain.jcl.base.tools.bytes.ByteArrayReader;
import com.nchain.jcl.base.tools.bytes.ByteArrayWriter;
import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext;
import com.nchain.jcl.net.protocol.serialization.common.MessageSerializer;
import com.nchain.jcl.net.protocol.serialization.common.SerializerContext;
import com.nchain.jcl.net.protocol.messages.GetAddrMsg;

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 07/08/2019
 *
 * A serializer for {@link GetAddrMsg} messages
 */
public class GetAddrMsgSerializer implements MessageSerializer<GetAddrMsg> {

    private static GetAddrMsgSerializer instance;

    private GetAddrMsgSerializer() {}

    /** Returns the instance of this Class (Singleton) */
    public static GetAddrMsgSerializer getInstance() {

        if(instance == null) {
            synchronized (GetAddrMsgSerializer.class) {
                instance = new GetAddrMsgSerializer();
            }
        }
        return instance;
    }

    @Override
    public GetAddrMsg deserialize(DeserializerContext context, ByteArrayReader byteReader) {
        return  GetAddrMsg.builder().build();
    }

    @Override
    public void serialize(SerializerContext context, GetAddrMsg message, ByteArrayWriter byteWriter) {
        // Empty Message
    }
}
