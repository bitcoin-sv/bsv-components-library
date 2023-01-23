package io.bitcoinsv.jcl.net.protocol.serialization;

import io.bitcoinsv.jcl.net.protocol.messages.CompactTransactionMsg;
import io.bitcoinsv.jcl.net.protocol.messages.HashMsg;
import io.bitcoinsv.jcl.net.protocol.serialization.common.DeserializerContext;
import io.bitcoinsv.jcl.net.protocol.serialization.common.MessageSerializer;
import io.bitcoinsv.jcl.net.protocol.serialization.common.SerializerContext;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayWriter;

/**
 * A Serializer for {@link CompactTransactionMsg} messages
 */
public class CompactTransactionMsgSerializer implements MessageSerializer<CompactTransactionMsg> {
    private static CompactTransactionMsgSerializer instance;

    protected CompactTransactionMsgSerializer() {
    }

    public static CompactTransactionMsgSerializer getInstance() {
        if (instance == null) {
            synchronized (CompactTransactionMsgSerializer.class) {
                instance = new CompactTransactionMsgSerializer();
            }
        }
        return instance;
    }

    /**
     * Deserializes the message.
     * @param context                   Serializer Context
     * @param byteReader                Wrapper for the Byte Array Source
     * @return CompactTransactionMsg instance.
     */
    @Override
    public CompactTransactionMsg deserialize(DeserializerContext context, ByteArrayReader byteReader) {

        HashMsg txId = HashMsgSerializer.getInstance().deserialize(context, byteReader);
        boolean isIndependent = byteReader.readBoolean();
        return new CompactTransactionMsg(txId, isIndependent);
    }

    /**
     * Serializes the CompactTransactionMsg into a P2P message body.
     * @param context                   Serializer Context
     * @param message                   Message to Serialize
     * @param byteWriter                Result of the Serialization
     */
    @Override
    public void serialize(SerializerContext context, CompactTransactionMsg message, ByteArrayWriter byteWriter) {
        HashMsgSerializer.getInstance().serialize(context, message.getTxId(), byteWriter);
        byteWriter.writeBoolean(message.isIndependent());
    }
}