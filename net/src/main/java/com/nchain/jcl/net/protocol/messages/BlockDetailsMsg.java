package com.nchain.jcl.net.protocol.messages;

import com.nchain.jcl.net.protocol.messages.common.Message;

import java.util.List;


/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2021 nChain Ltd
 * @date 24/08/2021
 */
public class BlockDetailsMsg extends Message {

    public static final String MESSAGE_TYPE = "blockdetails";

    private VarIntMsg headerCount;
    private List<BlockHeaderMsg> headerMsg;
    private MerkleProofMsg merkleProofMsg;

    public BlockDetailsMsg() {
        init();
    }

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }

    @Override
    protected long calculateLength() {
        return 0;
    }

    @Override
    protected void validateMessage() {
    }


    public List<BlockHeaderMsg> getHeaderMsg() {
        return headerMsg;
    }

    public void setHeaderMsg(List<BlockHeaderMsg> headerMsg) {
        this.headerMsg = headerMsg;
    }

    public VarIntMsg getHeaderCount() {
        return headerCount;
    }

    public void setHeaderCount(VarIntMsg headerCount) {
        this.headerCount = headerCount;
    }

    public MerkleProofMsg getMerkleProofMsg() {
        return merkleProofMsg;
    }

    public void setMerkleProofMsg(MerkleProofMsg merkleProofMsg) {
        this.merkleProofMsg = merkleProofMsg;
    }

    public static BlockDetailsMsgBuilder builder(){
        return new BlockDetailsMsgBuilder();
    }

    public static final class BlockDetailsMsgBuilder {
        private VarIntMsg headerCount;
        private List<BlockHeaderMsg> headerMsg;
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

        public BlockDetailsMsgBuilder headerMsg(List<BlockHeaderMsg> headerMsg) {
            this.headerMsg = headerMsg;
            return this;
        }

        public BlockDetailsMsgBuilder merkleProofMsg(MerkleProofMsg merkleProofMsg) {
            this.merkleProofMsg = merkleProofMsg;
            return this;
        }

        public BlockDetailsMsg build() {
            BlockDetailsMsg blockDetailsMsg = new BlockDetailsMsg();
            blockDetailsMsg.setHeaderCount(headerCount);
            blockDetailsMsg.setHeaderMsg(headerMsg);
            blockDetailsMsg.setMerkleProofMsg(merkleProofMsg);
            return blockDetailsMsg;
        }
    }
}