package com.nchain.jcl.net.protocol.messages;

import com.google.common.base.Preconditions;
import com.nchain.jcl.net.protocol.messages.common.Message;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * notfound is a response to a getdata, sent if any requested data items could not be relayed.
 */
@Value
@EqualsAndHashCode
public class NotFoundMsg extends Message {
    public static final String MESSAGE_TYPE = "notfound";
    private static final long MAX_ADDRESSES = 50000;

    private VarIntMsg count;

    private List<InventoryVectorMsg> invVectorList;

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }

    /**
     * Creates the InvMessage Object.Use the corresponding builder to create the instance.
     *
     * @param count
     * @param invVectorMsgList
     */
    @Builder
    protected NotFoundMsg(VarIntMsg count, List<InventoryVectorMsg> invVectorMsgList) {
        this.count = count;
        this.invVectorList = invVectorMsgList.stream().collect(Collectors.toUnmodifiableList());
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
}
