package com.nchain.jcl.net.protocol.serialization;


import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext;
import com.nchain.jcl.net.protocol.serialization.common.MessageSerializer;
import com.nchain.jcl.net.protocol.serialization.common.SerializerContext;
import com.nchain.jcl.net.protocol.messages.NetAddressMsg;
import com.nchain.jcl.net.protocol.messages.VarStrMsg;
import com.nchain.jcl.net.protocol.messages.VersionMsg;
import com.nchain.jcl.tools.bytes.ByteArrayReader;
import com.nchain.jcl.tools.bytes.ByteArrayWriter;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
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

        byteReader.waitForBytes(8);
        long nonce = byteReader.readInt64LE(); // TODO: We need to know who to process this field
        VarStrMsg user_agent = VarStrMsgSerializer.getinstance().deserialize(context, byteReader);
        byteReader.waitForBytes(4);
        long start_height = byteReader.readUint32();
        Boolean relay = null;


        // The "RELAY" Field is optional. So we need to check if the field is there or not. Its NOT enough to just check
        // if there is mre data in the reader, since that data might belong to tnext message in line, not this one. So
        // we need to compare the bytes we've read so far for this message, to the MAXIMUM number of Bytes that this
        // message takes in te reader...
        int bytesReadedForThisMessage = 20 + (int) addr_from.getLengthInBytes() + (int) addr_recv.getLengthInBytes() + 8 + 4 + (int) user_agent.getLengthInBytes();

        if (bytesReadedForThisMessage == (context.getMaxBytesToRead() - 1)) {
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
        if (message.getRelay() != null) byteWriter.writeBoolean(message.getRelay());
    }
}
