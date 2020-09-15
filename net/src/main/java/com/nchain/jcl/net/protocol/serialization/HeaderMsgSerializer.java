package com.nchain.jcl.net.protocol.serialization;


import com.nchain.jcl.base.tools.bytes.ByteArrayReader;
import com.nchain.jcl.base.tools.bytes.ByteArrayWriter;
import com.nchain.jcl.base.tools.util.StringUtils;
import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext;
import com.nchain.jcl.net.protocol.serialization.common.MessageSerializer;
import com.nchain.jcl.net.protocol.serialization.common.SerializerContext;
import com.nchain.jcl.net.protocol.messages.HeaderMsg;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2019-07-14
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
        byteReader.waitForBytes(24);
        long  magic = byteReader.readUint32();

        // The "command" field is NULL-padded, so we remove the NULL values before
        // storing the value in a String:
        String command = byteReader.readString(12, "UTF-8");
        String commandClean = StringUtils.removeNulls(command);

        HeaderMsg headerMsg = HeaderMsg.builder()
                .magic(magic)
                .command(commandClean)
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