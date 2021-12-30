package io.bitcoinsv.jcl.net.protocol.serialization;

import io.bitcoinsv.jcl.net.protocol.messages.HashMsg;
import io.bitcoinsv.jcl.net.protocol.messages.MerkleProofMsg;
import io.bitcoinsv.jcl.net.protocol.messages.TxMsg;
import io.bitcoinsv.jcl.net.protocol.messages.VarIntMsg;
import io.bitcoinsv.jcl.net.protocol.messages.merkle.MerkleNode;
import io.bitcoinsv.jcl.net.protocol.messages.merkle.MerkleProofMsgFlags;
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
        TxMsg txMsg = TxMsgSerializer.getInstance().deserialize(context, byteReader);
        HashMsg target = HashMsgSerializer.getInstance().deserialize(context, byteReader);
        VarIntMsg nodeCount = VarIntMsgSerializer.getInstance().deserialize(context, byteReader);

        List<MerkleNode> nodes = new ArrayList<>();
        for(int i = 0; i < nodeCount.getValue(); i++){
            VarIntMsg nodeType = VarIntMsgSerializer.getInstance().deserialize(context, byteReader);
            HashMsg nodeHash = HashMsgSerializer.getInstance().deserialize(context, byteReader);

            nodes.add(new MerkleNode(nodeType, nodeHash));
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
        byteWriter.write(message.getFlags().getFlag());
        VarIntMsgSerializer.getInstance().serialize(context, message.getTransactionIndex(), byteWriter);
        VarIntMsgSerializer.getInstance().serialize(context, message.getTransactionLength(), byteWriter);
        TxMsgSerializer.getInstance().serialize(context, message.getTransaction(), byteWriter);
        HashMsgSerializer.getInstance().serialize(context, message.getTarget(), byteWriter);
        VarIntMsgSerializer.getInstance().serialize(context, message.getNodeCount(), byteWriter);

        for(MerkleNode merkleNode: message.getNodes()){
           VarIntMsgSerializer.getInstance().serialize(context, merkleNode.getType(), byteWriter);
           HashMsgSerializer.getInstance().serialize(context, merkleNode.getHash(), byteWriter);
        }
    }
}