package com.nchain.jcl.net.protocol.messages;

import com.nchain.jcl.net.protocol.messages.common.Message;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-18 12:39
 *
 */
@Value
@EqualsAndHashCode
public class PartialBlockHeaderMsg extends Message {
    public static final String MESSAGE_TYPE = "PartialBlockHeader";
    private BlockHeaderMsg blockHeader;

    @Builder
    public PartialBlockHeaderMsg(BlockHeaderMsg blockHeader) {
        this.blockHeader = blockHeader;
        init();
    }

    @Override
    protected long calculateLength() { return blockHeader.calculateLength(); }

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }

    @Override
    protected void validateMessage() {}

}
