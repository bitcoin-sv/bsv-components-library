package com.nchain.jcl.net.protocol.messages;

import com.nchain.jcl.net.protocol.messages.common.BodyMessage;
import com.nchain.jcl.net.protocol.messages.common.Message;

import java.io.Serializable;
import java.util.List;

/**
 * @author j.pomer@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */
public class CompactBlockMsg extends BodyMessage implements Serializable {

    public static final String MESSAGE_TYPE = "cmpctblock";

    private static final long NONCE_BYTES = 8;
    private static final long HOST_TX_ID_BYTES = 6;

    private final CompactBlockHeaderMsg header;
    private final long nonce;
    private final List<Long> shortTxIds;
    private final List<PrefilledTxMsg> prefilledTransactions;

    public CompactBlockMsg(CompactBlockHeaderMsg header,
                           long nonce,
                           List<Long> shortTxIds,
                           List<PrefilledTxMsg> prefilledTransactions,
                           byte[] extraBytes, long checksum) {
        super(extraBytes, checksum);
        this.header = header;
        this.nonce = nonce;
        this.shortTxIds = shortTxIds;
        this.prefilledTransactions = prefilledTransactions;
        init();
    }

    @Override
    public String getMessageType()                          { return MESSAGE_TYPE; }

    public CompactBlockHeaderMsg getHeader()                { return header; }
    public long getNonce()                                  { return nonce; }
    public List<Long> getShortTxIds()                       { return shortTxIds; }
    public List<PrefilledTxMsg> getPrefilledTransactions()  { return prefilledTransactions; }

    @Override
    protected long calculateLength() {
        return header.calculateLength()
            + NONCE_BYTES
            + VarIntMsg.builder().value(shortTxIds.size()).build().calculateLength()
            + HOST_TX_ID_BYTES * shortTxIds.size()
            + VarIntMsg.builder().value(prefilledTransactions.size()).build().calculateLength()
            + prefilledTransactions.stream().mapToLong(PrefilledTxMsg::calculateLength).sum();
    }

    @Override
    protected void validateMessage() {
    }

    @Override
    public CompactBlockMsgBuilder toBuilder() {
        return new CompactBlockMsgBuilder(super.extraBytes, super.checksum)
                    .header(this.header)
                    .nonce(this.nonce)
                    .shortTxIds(this.shortTxIds)
                    .prefilledTransactions(this.prefilledTransactions);
    }

    public static CompactBlockMsgBuilder builder() {
        return new CompactBlockMsgBuilder();
    }

    /**
     * Builder
     */
    public static class CompactBlockMsgBuilder extends BodyMessageBuilder {
        private CompactBlockHeaderMsg header;
        private long nonce;
        private List<Long> shortTxIds;
        private List<PrefilledTxMsg> prefilledTransactions;

        public CompactBlockMsgBuilder() {}
        public CompactBlockMsgBuilder(byte[] extraBytes, long checksum) { super(extraBytes, checksum);}

        public CompactBlockMsgBuilder header(CompactBlockHeaderMsg header) {
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
            return new CompactBlockMsg(header, nonce, shortTxIds, prefilledTransactions, super.extraBytes, super.checksum);
        }
    }
}
