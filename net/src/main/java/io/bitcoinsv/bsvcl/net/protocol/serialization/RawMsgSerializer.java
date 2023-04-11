package io.bitcoinsv.bsvcl.net.protocol.serialization;


import io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext;
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.MessageSerializer;
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext;
import io.bitcoinsv.bsvcl.net.protocol.messages.RawMsg;
import io.bitcoinsv.bsvcl.common.bytes.ByteArrayReader;
import io.bitcoinsv.bsvcl.common.bytes.ByteArrayWriter;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A Serializer for instance of {@Link RawTxMsg} messages.
 * This Serializer can be used when the WHOLE Message is Stored in RawFormat in the {@link RawMsg} class. If the
 * Message has other fields that are NOT in Raw format, a custom Serializer must be developed.
 */
public abstract class RawMsgSerializer<M extends RawMsg> implements MessageSerializer<M> {

    public abstract M buildRawMsg(byte[] content);

    // TODO: CHECK PERFORMANCE WHEN CALCULATING THE HASH!!!!!!!!
    @Override
    public M deserialize(DeserializerContext context, ByteArrayReader byteReader) {

        // We read all the Bytes...
        byte[] content = byteReader.read(context.getMaxBytesToRead().intValue());

        // We return the object:
        return buildRawMsg(content);
    }

    @Override
    public void serialize(SerializerContext context, M message, ByteArrayWriter byteWriter) {
        // We write the content directly....
        byteWriter.write(message.getContent());
    }
}
