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
 * Allows a node to advertise its knowledge of one or more objects. It can be received unsolicited, or in reply to getblocks.
 *
 * Structure of the BODY of Message:
 *
 *  - field: "count" (1+ bytes) 	var_int
 *   Number of Inventory entries (max: 50,000 entries)
 *
 * - field: "inv_vect[]" (36*count) array of inventory vectors
 *   Inventory vectors.
 *
 */
public class InvMessage extends Message implements Serializable {
    private static final long MAX_ADDRESSES = 50000;
    public static final String MESSAGE_TYPE = "inv";

    private VarIntMsg count;

    private List<InventoryVectorMsg> invVectorList;

    /**
     * Creates the InvMessage Object.Use the corresponding byteArray to create the instance.
     */
    protected InvMessage(List<InventoryVectorMsg> invVectorMsgList) {
        this.invVectorList = invVectorMsgList.stream().collect(Collectors.toUnmodifiableList());
        this.count = VarIntMsg.builder().value(invVectorMsgList.size()).build();
        init();
    }

    @Override
    protected long calculateLength() {
        long length = count.getLengthInBytes() + count.getValue() * (InventoryVectorMsg.VECTOR_TYPE_LENGTH + HashMsg.HASH_LENGTH);;
        return length;
    }

    @Override
    protected void validateMessage() {
        Preconditions.checkArgument(count.getValue() <= MAX_ADDRESSES,"Inv message too largeMsgs.");
        Preconditions.checkArgument(count.getValue() ==  invVectorList.size(), "InvMessage list and count value are not same.");
    }

    @Override
    public String getMessageType()                      { return MESSAGE_TYPE; }
    public VarIntMsg getCount()                         { return this.count; }
    public List<InventoryVectorMsg> getInvVectorList()  { return this.invVectorList; }

    @Override
    public int hashCode() {
        return Objects.hashCode(count, invVectorList);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) { return false; }
        if (obj == this) { return true; }
        if (obj.getClass() != getClass()) { return false; }
        InvMessage other = (InvMessage) obj;
        return Objects.equal(this.count, other.count)
                && Objects.equal(this.invVectorList, other.invVectorList);
    }

    @Override
    public String toString() {
        return "InvMessage(count=" + this.getCount() + ", invVectorList=" + this.getInvVectorList() + ")";
    }

    public static InvMessageBuilder builder() {
        return new InvMessageBuilder();
    }

    @Override
    public InvMessageBuilder toBuilder() {
        return new InvMessageBuilder().invVectorMsgList(this.invVectorList);
    }

    /**
     * Builder
     */
    public static class InvMessageBuilder extends MessageBuilder{
        private List<InventoryVectorMsg> invVectorMsgList;

        InvMessageBuilder() {}

        public InvMessage.InvMessageBuilder invVectorMsgList(List<InventoryVectorMsg> invVectorMsgList) {
            this.invVectorMsgList = invVectorMsgList;
            return this;
        }

        public InvMessage build() {
            return new InvMessage(invVectorMsgList);
        }

    }
}
