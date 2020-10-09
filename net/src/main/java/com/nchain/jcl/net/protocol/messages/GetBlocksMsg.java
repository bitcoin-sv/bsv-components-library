package com.nchain.jcl.net.protocol.messages;

import com.nchain.jcl.net.protocol.messages.common.Message;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;


/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * GetBlocks message return an inv packet containing the list of blocks starting right after the last known hash
 * in the block locator object, up to hash_stop or 500 blocks, whichever comes first.
 *
 */
@Value
@EqualsAndHashCode
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
