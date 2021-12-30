package io.bitcoinsv.jcl.net.protocol.messages.merkle;

/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2021 nChain Ltd
 * @date 25/08/2021
 */
public enum MerkleProofMsgProofFlagType {
    MERKLE_BRANCH(0),
    MERKLE_TREE(1);

    int flag;

    MerkleProofMsgProofFlagType(int flag) {
        this.flag = flag;
    }

    public static MerkleProofMsgProofFlagType fromCode(int flag) {
        for (MerkleProofMsgProofFlagType flagType : MerkleProofMsgProofFlagType.values())
            if (flagType.flag == flag)
                return flagType;
        return MERKLE_BRANCH;
    }
}
