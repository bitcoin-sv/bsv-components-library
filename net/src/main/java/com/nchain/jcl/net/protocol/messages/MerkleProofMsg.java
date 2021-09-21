package com.nchain.jcl.net.protocol.messages;

import com.nchain.jcl.net.protocol.messages.common.Message;
import com.nchain.jcl.net.protocol.messages.merkle.MerkleNode;
import com.nchain.jcl.net.protocol.messages.merkle.MerkleProofMsgFlags;
import io.bitcoinj.core.Sha256Hash;

import java.util.List;


/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2021 nChain Ltd
 * @date 24/08/2021
 *
 * The merkle proof message contains the transaction, a merkle nodes in order to verify the transaction was a member of a given header. The flag field indicates the structure of the message that will be deserialized,
 * but in this case, it's currently hardcoded and the message structure will not change. But, it's possible to generate similar merkle proof messages that contain different pieces of information by amending the flag,
 * see: https://tsc.bitcoinassociation.net/standards/merkle-proof-standardised-format/
 */
public class MerkleProofMsg extends Message {

    public static final String MESSAGE_TYPE = "merkleproof";

    private MerkleProofMsgFlags flags;
    private VarIntMsg transactionIndex;
    private VarIntMsg transactionLength;
    private TxMsg transaction;
    private HashMsg target;
    private VarIntMsg nodeCount;
    private List<MerkleNode> nodes;

    public MerkleProofMsg(MerkleProofMsgFlags flags,
                          VarIntMsg transactionIndex,
                          VarIntMsg transactionLength,
                          TxMsg transaction,
                          HashMsg target,
                          VarIntMsg nodeCount,
                          List<MerkleNode> nodes) {
        this.flags = flags;
        this.transactionIndex = transactionIndex;
        this.transactionLength = transactionLength;
        this.transaction = transaction;
        this.target = target;
        this.nodeCount = nodeCount;
        this.nodes = nodes;
        init();
    }

    public static MerkleProofMsg.MerkleProofMsgBuilder builder() {
        return new MerkleProofMsg.MerkleProofMsgBuilder();
    }

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }

    @Override
    protected long calculateLength() {
        return  1
                + transactionIndex.getLengthInBytes()
                + transactionLength.getLengthInBytes()
                + transaction.getLengthInBytes()
                + target.getLengthInBytes()
                + nodeCount.getLengthInBytes()
                + nodes.stream().mapToLong(n -> n.getType().getLengthInBytes() + n.getHash().getLengthInBytes()).sum();
    }

    @Override
    protected void validateMessage() {
    }

    public MerkleProofMsgFlags getFlags() {
        return flags;
    }

    public void setFlags(MerkleProofMsgFlags flags) {
        this.flags = flags;
    }

    public VarIntMsg getTransactionIndex() {
        return transactionIndex;
    }

    public void setTransactionIndex(VarIntMsg transactionIndex) {
        this.transactionIndex = transactionIndex;
    }

    public VarIntMsg getTransactionLength() {
        return transactionLength;
    }

    public void setTransactionLength(VarIntMsg transactionLength) {
        this.transactionLength = transactionLength;
    }

    public TxMsg getTransaction() {
        return transaction;
    }

    public void setTransaction(TxMsg transaction) {
        this.transaction = transaction;
    }

    public HashMsg getTarget() {
        return target;
    }

    public void setTarget(HashMsg target) {
        this.target = target;
    }

    public VarIntMsg getNodeCount() {
        return nodeCount;
    }

    public void setNodeCount(VarIntMsg nodeCount) {
        this.nodeCount = nodeCount;
    }

    public List<MerkleNode> getNodes() {
        return nodes;
    }

    public void setNodes(List<MerkleNode> nodes) {
        this.nodes = nodes;
    }

    public static final class MerkleProofMsgBuilder {
        private MerkleProofMsgFlags flags;
        private VarIntMsg transactionIndex;
        private VarIntMsg transactionLength;
        private TxMsg transaction;
        private HashMsg target;
        private VarIntMsg nodeCount;
        private List<MerkleNode> nodes;

        private MerkleProofMsgBuilder() {
        }

        public static MerkleProofMsgBuilder aMerkleProofMsg() {
            return new MerkleProofMsgBuilder();
        }

        public MerkleProofMsgBuilder withFlags(MerkleProofMsgFlags flags) {
            this.flags = flags;
            return this;
        }

        public MerkleProofMsgBuilder withTransactionIndex(VarIntMsg transactionIndex) {
            this.transactionIndex = transactionIndex;
            return this;
        }

        public MerkleProofMsgBuilder withTransactionLength(VarIntMsg transactionLength) {
            this.transactionLength = transactionLength;
            return this;
        }

        public MerkleProofMsgBuilder withTransaction(TxMsg transaction) {
            this.transaction = transaction;
            return this;
        }

        public MerkleProofMsgBuilder withTarget(HashMsg target) {
            this.target = target;
            return this;
        }

        public MerkleProofMsgBuilder withNodeCount(VarIntMsg nodeCount) {
            this.nodeCount = nodeCount;
            return this;
        }

        public MerkleProofMsgBuilder withNodes(List<MerkleNode> nodes) {
            this.nodes = nodes;
            return this;
        }

        public MerkleProofMsg build() {
            return new MerkleProofMsg(flags, transactionIndex, transactionLength, transaction, target, nodeCount, nodes);
        }
    }


}