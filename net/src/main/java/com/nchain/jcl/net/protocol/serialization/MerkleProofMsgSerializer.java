package com.nchain.jcl.net.protocol.serialization;

import com.nchain.jcl.net.protocol.messages.HashMsg;
import com.nchain.jcl.net.protocol.messages.MerkleProofMsg;
import com.nchain.jcl.net.protocol.messages.TxMsg;
import com.nchain.jcl.net.protocol.messages.VarIntMsg;
import com.nchain.jcl.net.protocol.messages.merkle.MerkleProofMsgFlags;
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
public class MerkleProofMsgSerializer implements MessageSerializer<MerkleProofMsg> {

    private static MerkleProofMsgSerializer instance;

    // Constructor
    private MerkleProofMsgSerializer() { }

    /** Returns an instance of this Serializer (Singleton) */
    public static MerkleProofMsgSerializer getInstance() {
        if (instance == null) {
            synchronized (MerkleProofMsgSerializer.class) {
                instance = new MerkleProofMsgSerializer();
            }
        }
        return instance;
    }

    @Override
    public MerkleProofMsg deserialize(DeserializerContext context, ByteArrayReader byteReader) {
        MerkleProofMsgFlags flags = new MerkleProofMsgFlags(byteReader.read());
        VarIntMsg txIndex = VarIntMsgSerializer.getInstance().deserialize(context, byteReader);
        VarIntMsg txLength = VarIntMsgSerializer.getInstance().deserialize(context, byteReader);
        HashMsg target = HashMsgSerializer.getInstance().deserialize(context, byteReader);
        VarIntMsg nodeCount = VarIntMsgSerializer.getInstance().deserialize(context, byteReader);
        TxMsg txMsg = TxMsgSerializer.getInstance().deserialize(context, byteReader);

        List<HashMsg> nodes = new ArrayList<>();
        for(int i = 0; i < nodeCount.getValue(); i++){
            nodes.add(HashMsgSerializer.getInstance().deserialize(context, byteReader));
        }

        return MerkleProofMsg.builder()
                .withFlags(flags)
                .withTransactionIndex(txIndex)
                .withTransactionLength(txLength)
                .withTransaction(txMsg)
                .withTarget(target)
                .withNodeCount(nodeCount)
                .withNodes(nodes)
                .build();

    }

    @Override
    public void serialize(SerializerContext context, MerkleProofMsg message, ByteArrayWriter byteWriter) {

    }
}