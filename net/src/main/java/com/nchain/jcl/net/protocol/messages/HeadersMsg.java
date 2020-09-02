package com.nchain.jcl.net.protocol.messages;

import com.google.common.base.Preconditions;
import com.nchain.jcl.net.protocol.messages.common.Message;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.stream.Collectors;


/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 20/08/2019
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
@Value
public class HeadersMsg extends Message {
    private static final long MAX_ADDRESSES = 2000;
    public static final String MESSAGE_TYPE = "headers";

    private VarIntMsg count;

    private List<BlockHeaderMsg> blockHeaderMsgList;

    /**
     * Creates the HeadersMsg Object. Use the corresponding builder to create the instance.
     *
     * @param blockHeaderMsgList
     */
    @Builder
    protected HeadersMsg(List<BlockHeaderMsg> blockHeaderMsgList) {
        this.blockHeaderMsgList = blockHeaderMsgList.stream().collect(Collectors.toUnmodifiableList());
        this.count = VarIntMsg.builder().value(blockHeaderMsgList.size()).build();
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
        Preconditions.checkArgument(count.getValue() <= MAX_ADDRESSES,"Headers message exceeds maximum size");
        Preconditions.checkArgument(count.getValue() ==  blockHeaderMsgList.size(), "Headers list size and count value are not the same.");
    }
}
