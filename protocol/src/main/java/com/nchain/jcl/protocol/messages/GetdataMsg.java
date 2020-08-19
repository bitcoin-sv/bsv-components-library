package com.nchain.jcl.protocol.messages;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.nchain.jcl.protocol.messages.common.Message;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.List;

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 23/08/2019
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
@Value
@EqualsAndHashCode
public class GetdataMsg extends Message {
    private static final long MAX_ADDRESSES = 50000;
    public static final String MESSAGE_TYPE = "getdata";

    private VarIntMsg count;

    private ImmutableList<InventoryVectorMsg> invVectorList;

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }

    /**
     * Creates the InvMessage Object.Use the corresponding builder to create the instance.
     *
     * @param invVectorList
     */
    @Builder
    protected GetdataMsg(List<InventoryVectorMsg> invVectorList) {
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
}
