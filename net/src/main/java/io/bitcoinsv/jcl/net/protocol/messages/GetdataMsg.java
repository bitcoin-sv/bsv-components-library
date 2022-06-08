package io.bitcoinsv.jcl.net.protocol.messages;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import io.bitcoinsv.jcl.net.protocol.messages.common.BodyMessage;

import java.io.Serializable;
import java.util.List;

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * getdata is used in response to inv, to retrieve the content of a specific object, and is usually sent after receiving
 * an inv packet, after filtering known elements. It can be used to retrieve transactions, but only if they are
 * in the memory pool or isHandshakeUsingRelay set.
 *
 * Structure of the BODY of Message:
 *
 *  - field: "count" (1+ bytes) 	var_int
 *   Number of Inventory entries (max: 50,000 entries)
 *
 * - field: "inv_vect[]" (36*count) array of inventory vectors
 *   Inventory vectors.
 */
public final class GetdataMsg extends BodyMessage implements Serializable {
    private static final long MAX_ADDRESSES = 50000;
    public static final String MESSAGE_TYPE = "getdata";

    private final VarIntMsg count;

    private final ImmutableList<InventoryVectorMsg> invVectorList;

    /**
     * Creates the InvMessage Object.Use the corresponding byteArray to create the instance.
     */
    protected GetdataMsg(List<InventoryVectorMsg> invVectorList,
                         byte[] extraBytes, long checksum) {
        super(extraBytes, checksum);
        this.invVectorList = ImmutableList.copyOf(invVectorList);
        //count value is calculated from the inv vector list
        long count =  this.invVectorList.size();
        this.count =  VarIntMsg.builder().value(count).build();
        init();
    }

    @Override
    protected long calculateLength() {
        long length  = count.getLengthInBytes();
        for (InventoryVectorMsg invMsg : invVectorList) {
            length += invMsg.getLengthInBytes();
        }
        return length;
    }

    @Override
    protected void validateMessage() {
        Preconditions.checkArgument(count.getValue() <= MAX_ADDRESSES, "GetdataMsg message too largeMsgs.");
    }

    @Override
    public String getMessageType()                              { return MESSAGE_TYPE; }
    public VarIntMsg getCount()                                 { return this.count; }
    public ImmutableList<InventoryVectorMsg> getInvVectorList() { return this.invVectorList; }

    @Override
    public String toString() {
        return "GetdataMsg(count=" + this.getCount() + ", invVectorList=" + this.getInvVectorList() + ")";
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), count, invVectorList);
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) { return false; }
        GetdataMsg other = (GetdataMsg) obj;
        return Objects.equal(this.count, other.count)
                && Objects.equal(this.invVectorList, other.invVectorList);
    }

    public static GetdataMsgBuilder builder() {
        return new GetdataMsgBuilder();
    }

    @Override
    public GetdataMsgBuilder toBuilder() {
        return new GetdataMsgBuilder(super.extraBytes, super.checksum).invVectorList(this.invVectorList);
    }

    /**
     * Builder
     */
    public static class GetdataMsgBuilder extends BodyMessageBuilder {
        private List<InventoryVectorMsg> invVectorList;

        public GetdataMsgBuilder() {}
        public GetdataMsgBuilder(byte[] extraBytes, long checksum) { super(extraBytes, checksum);}

        public GetdataMsg.GetdataMsgBuilder invVectorList(List<InventoryVectorMsg> invVectorList) {
            this.invVectorList = invVectorList;
            return this;
        }

        public GetdataMsg build() {
            return new GetdataMsg(invVectorList, super.extraBytes, super.checksum);
        }
    }
}
