package io.bitcoinsv.jcl.net.protocol.serialization;



import io.bitcoinsv.jcl.net.protocol.serialization.common.DeserializerContext;
import io.bitcoinsv.jcl.net.protocol.serialization.common.MessageSerializer;
import io.bitcoinsv.jcl.net.protocol.serialization.common.SerializerContext;
import io.bitcoinsv.jcl.net.protocol.messages.GetAddrMsg;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayWriter;

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
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
