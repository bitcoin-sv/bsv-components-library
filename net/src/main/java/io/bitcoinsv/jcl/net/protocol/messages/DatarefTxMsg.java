package io.bitcoinsv.jcl.net.protocol.messages;

import com.google.common.base.Objects;
import io.bitcoinsv.jcl.net.protocol.messages.common.BodyMessage;

import java.io.Serializable;

/**
 * Copyright (c) 2024 nChain Ltd
 * <br>
 * Dataref transaction message, which is usually a response to a {@code getdata} message containing the
 * {@link InventoryVectorMsg.VectorType#MSG_DATAREF_TX} vector type.
 *
 * <ul>
 *     <li>field: "txn" - the serialised dataref transaction in the standard transaction format as for the P2P {@code tx} message</li>
 *     <li>field: "merkle proof" - a proof that the above dataref transaction is included in a block</li>
 * </ul>
 *
 * <i>Field descriptions were taken from <a href="https://www.bsvblockchain.org/releases/bsv-blockchain-node-software-v1-0-13">BSV node v1.0.13 changelogs</a>.</i>
 *
 * @author nChain Ltd
 */
public final class DatarefTxMsg extends BodyMessage implements Serializable {
    public static final String MESSAGE_TYPE = "datareftx";

    private final TxMsg txMsg;
    private final MerkleProofMsg merkleProofMsg;

    public DatarefTxMsg(TxMsg txMsg, MerkleProofMsg merkleProofMsg, byte[] extraBytes, long checksum) {
        super(extraBytes, checksum);
        this.txMsg = txMsg;
        this.merkleProofMsg = merkleProofMsg;
        init();
    }

    @Override
    protected long calculateLength() {
        return txMsg.getLengthInBytes() +
            merkleProofMsg.getLengthInBytes();
    }

    @Override
    protected void validateMessage() {
        //needs validation?
    }

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }

    public TxMsg getTxMsg() {
        return txMsg;
    }

    public MerkleProofMsg getMerkleProofMsg() {
        return merkleProofMsg;
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) { return false; }
        DatarefTxMsg other = (DatarefTxMsg) obj;
        return Objects.equal(this.getTxMsg(), other.getTxMsg())
            && Objects.equal(this.getMerkleProofMsg(), other.getMerkleProofMsg());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), this.getTxMsg(), this.getMerkleProofMsg());
    }

    public static DatarefTxMsgBuilder builder(){
        return new DatarefTxMsgBuilder();
    }

    @Override
    public DatarefTxMsgBuilder toBuilder() {
        return new DatarefTxMsgBuilder(extraBytes, checksum)
            .txMsg(this.txMsg)
            .merkleProofMsg(this.merkleProofMsg);
    }

    /**
     * Builder
     */
    public static final class DatarefTxMsgBuilder extends BodyMessageBuilder {
        private TxMsg txMsg;
        private MerkleProofMsg merkleProofMsg;

        public DatarefTxMsgBuilder() {}

        public DatarefTxMsgBuilder(byte[] extraBytes, long checksum) { super(extraBytes, checksum);}

        public DatarefTxMsgBuilder txMsg(TxMsg txMsg) {
            this.txMsg = txMsg;
            return this;
        }

        public DatarefTxMsgBuilder merkleProofMsg(MerkleProofMsg merkleProofMsg) {
            this.merkleProofMsg = merkleProofMsg;
            return this;
        }

        public DatarefTxMsg build() {
            return new DatarefTxMsg(txMsg, merkleProofMsg, super.extraBytes, super.checksum);
        }
    }
}
