package com.nchain.jcl.protocol.serialization;


import com.nchain.jcl.protocol.messages.GetAddrMsg;
import com.nchain.jcl.protocol.serialization.common.*;
import com.nchain.jcl.tools.bytes.ByteArrayReader;
import com.nchain.jcl.tools.bytes.ByteArrayWriter;

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
