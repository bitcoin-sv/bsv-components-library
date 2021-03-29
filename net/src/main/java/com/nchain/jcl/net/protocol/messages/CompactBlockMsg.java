package com.nchain.jcl.net.protocol.messages;

import com.nchain.jcl.net.protocol.messages.common.Message;

import java.util.List;

/**
 * @author j.pomer@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */
public class CompactBlockMsg extends Message {

    public static final String MESSAGE_TYPE = "cmpctblock";

    private final BlockHeaderMsg header;
    private final long nonce;
    private final List<Long> shortTxIds;
    private final List<PrefilledTxMsg> prefilledTransactions;

    public CompactBlockMsg(BlockHeaderMsg header, long nonce, List<Long> shortTxIds, List<PrefilledTxMsg> prefilledTransactions) {
        this.header = header;
        this.nonce = nonce;
        this.shortTxIds = shortTxIds;
        this.prefilledTransactions = prefilledTransactions;
    }

    public static CompactBlockMsgBuilder builder() {
        return new CompactBlockMsgBuilder();
    }

    public BlockHeaderMsg getHeader() {
        return header;
    }

    public long getNonce() {
        return nonce;
    }

    public List<Long> getShortTxIds() {
        return shortTxIds;
    }

    public List<PrefilledTxMsg> getPrefilledTransactions() {
        return prefilledTransactions;
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

    public static class CompactBlockMsgBuilder {
        private BlockHeaderMsg header;
        private long nonce;
        private List<Long> shortTxIds;
        private List<PrefilledTxMsg> prefilledTransactions;

        public CompactBlockMsgBuilder header(BlockHeaderMsg header) {
            this.header = header;
            return this;
        }

        public CompactBlockMsgBuilder nonce(long nonce) {
            this.nonce = nonce;
            return this;
        }

        public CompactBlockMsgBuilder shortTxIds(List<Long> shortTxIds) {
            this.shortTxIds = shortTxIds;
            return this;
        }

        public CompactBlockMsgBuilder prefilledTransactions(List<PrefilledTxMsg> prefilledTransactions) {
            this.prefilledTransactions = prefilledTransactions;
            return this;
        }

        public CompactBlockMsg build() {
            return new CompactBlockMsg(header, nonce, shortTxIds, prefilledTransactions);
        }
    }
}
