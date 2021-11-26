package com.nchain.jcl.net.protocol.messages;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.nchain.jcl.net.protocol.messages.common.BodyMessage;
import com.nchain.jcl.net.protocol.messages.common.Message;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;


/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Allows a node to advertise its knowledge of one or more objects, and is received in response to the "getheaders" message.
 *
 * Structure of the BODY of Message:
 *
 *  - field: "count" (1+ bytes) var_int
 *   Number of Inventory entries (max: 2000 entries)
 *
 * - field: "block_header" (81+ * MAX_ADDRESS bytes) block_header[]
 *   Array of header messages.
 *
 */
public final class HeadersMsg extends BodyMessage implements Serializable {
    private static final long MAX_ADDRESSES = 2000;
    public static final String MESSAGE_TYPE = "headers";

    private final VarIntMsg count;

    private final List<BlockHeaderMsg> blockHeaderMsgList;

    /**
     * Creates the HeadersMsg Object. Use the corresponding byteArray to create the instance.
     *
     * @param blockHeaderMsgList
     */
    protected HeadersMsg(List<BlockHeaderMsg> blockHeaderMsgList,
                         byte[] extraBytes, long checksum) {
        super(extraBytes, checksum);
        this.blockHeaderMsgList = blockHeaderMsgList.stream().collect(Collectors.toUnmodifiableList());
        this.count = VarIntMsg.builder().value(blockHeaderMsgList.size()).build();
        init();
    }

    @Override
    protected long calculateLength() {
        long length = count.getLengthInBytes() + blockHeaderMsgList.stream().mapToLong(h -> h.getLengthInBytes()).sum();
        return length;
    }

    @Override
    protected void validateMessage() {
        Preconditions.checkArgument(count.getValue() <= MAX_ADDRESSES,"Headers message exceeds maximum size");
        Preconditions.checkArgument(count.getValue() ==  blockHeaderMsgList.size(), "Headers list size and count value are not the same.");
    }

    @Override
    public String getMessageType()                      { return MESSAGE_TYPE; }
    public VarIntMsg getCount()                         { return this.count; }
    public List<BlockHeaderMsg> getBlockHeaderMsgList() { return this.blockHeaderMsgList; }

    @Override
    public int hashCode() {
        return Objects.hashCode(count, blockHeaderMsgList);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) { return false; }
        if (obj == this) { return true; }
        if (obj.getClass() != getClass()) { return false; }
        HeadersMsg other = (HeadersMsg) obj;
        return Objects.equal(this.count, other.count)
                && Objects.equal(this.blockHeaderMsgList, other.blockHeaderMsgList);
    }

    @Override
    public String toString() {
        return "HeadersMsg(count=" + this.getCount() + ", blockHeaderMsgList=" + this.getBlockHeaderMsgList() + ")";
    }

    public static HeadersMsgBuilder builder() {
        return new HeadersMsgBuilder();
    }

    @Override
    public HeadersMsgBuilder toBuilder() {
        return new HeadersMsgBuilder(super.extraBytes, super.checksum).blockHeaderMsgList(this.blockHeaderMsgList);
    }

    /**
     * Builder
     */
    public static class HeadersMsgBuilder extends BodyMessageBuilder {
        private List<BlockHeaderMsg> blockHeaderMsgList;

        public HeadersMsgBuilder() {}
        public HeadersMsgBuilder(byte[] extraBytes, long checksum) { super(extraBytes, checksum);}

        public HeadersMsg.HeadersMsgBuilder blockHeaderMsgList(List<BlockHeaderMsg> blockHeaderMsgList) {
            this.blockHeaderMsgList = blockHeaderMsgList;
            return this;
        }

        public HeadersMsg build() {
            return new HeadersMsg(blockHeaderMsgList, super.extraBytes, super.checksum);
        }
    }
}
