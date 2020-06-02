package com.nchain.jcl.protocol.messages;

import com.nchain.jcl.protocol.messages.common.Message;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;


/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 06/09/2019
 *
 * GetBlocks message return an inv packet containing the list of blocks starting right after the last known hash
 * in the block locator object, up to hash_stop or 500 blocks, whichever comes first.
 *
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class GetBlocksMsg extends Message {

    public static final String MESSAGE_TYPE = "getblocks";
    BaseGetDataAndHeaderMsg baseGetDataAndHeaderMsg;

    @Builder
    protected GetBlocksMsg(BaseGetDataAndHeaderMsg baseGetDataAndHeaderMsg) {
        this.baseGetDataAndHeaderMsg = baseGetDataAndHeaderMsg;
        init();
    }

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }

    @Override
    protected long calculateLength() {
        long length = this.baseGetDataAndHeaderMsg.getLengthInBytes();
        return length;
    }

    @Override
    protected void validateMessage() {}

}
