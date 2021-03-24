package com.nchain.jcl.net.protocol.messages;

import com.nchain.jcl.net.protocol.messages.common.Message;

/**
 * @author j.pomer@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */
public class CompactBlockMsg extends Message {

    public static final String MESSAGE_TYPE = "cmpctblock";

    private final BasicBlockHeaderMsg header;
    private final long nonce;
    private final long[] shortTxIds;
    private final PrefilledTxMsg[] prefilledTransactions;

    public CompactBlockMsg(BasicBlockHeaderMsg header, long nonce, long[] shortTxIds, PrefilledTxMsg[] prefilledTransactions) {
        this.header = header;
        this.nonce = nonce;
        this.shortTxIds = shortTxIds;
        this.prefilledTransactions = prefilledTransactions;
    }

    public static CompactBlockMsgBuilder builder() {
        return new CompactBlockMsgBuilder();
    }

    public BasicBlockHeaderMsg getHeader() {
        return header;
    }

    public long getNonce() {
        return nonce;
    }

    public long[] getShortTxIds() {
        return shortTxIds;
    }

    public PrefilledTxMsg[] getPrefilledTransactions() {
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
        private BasicBlockHeaderMsg header;
        private long nonce;
        private long[] shortTxIds;
        private PrefilledTxMsg[] prefilledTransactions;

        public CompactBlockMsgBuilder header(BasicBlockHeaderMsg header) {
            this.header = header;
            return this;
        }

        public CompactBlockMsgBuilder nonce(long nonce) {
            this.nonce = nonce;
            return this;
        }

        public CompactBlockMsgBuilder shortTxIds(long[] shortTxIds) {
            this.shortTxIds = shortTxIds;
            return this;
        }

        public CompactBlockMsgBuilder prefilledTransactions(PrefilledTxMsg[] prefilledTransactions) {
            this.prefilledTransactions = prefilledTransactions;
            return this;
        }

        public CompactBlockMsg build() {
            return new CompactBlockMsg(header, nonce, shortTxIds, prefilledTransactions);
        }
    }
}
