package io.bitcoinsv.jcl.net.protocol.messages.merkle;


/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2021 nChain Ltd
 * @date 25/08/2021
 */
public enum MerkleProofMsgCompositeFlagType {
    SINGLE_PROOF(0),
    COMPOSITE_PROOF(1);

    int flag;

    MerkleProofMsgCompositeFlagType(int flag) {
        this.flag = flag;
    }

    public static MerkleProofMsgCompositeFlagType fromCode(int flag) {
        for (MerkleProofMsgCompositeFlagType flagType : MerkleProofMsgCompositeFlagType.values())
            if (flagType.flag == flag)
                return flagType;
        return SINGLE_PROOF;
    }
}
