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
        String command = byteReader.readString(12, "UTF-8");

        // We read the values present in any header:
        HeaderMsg.HeaderMsgBuilder headerBuilder = HeaderMsg.builder();
        headerBuilder.magic(magic);
        headerBuilder.command(command);
        headerBuilder.length(byteReader.readUint32());
        headerBuilder.checksum(byteReader.readUint32());

        // Messages bigger than 4GB use an special command, and extra fields are used:
        if (command.equalsIgnoreCase(HeaderMsg.EXT_COMMAND)) {
            String extCommand = byteReader.readString(12, "UTF-8");
            long extLength = byteReader.readUint64();
            headerBuilder.extCommand(extCommand);
            headerBuilder.extLength(extLength);
        }

        // We get the Header Object:
        HeaderMsg headerMsg = headerBuilder.build();

        return headerMsg;
    }

    @Override
    public void serialize(SerializerContext context, HeaderMsg message, ByteArrayWriter byteWriter) {
        byteWriter.writeUint32LE(message.getMagic());
        byteWriter.writeStr(message.getCommand(), 12);
        byteWriter.writeUint32LE(message.getLength());
        byteWriter.writeUint32LE(message.getChecksum());

        // Messages bigger than 4GB use an special command, and extra fields are used:
        if (message.getCommand().equalsIgnoreCase(HeaderMsg.EXT_COMMAND)) {
            byteWriter.writeStr(message.getExtCommand(), 12);
            byteWriter.writeUint64LE(message.getExtLength());
        }
    }
}
