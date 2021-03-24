package com.nchain.jcl.net.protocol.serialization;


import com.nchain.jcl.net.protocol.messages.CompleteBlockHeaderMsg;
import com.nchain.jcl.net.protocol.messages.HashMsg;
import com.nchain.jcl.net.protocol.messages.VarIntMsg;
import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext;
import com.nchain.jcl.net.protocol.serialization.common.SerializerContext;
import com.nchain.jcl.tools.bytes.ByteArrayReader;
import com.nchain.jcl.tools.bytes.ByteArrayWriter;

/**
 * @author j.pomer@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */
public class CompleteBlockHeaderMsgSerializer extends BlockHeaderMsgSerializer<CompleteBlockHeaderMsg> {
    private static CompleteBlockHeaderMsgSerializer instance;

    /**
     * Returns the instance of this Serializer (Singleton)
     */
    public static CompleteBlockHeaderMsgSerializer getInstance() {
        if (instance == null) {
            synchronized (CompleteBlockHeaderMsgSerializer.class) {
                instance = new CompleteBlockHeaderMsgSerializer();
            }
        }
        return instance;
    }

    @Override
    protected CompleteBlockHeaderMsg build(HashMsg hash, long version, HashMsg prevBlockHash, HashMsg merkleRoot, long creationTimestamp, long difficultyTarget, long nonce, VarIntMsg transactionCount) {
        return CompleteBlockHeaderMsg.builder()
            .hash(hash)
            .version(version)
            .prevBlockHash(prevBlockHash)
            .merkleRoot(merkleRoot)
            .creationTimestamp(creationTimestamp)
            .difficultyTarget(difficultyTarget)
            .nonce(nonce)
            .transactionCount(transactionCount)
            .build();
    }

    @Override
    protected VarIntMsg deserializeTransactionCount(DeserializerContext context, ByteArrayReader byteReader) {
        return VarIntMsgSerializer.getInstance().deserialize(context, byteReader);
    }

    @Override
    protected void serializeTransactionCount(SerializerContext context, CompleteBlockHeaderMsg message, ByteArrayWriter byteWriter) {
        VarIntMsgSerializer.getInstance().serialize(context, message.getTransactionCount(), byteWriter);
    }
}
