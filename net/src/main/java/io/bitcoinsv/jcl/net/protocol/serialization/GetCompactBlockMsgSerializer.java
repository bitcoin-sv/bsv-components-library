package io.bitcoinsv.jcl.net.protocol.serialization;

import io.bitcoinsv.jcl.net.protocol.messages.GetCompactBlockMsg;
import io.bitcoinsv.jcl.net.protocol.messages.HashMsg;
import io.bitcoinsv.jcl.net.protocol.serialization.common.DeserializerContext;
import io.bitcoinsv.jcl.net.protocol.serialization.common.MessageSerializer;
import io.bitcoinsv.jcl.net.protocol.serialization.common.SerializerContext;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayWriter;

/**
 * A Serializer for {@link GetCompactBlockMsg} messages
 */
public class GetCompactBlockMsgSerializer implements MessageSerializer<GetCompactBlockMsg> {
    private static GetCompactBlockMsgSerializer instance;

    protected GetCompactBlockMsgSerializer() {
    }

    public static GetCompactBlockMsgSerializer getInstance() {
        if (instance == null) {
            synchronized (GetCompactBlockMsgSerializer.class) {
                instance = new GetCompactBlockMsgSerializer();
            }
        }
        return instance;
    }

    /**
     * Deserializes the message.
     * @param context                   Serializer Context
     * @param byteReader                Wrapper for the Byte Array Source
     * @return GetCompactBlockMsg instance.
     */
    @Override
    public GetCompactBlockMsg deserialize(DeserializerContext context, ByteArrayReader byteReader) {
        HashMsg blockHash = HashMsgSerializer.getInstance().deserialize(context, byteReader);
        return GetCompactBlockMsg.builder().blockHash(blockHash).build();
    }

    /**
     * Serializes the GetCompactBlockMsg into a P2P message body.
     * @param context                   Serializer Context
     * @param message                   Message to Serialize
     * @param byteWriter                Result of the Serialization
     */
    @Override
    public void serialize(SerializerContext context, GetCompactBlockMsg message, ByteArrayWriter byteWriter) {
        HashMsgSerializer.getInstance().serialize(context, message.getblockHash(), byteWriter);
    }
}