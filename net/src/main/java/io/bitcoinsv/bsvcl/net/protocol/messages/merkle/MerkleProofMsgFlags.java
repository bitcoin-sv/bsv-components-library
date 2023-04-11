package io.bitcoinsv.bsvcl.net.protocol.messages.merkle;

/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2021 nChain Ltd
 * @date 25/08/2021
 */
public class MerkleProofMsgFlags {
    private final byte flag;
    private final MerkleProofMsgTxFlagType merkleProofMsgTxFlagType;
    private final MerkleProofMsgTargetFlagType merkleProofMsgTargetFlagType;
    private final MerkleProofMsgProofFlagType merkleProofMsgProofFlagType;
    private final MerkleProofMsgCompositeFlagType merkleProofMsgCompositeFlagType;

    public MerkleProofMsgFlags(byte flag){
        this.flag = flag;
        merkleProofMsgTxFlagType = MerkleProofMsgTxFlagType.fromCode(flag & 0x01);
        merkleProofMsgTargetFlagType = MerkleProofMsgTargetFlagType.fromCode(flag & (0x04 | 0x02));
        merkleProofMsgProofFlagType = MerkleProofMsgProofFlagType.fromCode(flag & 0x08);
        merkleProofMsgCompositeFlagType = MerkleProofMsgCompositeFlagType.fromCode(flag & 0x10);
    }

    public MerkleProofMsgTxFlagType getMerkleProofMsgTxFlagType() {
        return merkleProofMsgTxFlagType;
    }

    public MerkleProofMsgTargetFlagType getMerkleProofMsgTargetFlagType() {
        return merkleProofMsgTargetFlagType;
    }

    public MerkleProofMsgProofFlagType getMerkleProofMsgProofFlagType() {
        return merkleProofMsgProofFlagType;
    }

    public MerkleProofMsgCompositeFlagType getMerkleProofMsgCompositeFlagType() {
        return merkleProofMsgCompositeFlagType;
    }

    public byte getFlag() {
        return flag;
    }


}
