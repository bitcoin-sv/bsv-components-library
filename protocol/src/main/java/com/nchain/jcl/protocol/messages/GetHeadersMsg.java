package com.nchain.jcl.protocol.messages;

import com.nchain.jcl.protocol.messages.common.Message;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;


/**
 * @author m.jose@nchain.com
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 12/09/2019
 *
 * Return a headers packet containing the headers of blocks starting right after the last known hash in the block
 * locator object, up to hash_stop or 2000 blocks, whichever comes first.
 *
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class GetHeadersMsg extends Message {
    public static final String MESSAGE_TYPE = "getheaders";
    BaseGetDataAndHeaderMsg baseGetDataAndHeaderMsg;

    @Builder
    protected GetHeadersMsg(BaseGetDataAndHeaderMsg baseGetDataAndHeaderMsg) {
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
