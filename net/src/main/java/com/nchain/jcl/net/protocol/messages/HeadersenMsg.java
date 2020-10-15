package com.nchain.jcl.net.protocol.messages;

import com.google.common.base.Preconditions;
import com.nchain.jcl.net.protocol.messages.common.Message;

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
public class HeadersenMsg extends Message {
    private static final long MAX_ADDRESSES = 2000;
    public static final String MESSAGE_TYPE = "headersen";

    private VarIntMsg count;

    private List<BlockHeaderEnrichedMsg> blockHeaderEnMsgList;

    /**
     * Creates the HeadersenMsg Object. Use the corresponding builder to create the instance.
     *
     * @param blockHeaderEnMsgList
     */
    public HeadersenMsg(List<BlockHeaderEnrichedMsg> blockHeaderEnMsgList) {
        this.blockHeaderEnMsgList = blockHeaderEnMsgList.stream().collect(Collectors.toUnmodifiableList());
        this.count = VarIntMsg.builder().value(blockHeaderEnMsgList.size()).build();
        init();
    }

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }

    @Override
    protected long calculateLength() {
        long length = count.getLengthInBytes() ;

        for (BlockHeaderEnrichedMsg blockHeaderEnrichedMsg:blockHeaderEnMsgList) {
            length += blockHeaderEnrichedMsg.calculateLength();
        }
        return length;
    }

    @Override
    protected void validateMessage() {
        Preconditions.checkArgument(count.getValue() <= MAX_ADDRESSES,"Headers message exceeds maximum size");
        Preconditions.checkArgument(count.getValue() ==  blockHeaderEnMsgList.size(), "Headers list size and count value are not the same.");
    }
}
