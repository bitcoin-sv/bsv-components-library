package io.bitcoinsv.jcl.net.protocol.serialization;


import io.bitcoinsv.jcl.net.protocol.config.ProtocolVersion;
import io.bitcoinsv.jcl.net.protocol.serialization.common.DeserializerContext;
import io.bitcoinsv.jcl.net.protocol.serialization.common.MessageSerializer;
import io.bitcoinsv.jcl.net.protocol.serialization.common.SerializerContext;
import io.bitcoinsv.jcl.net.protocol.messages.NetAddressMsg;
import io.bitcoinsv.jcl.net.protocol.messages.VarStrMsg;
import io.bitcoinsv.jcl.net.protocol.messages.VersionMsg;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayWriter;
import io.bitcoinsv.bitcoinjsv.core.Utils;

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

        long version = byteReader.readUint32();
        long services = byteReader.readInt64LE();
        long timestamp = byteReader.readInt64LE();

        NetAddressMsg addr_from = NetAddressMsgSerializer.getInstance().deserialize(context, byteReader);
        NetAddressMsg addr_recv = NetAddressMsgSerializer.getInstance().deserialize(context, byteReader);

        long nonce = byteReader.readInt64LE(); // TODO: We need to know who to process this field
        VarStrMsg user_agent = VarStrMsgSerializer.getinstance().deserialize(context, byteReader);
        long start_height = byteReader.readInt32();

        // We check if there are more bytes pending to read. Depending on the protocol Version, there might be:
        // - "relay" (1 byte) :         is only present if protocol >= 7001
        // - "associationId" (1 byte) : is only present if protocol >= 70015 and its actually there (its optional)

        Boolean relay = null;
        byte[] associationId = Utils.EMPTY_BYTE_ARRAY;

        int numBytesReadedForThisMessage = 20 + (int) addr_from.getLengthInBytes() + (int) addr_recv.getLengthInBytes() + 8 + 4 + (int) user_agent.getLengthInBytes();

        if (numBytesReadedForThisMessage < context.getMaxBytesToRead()) {
            int numBytesRemaining = (int) (context.getMaxBytesToRead() - numBytesReadedForThisMessage);

            boolean isRelayField = (version >= ProtocolVersion.ENABLE_VERSION.getVersion());
            boolean isAssociationIdField = (isRelayField && numBytesRemaining == 2) || (!isRelayField && numBytesRemaining == 1);

            if (isRelayField)           { relay = byteReader.readBoolean(); }
            if (isAssociationIdField)   { associationId = byteReader.read(1); }
        }

        // We build the VERSION Message:
        VersionMsg versionMsg = VersionMsg.builder()
                .version(version)
                .services(services)
                .timestamp(timestamp)
                .addr_from(addr_from)
                .addr_recv(addr_recv)
                .nonce(nonce)
                .user_agent(user_agent)
                .start_height(start_height)
                .relay(relay)
                .associationId(associationId)
                .build();

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
        byteWriter.writeInt32LE(message.getStart_height());
        if (message.getRelay() != null) { byteWriter.writeBoolean(message.getRelay());}
        if (message.getAssociationId().length > 0) { byteWriter.write(message.getAssociationId());}
    }
}
