package com.nchain.jcl.net.protocol.serialization;


import com.nchain.jcl.net.protocol.messages.BlockHeaderMsg;
import com.nchain.jcl.net.protocol.messages.BlockHeaderSimpleMsg;
import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext;
import com.nchain.jcl.net.protocol.serialization.common.MessageSerializer;
import com.nchain.jcl.net.protocol.serialization.common.SerializerContext;
import com.nchain.jcl.tools.bytes.ByteArrayReader;
import com.nchain.jcl.tools.bytes.ByteArrayWriter;

/**
 * @author j.pomer@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */
public class BlockHeaderMsgSerializer implements MessageSerializer<BlockHeaderMsg> {

    protected static final int HEADER_LENGTH = 80; // Block header length (up to the "nonce" field, included)

    private static BlockHeaderMsgSerializer instance;

    protected BlockHeaderMsgSerializer() {
    }

    public static BlockHeaderMsgSerializer getInstance() {
        if (instance == null) {
            synchronized (BlockHeaderMsgSerializer.class) {
                instance = new BlockHeaderMsgSerializer();
            }
        }
        return instance;
    }

    @Override
    public BlockHeaderMsg deserialize(DeserializerContext context, ByteArrayReader byteReader) {

        BlockHeaderSimpleMsg blockSimpleMsg = BlockHeaderSimpleMsgSerializer.getInstance().deserialize(context, byteReader);
        var transactionCount = VarIntMsgSerializer.getInstance().deserialize(context, byteReader);

        return BlockHeaderMsg.builder()
            .hash(blockSimpleMsg.getHash())
            .version(blockSimpleMsg.getVersion())
            .prevBlockHash(blockSimpleMsg.getPrevBlockHash())
            .merkleRoot(blockSimpleMsg.getMerkleRoot())
            .creationTimestamp(blockSimpleMsg.getCreationTimestamp())
            .difficultyTarget(blockSimpleMsg.getDifficultyTarget())
            .nonce(blockSimpleMsg.getNonce())
            .transactionCount(transactionCount)
            .build();
    }

    @Override
    public void serialize(SerializerContext context, BlockHeaderMsg message, ByteArrayWriter byteWriter) {
        BlockHeaderSimpleMsgSerializer.getInstance().serialize(context, message.getBlockHeaderSimple(), byteWriter);
        VarIntMsgSerializer.getInstance().serialize(context, message.getTransactionCount(), byteWriter);
    }

}