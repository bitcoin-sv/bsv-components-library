package com.nchain.jcl.net.protocol.messages;

import com.nchain.jcl.net.protocol.messages.common.Message;

import java.io.Serializable;
import java.util.List;

/**
 * @author j.pomer@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */
public class BlockTxnMsg extends Message implements Serializable {
    public static final String MESSAGE_TYPE = "blocktxn";

    private final HashMsg blockHash;
    private final List<TxMsg> transactions;

    public BlockTxnMsg(HashMsg blockHash, List<TxMsg> transactions) {
        this.blockHash = blockHash;
        this.transactions = transactions;
        init();
    }

    public static BlockTxnMsgBuilder builder() {
        return new BlockTxnMsgBuilder();
    }

    @Override
    public String getMessageType()          { return MESSAGE_TYPE; }
    public HashMsg getBlockHash()           { return blockHash; }
    public List<TxMsg> getTransactions()    { return transactions; }



    @Override
    protected long calculateLength() {
        return blockHash.calculateLength()
            + VarIntMsg.builder().value(transactions.size()).build().calculateLength()
            + transactions.stream()
            .mapToLong(TxMsg::calculateLength)
            .sum();
    }

    @Override
    protected void validateMessage() {}

    @Override
    public BlockTxnMsgBuilder toBuilder() {
        return new BlockTxnMsgBuilder()
                    .blockHash(this.blockHash)
                    .transactions(this.transactions);
    }

    public static class BlockTxnMsgBuilder extends MessageBuilder {
        private HashMsg blockHash;
        private List<TxMsg> transactions;

        public BlockTxnMsgBuilder blockHash(HashMsg blockHash) {
            this.blockHash = blockHash;
            return this;
        }

        public BlockTxnMsgBuilder transactions(List<TxMsg> transactions) {
            this.transactions = transactions;
            return this;
        }

        public BlockTxnMsg build() {
            return new BlockTxnMsg(blockHash, transactions);
        }
    }
}
