package io.bitcoinsv.bsvcl.net.protocol.messages;

import com.google.common.base.Objects;
import io.bitcoinsv.bsvcl.net.protocol.messages.common.Message;

import java.util.List;



/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2021 nChain Ltd
 *
 * This message is used by the DsDetectedMsg, and contains a list of ancestor histories up to the point of the fork,
 * along with the merkle proof msg of the block that contains the double spend.
 */
public final class BlockDetailsMsg extends Message {

    public static final String MESSAGE_TYPE = "blockdetails";

    private VarIntMsg headerCount;
    private List<BlockHeaderSimpleMsg> headerMsg;
    private MerkleProofMsg merkleProofMsg;

    public BlockDetailsMsg(VarIntMsg headerCount, List<BlockHeaderSimpleMsg> headerMsg, MerkleProofMsg merkleProofMsg) {
        this.headerCount = headerCount;
        this.headerMsg = headerMsg;
        this.merkleProofMsg = merkleProofMsg;
        init();
    }

    @Override
    protected long calculateLength() {
        return headerCount.getLengthInBytes()
                + headerMsg.stream().mapToLong(h -> h.getLengthInBytes()).sum()
                + merkleProofMsg.getLengthInBytes();
    }

    @Override
    protected void validateMessage() {}

    @Override
    public String getMessageType()                      { return MESSAGE_TYPE; }
    public List<BlockHeaderSimpleMsg> getHeaderMsg()    { return headerMsg; }
    public VarIntMsg getHeaderCount()                   { return headerCount; }
    public MerkleProofMsg getMerkleProofMsg()           { return merkleProofMsg; }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) { return false; }
        BlockDetailsMsg other = (BlockDetailsMsg) obj;
        return Objects.equal(this.headerCount, other.headerCount)
                && Objects.equal(this.headerMsg, other.headerMsg)
                && Objects.equal(this.merkleProofMsg, other.merkleProofMsg);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), this.headerCount, this.headerMsg, this.merkleProofMsg);
    }

    public static BlockDetailsMsgBuilder builder(){
        return new BlockDetailsMsgBuilder();
    }

    public BlockDetailsMsgBuilder toBuilder() {
        return new BlockDetailsMsgBuilder()
                    .headerCount(this.headerCount)
                    .headerMsg(this.headerMsg)
                    .merkleProofMsg(this.merkleProofMsg);
    }

    /**
     * Builder
     */
    public static final class BlockDetailsMsgBuilder{
        private VarIntMsg headerCount;
        private List<BlockHeaderSimpleMsg> headerMsg;
        private MerkleProofMsg merkleProofMsg;

        private BlockDetailsMsgBuilder() {}

        public static BlockDetailsMsgBuilder BlockDetailsMsgBuilder() {
            return new BlockDetailsMsgBuilder();
        }

        public BlockDetailsMsgBuilder headerCount(VarIntMsg headerCount) {
            this.headerCount = headerCount;
            return this;
        }

        public BlockDetailsMsgBuilder headerMsg(List<BlockHeaderSimpleMsg> headerMsg) {
            this.headerMsg = headerMsg;
            return this;
        }

        public BlockDetailsMsgBuilder merkleProofMsg(MerkleProofMsg merkleProofMsg) {
            this.merkleProofMsg = merkleProofMsg;
            return this;
        }

        public BlockDetailsMsg build() {
            return new BlockDetailsMsg(headerCount, headerMsg, merkleProofMsg);
        }
    }
}