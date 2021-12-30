package io.bitcoinsv.jcl.net.protocol.messages.merkle;

/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2021 nChain Ltd
 * @date 25/08/2021
 */
public enum MerkleProofMsgTargetFlagType {
    BLOCK_HASH(0),
    BLOCK_HEADER(2),
    MERKLE_ROOT(4),
    NOT_VALID(8);

    int flag;

    MerkleProofMsgTargetFlagType(int flag) {
        this.flag = flag;
    }

    public static MerkleProofMsgTargetFlagType fromCode(int flag) {
        for (MerkleProofMsgTargetFlagType flagType : MerkleProofMsgTargetFlagType.values())
            if (flagType.flag == flag)
                return flagType;
        return BLOCK_HASH;
    }
}
