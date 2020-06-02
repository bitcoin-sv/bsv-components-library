package com.nchain.jcl.protocol.messages;

import com.google.common.base.Preconditions;
import com.nchain.jcl.protocol.messages.common.Message;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.stream.Collectors;


/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 20/08/2019
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
@Getter
@ToString
@EqualsAndHashCode(callSuper = true)
public class InvMessage extends Message {
    private static final long MAX_ADDRESSES = 50000;
    public static final String MESSAGE_TYPE = "inv";

    private VarIntMsg count;

    private List<InventoryVectorMsg> invVectorList;

    /**
     * Creates the InvMessage Object.Use the corresponding builder to create the instance.
     *
     * @param invVectorMsgList
     */
    @Builder
    protected InvMessage(List<InventoryVectorMsg> invVectorMsgList) {
        this.invVectorList = invVectorMsgList.stream().collect(Collectors.toUnmodifiableList());
        this.count = VarIntMsg.builder().value(invVectorMsgList.size()).build();
        init();
    }

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
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
}
