package com.nchain.jcl.protocol.serialization;


import com.nchain.jcl.protocol.messages.NetAddressMsg;
import com.nchain.jcl.protocol.messages.VarStrMsg;
import com.nchain.jcl.protocol.messages.VersionMsg;
import com.nchain.jcl.protocol.serialization.common.*;
import com.nchain.jcl.tools.bytes.ByteArrayReader;
import com.nchain.jcl.tools.bytes.ByteArrayWriter;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2019-07-17
 *
 * A Serializer for {@link VersionMsg} messages
 */
public class VersionMsgSerializer implements MessageSerializer<VersionMsg> {

    private static VersionMsgSerializer instance;

    private VersionMsgSerializer() { }

    /** Returns the instance of this Serializer (Singleton) */
    public static VersionMsgSerializer getInstance() {
        if (instance == null) {
            synchronized (VersionMsgSerializer.class) {
                instance = new VersionMsgSerializer();
            }
        }
        return instance;
    }

    @Override
    public VersionMsg deserialize(DeserializerContext context, ByteArrayReader byteReader) {
        context.setInsideVersionMsg(true);

        byteReader.waitForBytes(20);
        long version = byteReader.readUint32();
        long services = byteReader.readInt64LE();
        long timestamp = byteReader.readInt64LE();

        NetAddressMsg addr_from = NetAddressMsgSerializer.getInstance().deserialize(context, byteReader);
        NetAddressMsg addr_recv = NetAddressMsgSerializer.getInstance().deserialize(context, byteReader);

        byteReader.waitForBytes(12);
        long nonce = byteReader.readInt64LE(); // TODO: We need to know who to process this field
        VarStrMsg user_agent = VarStrMsgSerializer.getinstance().deserialize(context, byteReader);
        long start_height = byteReader.readUint32();
        boolean relay = true;


        // The "RELAY" Field is optional. For version >= 70001, this field might be included or not.
        if (byteReader.getBytesReadCount() == (context.getMaxBytesToRead() - 1)) {
            byteReader.waitForBytes(1);
            relay = byteReader.readBoolean();
        }

        VersionMsg versionMsg = VersionMsg.builder()
                .version(version)
                .services(services)
                .timestamp(timestamp)
                .addr_from(addr_from)
                .addr_recv(addr_recv)
                .nonce(nonce)
                .user_agent(user_agent)
                .start_height(start_height)
                .relay(relay).build();

        return versionMsg;
    }

    @Override
    public void serialize(SerializerContext context, VersionMsg message, ByteArrayWriter byteWriter) {
        context.setInsideVersionMsg(true);

        byteWriter.writeUint32LE(message.getVersion());
        byteWriter.writeUint64LE(message.getServices()); // TODO: Check this!!!
        byteWriter.writeUint64LE(message.getTimestamp()); // TODO: Check this!!
        NetAddressMsgSerializer.getInstance().serialize(context, message.getAddr_from(), byteWriter);
        NetAddressMsgSerializer.getInstance().serialize(context, message.getAddr_recv(), byteWriter);
        byteWriter.writeUint64LE(message.getNonce());
        VarStrMsgSerializer.getinstance().serialize(context, message.getUser_agent(), byteWriter);
        byteWriter.writeUint32LE(message.getStart_height());
        byteWriter.writeBoolean(message.isRelay());
    }
}
