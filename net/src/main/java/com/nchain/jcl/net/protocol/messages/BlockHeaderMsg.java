package com.nchain.jcl.net.protocol.messages;

import com.nchain.jcl.net.protocol.messages.common.Message;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;


/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-03-23 14:57
 *
 * The Block Header represents the Header of information wihtint a Block.
 * It's also used in the HEADERS and GETHEADERS packages.
 */
@Value
@EqualsAndHashCode
public class BlockHeaderMsg extends Message {

    public static final String MESSAGE_TYPE = "BlockHeader";
    public static final int TIMESTAMP_LENGTH = 4;
    public static final int NONCE_LENGTH = 4;

    // IMPORTANT: This field (hash) is NOT SERIALIZED.
    // The hash of the block is NOT part of the BLOCK Message itself: its external to it.
    // In order to calculate a Block Hash we need to serialize the Block first, so instead of doing
    // that avery time we need a Hash, we store the Hash here, at the moment when we deserialize the
    // Block for the first time, so its available for further use.
    private final HashMsg hash;


    private final long version;
    private final HashMsg prevBlockHash;
    private final HashMsg merkleRoot;
    private final long creationTimestamp;
    private final long difficultyTarget;
    private final long nonce;
    private final VarIntMsg transactionCount;


    // Constructor (specifying the Hash of this Block)
    @Builder
    public BlockHeaderMsg(HashMsg hash, long version, HashMsg prevBlockHash, HashMsg merkleRoot,
                          long creationTimestamp, long difficultyTarget, long nonce,
                          long transactionCount) {
        this.hash = hash;
        this.version = version;
        this.prevBlockHash = prevBlockHash;
        this.merkleRoot = merkleRoot;
        this.creationTimestamp = creationTimestamp;
        this.difficultyTarget = difficultyTarget;
        this.nonce = nonce;
        this.transactionCount = VarIntMsg.builder().value(transactionCount).build();
        init();
    }

    @Override
    protected long calculateLength() {
        long length = 4 + prevBlockHash.getLengthInBytes() + merkleRoot.getLengthInBytes() +
                TIMESTAMP_LENGTH + TIMESTAMP_LENGTH + NONCE_LENGTH + transactionCount.getLengthInBytes();
        return length;
    }

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }

    @Override
    protected void validateMessage() {}
}
