package com.nchain.jcl.net.protocol.messages.merkle;

/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2021 nChain Ltd
 * @date 25/08/2021
 */
public enum MerkleProofMsgTxFlagType {
    TX_ID(0),
    TX(1);

    int flag;

    MerkleProofMsgTxFlagType(int flag) {
        this.flag = flag;
    }

    public static MerkleProofMsgTxFlagType fromCode(int flag) {
        for (MerkleProofMsgTxFlagType flagType : MerkleProofMsgTxFlagType.values())
            if (flagType.flag == flag)
                return flagType;
        return TX_ID;
    }
}
