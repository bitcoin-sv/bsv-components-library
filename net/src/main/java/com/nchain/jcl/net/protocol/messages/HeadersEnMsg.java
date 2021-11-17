package com.nchain.jcl.net.protocol.messages;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.nchain.jcl.net.protocol.messages.common.Message;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * The headersen message sends block headers to a node which requested certain headers with a getheadersen message.
 * This message returns same data as getheaders message with the addition of fields for actual number of transactions
 * that are included in the block and proof of inclussion for coinbase transaction along with the whole coinbase transaction.
 *
 * Structure of the BODY of Message:
 *
 * - field: "count" (1+ bytes) var_int
 *   Number of "block_header enriched"  entries (max: 2000 entries)
 *
 * - field: "block_header_ enriched" (90+ * MAX_ADDRESS bytes) block_header[]
 *   Array of headeren messages.
 */
public final class HeadersEnMsg extends Message implements Serializable {
    private static final long MAX_ADDRESSES = 2000;
    public static final String MESSAGE_TYPE = "headersen";

    private final VarIntMsg count;
    private final List<BlockHeaderEnMsg> blockHeaderEnMsgList;

    /**
     * Creates the HeadersEnMsg Object. Use the corresponding byteArray to create the instance.
     *
     * @param blockHeaderEnMsgList
     */
    public HeadersEnMsg(List<BlockHeaderEnMsg> blockHeaderEnMsgList, long payloadChecksum) {
        super(payloadChecksum);
        this.blockHeaderEnMsgList = blockHeaderEnMsgList.stream().collect(Collectors.toUnmodifiableList());
        this.count = VarIntMsg.builder().value(blockHeaderEnMsgList.size()).build();
        init();
    }

    @Override
    protected long calculateLength() {
        long length = count.getLengthInBytes() ;

        for (BlockHeaderEnMsg blockHeaderEnMsg :blockHeaderEnMsgList) {
            length += blockHeaderEnMsg.calculateLength();
        }
        return length;
    }

    @Override
    protected void validateMessage() {
        Preconditions.checkArgument(count.getValue() <= MAX_ADDRESSES,"Headers message exceeds maximum size");
        Preconditions.checkArgument(count.getValue() ==  blockHeaderEnMsgList.size(), "Headers list size and count value are not the same.");
    }

    @Override
    public String getMessageType()                          { return MESSAGE_TYPE; }
    public VarIntMsg getCount()                             { return this.count; }
    public List<BlockHeaderEnMsg> getBlockHeaderEnMsgList() { return this.blockHeaderEnMsgList; }

    @Override
    public int hashCode() {
        return Objects.hashCode(count, blockHeaderEnMsgList);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) { return false; }
        if (obj == this) { return true; }
        if (obj.getClass() != getClass()) { return false; }
        HeadersEnMsg other = (HeadersEnMsg) obj;
        return Objects.equal(this.count, other.count)
                && Objects.equal(this.blockHeaderEnMsgList, other.blockHeaderEnMsgList);
    }

    public String toString() {
        return "HeadersEnMsg(count=" + this.getCount() + ", blockHeaderEnMsgList=" + this.getBlockHeaderEnMsgList() + ")";
    }

    public static HeadersEnMsgBuilder builder() {
        return new HeadersEnMsgBuilder();
    }

    @Override
    public HeadersEnMsgBuilder toBuilder() {
        return new HeadersEnMsgBuilder().blockHeaderEnMsgList(this.blockHeaderEnMsgList);
    }

    /**
     * Builder
     */
    public static class HeadersEnMsgBuilder extends MessageBuilder {
        private List<BlockHeaderEnMsg> blockHeaderEnMsgList;

        HeadersEnMsgBuilder() {}

        public HeadersEnMsg.HeadersEnMsgBuilder blockHeaderEnMsgList(List<BlockHeaderEnMsg> blockHeaderEnMsgList) {
            this.blockHeaderEnMsgList = blockHeaderEnMsgList;
            return this;
        }

        public HeadersEnMsg build() {
            return new HeadersEnMsg(blockHeaderEnMsgList, payloadChecksum);
        }
    }
}
