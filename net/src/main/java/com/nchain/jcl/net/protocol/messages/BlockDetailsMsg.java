package com.nchain.jcl.net.protocol.messages;

import com.nchain.jcl.net.protocol.messages.common.Message;

import java.util.List;


/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2021 nChain Ltd
 *
 * This message is used by the DsDetectedMsg, and contains a list of ancestor histories up to the point of the fork,
 * along with the merkle proof msg of the block that contains the double spend.
 */
public class BlockDetailsMsg extends Message {

    public static final String MESSAGE_TYPE = "blockdetails";

    private VarIntMsg headerCount;
    private List<BlockHeaderSimpleMsg> headerMsg;
    private MerkleProofMsg merkleProofMsg;

    public BlockDetailsMsg(VarIntMsg headerCount, List<BlockHeaderSimpleMsg> headerMsg, MerkleProofMsg merkleProofMsg, long payloadChecksum) {
        super(payloadChecksum);
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


    public static BlockDetailsMsgBuilder builder(){
        return new BlockDetailsMsgBuilder();
    }

    @Override
    public BlockDetailsMsgBuilder toBuilder() {
        return new BlockDetailsMsgBuilder()
                    .headerCount(this.headerCount)
                    .headerMsg(this.headerMsg)
                    .merkleProofMsg(this.merkleProofMsg);
    }

    /**
     * Builder
     */
    public static final class BlockDetailsMsgBuilder extends  MessageBuilder{
        private VarIntMsg headerCount;
        private List<BlockHeaderSimpleMsg> headerMsg;
        private MerkleProofMsg merkleProofMsg;

        private BlockDetailsMsgBuilder() {
        }

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
            return new BlockDetailsMsg(headerCount, headerMsg, merkleProofMsg, super.payloadChecksum);
        }
    }
}