package io.bitcoinsv.bsvcl.net.protocol.serialization;



import io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext;
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.MessageSerializer;
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext;
import io.bitcoinsv.bsvcl.net.protocol.messages.RejectMsg;
import io.bitcoinsv.bsvcl.net.protocol.messages.VarStrMsg;
import io.bitcoinsv.bsvcl.common.bytes.ByteArrayReader;
import io.bitcoinsv.bsvcl.common.bytes.ByteArrayWriter;


/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A Serializer for {@link RejectMsg} messages
 */
public class RejectMsgSerializer implements MessageSerializer<RejectMsg> {

    private static RejectMsgSerializer instance;

    // Constructor
    private RejectMsgSerializer() { }

    /** Returns an instance of this Serializer (Singleton) */
    public static RejectMsgSerializer getInstance() {
        if (instance == null) {
            synchronized (RejectMsgSerializer.class) {
                instance = new RejectMsgSerializer();
            }
        }
        return instance;
    }

    @Override
    public RejectMsg deserialize(DeserializerContext context, ByteArrayReader byteReader) {
        RejectMsg.RejectMsgBuilder builder = RejectMsg.builder();

        // "message" field:
        VarStrMsg message = VarStrMsgSerializer.getinstance().deserialize(context, byteReader);
        builder.message(message);

        // "ccode" field:
        builder.ccode(RejectMsg.RejectCode.fromCode(byteReader.read()));

        // "reason" field:
        VarStrMsg reason = VarStrMsgSerializer.getinstance().deserialize(context, byteReader);
        builder.reason(reason);

        // The rest of the data is a Generic data field.
        if (context.getMaxBytesToRead() == null) throw new RuntimeException("The value of MaxBytesToRead is needed");
        int numBytesToRead = (int) (context.getMaxBytesToRead() - message.getLengthInBytes() - 1 - reason.getLengthInBytes());

        byte[] data = byteReader.read(numBytesToRead);
        builder.data(data);

        RejectMsg result = builder.build();
        return result;
    }

    @Override
    public void serialize(SerializerContext context, RejectMsg message, ByteArrayWriter byteWriter) {
        // "message" field:
        VarStrMsgSerializer.getinstance().serialize(context, message.getMessage(), byteWriter);
        byteWriter.write(message.getCcode().getValue());
        VarStrMsgSerializer.getinstance().serialize(context, message.getReason(), byteWriter);
        byteWriter.write(message.getData());
    }
}
