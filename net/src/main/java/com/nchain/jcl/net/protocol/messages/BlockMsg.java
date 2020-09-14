package com.nchain.jcl.net.protocol.messages;

import com.nchain.jcl.net.protocol.messages.common.Message;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.List;

import static com.google.common.base.Preconditions.checkState;

/**
 * @author n.srivastava@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 26/09/2019 14:53
 */

/**
 * <p>A block is a group of transactions, and is one of the fundamental data structures of the Bitcoin system.
 * * It records a set of {@link TxMsg}s together with some data that links it into a place in the global block
 * * chain, and proves that a difficult calculation was done over its contents. <p/>
 * *
 */
@Value
@EqualsAndHashCode
public class BlockMsg extends Message {

    public static final String MESSAGE_TYPE = "Block";

    private final BlockHeaderMsg blockHeader;
    private final List<TxMsg>  transactionMsg;

    // Constructor (specifying the Block Header and All Txs
    @Builder
    protected BlockMsg(BlockHeaderMsg blockHeader, List<TxMsg> transactionMsgs) {
        this.blockHeader = blockHeader;
        this.transactionMsg = transactionMsgs;
        init();
    }


    @Override
    protected long calculateLength() {
        long length = blockHeader.calculateLength();
        for (TxMsg transaction : transactionMsg)
            length += transaction.getLengthInBytes();
        return length;
    }

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }

    @Override
    protected void validateMessage() {
        checkState(blockHeader.getTransactionCount().getValue() == transactionMsg.size(),
                "The number of Txs must match the field in the 'txn_count'");
    }
}
