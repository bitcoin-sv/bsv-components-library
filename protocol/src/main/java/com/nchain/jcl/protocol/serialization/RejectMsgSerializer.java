package com.nchain.jcl.protocol.serialization;


import com.nchain.jcl.protocol.messages.RejectMsg;
import com.nchain.jcl.protocol.messages.RejectMsgBuilder;
import com.nchain.jcl.protocol.messages.VarStrMsg;
import com.nchain.jcl.protocol.serialization.common.*;
import com.nchain.jcl.tools.crypto.Sha256Wrapper;
import com.nchain.jcl.tools.bytes.ByteArrayReader;
import com.nchain.jcl.tools.bytes.ByteArrayWriter;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2019-07-23 10:47
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
        RejectMsgBuilder builder = new RejectMsgBuilder();

        // "message" field:
        VarStrMsg message = VarStrMsgSerializer.getinstance().deserialize(context, byteReader);
        builder.setMessage(message);

        // "ccode" field:
        byteReader.waitForBytes(1);
        builder.setCcode(RejectMsg.RejectCode.fromCode(byteReader.read()));

        // "reason" field:
        VarStrMsg reason = VarStrMsgSerializer.getinstance().deserialize(context, byteReader);
        builder.setReason(reason);

        // The rest of the data is a Generic data field. So far, its content is either empty or filled with the
        // HASH of a TX or Block Header. the value of the "message" field will tell us the case.
        // In case it's not a hash, well read a generic byte array

        if ((message.getStr().equals(RejectMsg.MESSAGE_BLOCK)) || (message.getStr().equals(RejectMsg.MESSAGE_TX))) {
            byteReader.waitForBytes(32);
            Sha256Wrapper dataHash = Sha256Wrapper.wrapReversed(byteReader.read(32));
            builder.setData(dataHash);
        } else {
            if (context.getMaxBytesToRead() == null)
                throw new RuntimeException("The value of MaxBytesToRead is needed");

            int numBytesToRead = (int) (context.getMaxBytesToRead() - message.getLengthInBytes()
                    - 1 - reason.getLengthInBytes());

            byteReader.waitForBytes(0);
            byte[] data = byteReader.read(numBytesToRead);
            builder.setData(data);
        }
        RejectMsg result = builder.build();
        return result;
    }

    @Override
    public void serialize(SerializerContext context, RejectMsg message, ByteArrayWriter byteWriter) {
        // "message" field:
        VarStrMsgSerializer.getinstance().serialize(context, message.getMessage(), byteWriter);
        byteWriter.write(message.getCcode().getValue());
        VarStrMsgSerializer.getinstance().serialize(context, message.getReason(), byteWriter);
        if (message.getDataHash() != null) {
            byteWriter.write(Sha256Wrapper.wrapReversed(message.getDataHash().getBytes()).getBytes());
        }
    }

}
