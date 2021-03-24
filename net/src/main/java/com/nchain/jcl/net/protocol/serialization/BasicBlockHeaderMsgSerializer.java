package com.nchain.jcl.net.protocol.serialization;


import com.nchain.jcl.net.protocol.messages.BasicBlockHeaderMsg;
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
public class BasicBlockHeaderMsgSerializer extends BlockHeaderMsgSerializer<BasicBlockHeaderMsg> {
    private static BasicBlockHeaderMsgSerializer instance;

    /**
     * Returns the instance of this Serializer (Singleton)
     */
    public static BasicBlockHeaderMsgSerializer getInstance() {
        if (instance == null) {
            synchronized (BasicBlockHeaderMsgSerializer.class) {
                instance = new BasicBlockHeaderMsgSerializer();
            }
        }
        return instance;
    }

    @Override
    protected BasicBlockHeaderMsg build(HashMsg hash, long version, HashMsg prevBlockHash, HashMsg merkleRoot, long creationTimestamp, long difficultyTarget, long nonce, VarIntMsg transactionCount) {
        return BasicBlockHeaderMsg.builder()
            .hash(hash)
            .version(version)
            .prevBlockHash(prevBlockHash)
            .merkleRoot(merkleRoot)
            .creationTimestamp(creationTimestamp)
            .difficultyTarget(difficultyTarget)
            .nonce(nonce)
            .build();
    }

    @Override
    protected VarIntMsg deserializeTransactionCount(DeserializerContext context, ByteArrayReader byteReader) {
        return VarIntMsg.builder().value(0).build();
    }

    @Override
    protected void serializeTransactionCount(SerializerContext context, BasicBlockHeaderMsg message, ByteArrayWriter byteWriter) {
    }
}
