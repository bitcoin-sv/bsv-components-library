package com.nchain.jcl.net.protocol.serialization;

import com.nchain.jcl.net.protocol.messages.BlockDetailsMsg;
import com.nchain.jcl.net.protocol.messages.DsDetectedMsg;
import com.nchain.jcl.net.protocol.messages.VarIntMsg;
import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext;
import com.nchain.jcl.net.protocol.serialization.common.MessageSerializer;
import com.nchain.jcl.net.protocol.serialization.common.SerializerContext;
import com.nchain.jcl.tools.bytes.ByteArrayReader;
import com.nchain.jcl.tools.bytes.ByteArrayWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A Serializer for {@link DsDetectedMsgSerializer} messages
 */
public class DsDetectedMsgSerializer implements MessageSerializer<DsDetectedMsg> {

    private static DsDetectedMsgSerializer instance;

    // Constructor
    private DsDetectedMsgSerializer() { }

    /** Returns an instance of this Serializer (Singleton) */
    public static DsDetectedMsgSerializer getInstance() {
        if (instance == null) {
            synchronized (DsDetectedMsgSerializer.class) {
                instance = new DsDetectedMsgSerializer();
            }
        }
        return instance;
    }

    @Override
    public DsDetectedMsg deserialize(DeserializerContext context, ByteArrayReader byteReader) {
        int version = byteReader.readUint16();
        VarIntMsg blockCount = VarIntMsgSerializer.getInstance().deserialize(context, byteReader);

        List<BlockDetailsMsg> blockDetailsMsgList = new ArrayList<>();
        for(int i = 0; i < blockCount.getValue(); i++){
            blockDetailsMsgList.add(BlockDetailsMsgSerializer.getInstance().deserialize(context, byteReader));
        }

        return DsDetectedMsg.builder()
                .withVersion(version)
                .withBlockCount(blockCount)
                .withBlockList(blockDetailsMsgList)
                .build();
    }

    @Override
    public void serialize(SerializerContext context, DsDetectedMsg message, ByteArrayWriter byteWriter) {
        byteWriter.writeUint16LE(message.getVersion());
        VarIntMsgSerializer.getInstance().serialize(context, message.getBlockCount(), byteWriter);

        for(int i = 0; i < message.getBlockCount().getValue(); i++){
            BlockDetailsMsgSerializer.getInstance().serialize(context, message.getBlockList().get(i), byteWriter);
        }
    }
}
