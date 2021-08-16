package io.bitcoinsv.jcl.net.protocol.serialization;


import io.bitcoinsv.jcl.net.protocol.serialization.common.DeserializerContext;
import io.bitcoinsv.jcl.net.protocol.serialization.common.MessageSerializer;
import io.bitcoinsv.jcl.net.protocol.serialization.common.SerializerContext;
import io.bitcoinsv.jcl.net.protocol.messages.HeaderMsg;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayWriter;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A serializer for {@link HeaderMsg} messages
 */
public class HeaderMsgSerializer implements MessageSerializer<HeaderMsg> {

    private static HeaderMsgSerializer instance;

    // Constructor
    private HeaderMsgSerializer() { }

    /** Returns the instance of this Class (Singleton) */
    public static HeaderMsgSerializer getInstance() {
        if (instance == null) {
            synchronized (HeaderMsgSerializer.class) {
                instance = new HeaderMsgSerializer();
            }
        }
        return instance;
    }

    @Override
    public HeaderMsg deserialize(DeserializerContext context, ByteArrayReader byteReader) {
        long  magic = byteReader.readUint32();

        // The "command" field is NULL-padded, so we remove the NULL values before
        // storing the value in a String:
        String command = byteReader.readString(12, "UTF-8");

        HeaderMsg headerMsg = HeaderMsg.builder()
                .magic(magic)
                .command(command)
                .length(byteReader.readUint32())
                .checksum(byteReader.readUint32()).build();

        return headerMsg;
    }

    @Override
    public void serialize(SerializerContext context, HeaderMsg message, ByteArrayWriter byteWriter) {
        byteWriter.writeUint32LE(message.getMagic());
        byteWriter.writeStr(message.getCommand(), 12);
        byteWriter.writeUint32LE(message.getLength());
        byteWriter.writeUint32LE(message.getChecksum());
    }
}
