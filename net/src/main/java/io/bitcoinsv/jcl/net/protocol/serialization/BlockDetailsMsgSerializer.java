package io.bitcoinsv.jcl.net.protocol.serialization;

import io.bitcoinsv.jcl.net.protocol.messages.BlockDetailsMsg;
import io.bitcoinsv.jcl.net.protocol.messages.BlockHeaderSimpleMsg;
import io.bitcoinsv.jcl.net.protocol.messages.VarIntMsg;
import io.bitcoinsv.jcl.net.protocol.serialization.common.DeserializerContext;
import io.bitcoinsv.jcl.net.protocol.serialization.common.MessageSerializer;
import io.bitcoinsv.jcl.net.protocol.serialization.common.SerializerContext;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A Serializer for {@link DsDetectedMsgSerializer} messages
 */
public class BlockDetailsMsgSerializer implements MessageSerializer<BlockDetailsMsg> {

    private static BlockDetailsMsgSerializer instance;

    // Constructor
    private BlockDetailsMsgSerializer() { }

    /** Returns an instance of this Serializer (Singleton) */
    public static BlockDetailsMsgSerializer getInstance() {
        if (instance == null) {
            synchronized (BlockDetailsMsgSerializer.class) {
                instance = new BlockDetailsMsgSerializer();
            }
        }
        return instance;
    }

    @Override
    public BlockDetailsMsg deserialize(DeserializerContext context, ByteArrayReader byteReader) {
        VarIntMsg headerCount = VarIntMsgSerializer.getInstance().deserialize(context, byteReader);

        List<BlockHeaderSimpleMsg> blockHeaderMsgList = new ArrayList<>();
        for(int i = 0; i < headerCount.getValue(); i++){
            blockHeaderMsgList.add(BlockHeaderSimpleMsgSerializer.getInstance().deserialize(context,byteReader));
        }

        return BlockDetailsMsg.builder()
                .headerCount(headerCount)
                .headerMsg(blockHeaderMsgList)
                .merkleProofMsg(MerkleProofMsgSerializer.getInstance().deserialize(context, byteReader))
                .build();

    }

    @Override
    public void serialize(SerializerContext context, BlockDetailsMsg message, ByteArrayWriter byteWriter) {
        VarIntMsgSerializer.getInstance().serialize(context, message.getHeaderCount(), byteWriter);

        for(int i = 0; i < message.getHeaderCount().getValue(); i++){
            BlockHeaderSimpleMsgSerializer.getInstance().serialize(context, message.getHeaderMsg().get(i), byteWriter);
        }

        MerkleProofMsgSerializer.getInstance().serialize(context, message.getMerkleProofMsg(), byteWriter);
    }
}