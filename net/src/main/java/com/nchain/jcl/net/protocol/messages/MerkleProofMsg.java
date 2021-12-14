package com.nchain.jcl.net.protocol.messages;

import com.nchain.jcl.net.protocol.messages.common.Message;
import com.nchain.jcl.net.protocol.messages.merkle.MerkleNode;
import com.nchain.jcl.net.protocol.messages.merkle.MerkleProofMsgFlags;
import io.bitcoinsv.bitcoinjsv.core.Sha256Hash;

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
    public String getMessageType()                  { return MESSAGE_TYPE; }
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
    protected void validateMessage()                {}

    public MerkleProofMsgFlags getFlags()           { return flags; }
    public VarIntMsg getTransactionIndex()          { return transactionIndex; }
    public VarIntMsg getTransactionLength()         { return transactionLength; }
    public TxMsg getTransaction()                   { return transaction; }
    public HashMsg getTarget()                      { return target; }
    public VarIntMsg getNodeCount()                 { return nodeCount; }
    public void setNodeCount(VarIntMsg nodeCount)   { this.nodeCount = nodeCount; }
    public List<MerkleNode> getNodes()              { return nodes; }


    public MerkleProofMsgBuilder toBuilder() {
        return new MerkleProofMsgBuilder()
                    .withFlags(this.flags)
                    .withTransactionIndex(this.transactionIndex)
                    .withTransactionLength(this.transactionLength)
                    .withTransaction(this.transaction)
                    .withTarget(this.target)
                    .withNodeCount(this.nodeCount)
                    .withNodes(this.nodes);
    }

    /**
     * Builder
     */
    public static final class MerkleProofMsgBuilder {
        private MerkleProofMsgFlags flags;
        private VarIntMsg transactionIndex;
        private VarIntMsg transactionLength;
        private TxMsg transaction;
        private HashMsg target;
        private VarIntMsg nodeCount;
        private List<MerkleNode> nodes;

        private MerkleProofMsgBuilder() {}

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
            MerkleProofMsg merkleProofMsg = new MerkleProofMsg(
                    this.flags,
                    this.transactionIndex,
                    this.transactionLength,
                    this.transaction,
                    this.target,
                    this.nodeCount,
                    this.nodes
            );
            return merkleProofMsg;
        }
    }
}