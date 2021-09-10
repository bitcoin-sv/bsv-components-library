package com.nchain.jcl.net.protocol.messages.merkle;

/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2021 nChain Ltd
 * @date 25/08/2021
 */
public class MerkleProofMsgFlags {
    private MerkleProofMsgTxFlagType merkleProofMsgTxFlagType;
    private MerkleProofMsgTargetFlagType merkleProofMsgTargetFlagType;
    private MerkleProofMsgProofFlagType merkleProofMsgProofFlagType;
    private MerkleProofMsgCompositeFlagType merkleProofMsgCompositeFlagType;

    public MerkleProofMsgFlags(byte flag){
        merkleProofMsgTxFlagType = MerkleProofMsgTxFlagType.fromCode(flag & 0x01);
        merkleProofMsgTargetFlagType = MerkleProofMsgTargetFlagType.fromCode(flag & (0x04 | 0x02));
        merkleProofMsgProofFlagType = MerkleProofMsgProofFlagType.fromCode(flag & 0x08);
        merkleProofMsgCompositeFlagType = MerkleProofMsgCompositeFlagType.fromCode(flag & 0x10);
    }

    public MerkleProofMsgTxFlagType getMerkleProofMsgTxFlagType() {
        return merkleProofMsgTxFlagType;
    }

    public void setMerkleProofMsgTxFlagType(MerkleProofMsgTxFlagType merkleProofMsgTxFlagType) {
        this.merkleProofMsgTxFlagType = merkleProofMsgTxFlagType;
    }

    public MerkleProofMsgTargetFlagType getMerkleProofMsgTargetFlagType() {
        return merkleProofMsgTargetFlagType;
    }

    public void setMerkleProofMsgTargetFlagType(MerkleProofMsgTargetFlagType merkleProofMsgTargetFlagType) {
        this.merkleProofMsgTargetFlagType = merkleProofMsgTargetFlagType;
    }

    public MerkleProofMsgProofFlagType getMerkleProofMsgProofFlagType() {
        return merkleProofMsgProofFlagType;
    }

    public void setMerkleProofMsgProofFlagType(MerkleProofMsgProofFlagType merkleProofMsgProofFlagType) {
        this.merkleProofMsgProofFlagType = merkleProofMsgProofFlagType;
    }

    public MerkleProofMsgCompositeFlagType getMerkleProofMsgCompositeFlagType() {
        return merkleProofMsgCompositeFlagType;
    }

    public void setMerkleProofMsgCompositeFlagType(MerkleProofMsgCompositeFlagType merkleProofMsgCompositeFlagType) {
        this.merkleProofMsgCompositeFlagType = merkleProofMsgCompositeFlagType;
    }
}
