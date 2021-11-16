package com.nchain.jcl.net.protocol.messages;

import com.google.common.base.Objects;
import com.nchain.jcl.net.protocol.messages.common.Message;

import java.io.Serializable;


/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * GetBlocks message return an inv packet containing the list of blocks starting right after the last known hash
 * in the block locator object, up to hash_stop or 500 blocks, whichever comes first.
 *
 */
public final class GetBlocksMsg extends Message implements Serializable {

    public static final String MESSAGE_TYPE = "getblocks";
    private final BaseGetDataAndHeaderMsg baseGetDataAndHeaderMsg;

    protected GetBlocksMsg(BaseGetDataAndHeaderMsg baseGetDataAndHeaderMsg) {
        this.baseGetDataAndHeaderMsg = baseGetDataAndHeaderMsg;
        init();
    }

    @Override
    protected long calculateLength() {
        long length = this.baseGetDataAndHeaderMsg.getLengthInBytes();
        return length;
    }

    @Override
    protected void validateMessage() {}

    @Override
    public String getMessageType()                              { return MESSAGE_TYPE; }
    public BaseGetDataAndHeaderMsg getBaseGetDataAndHeaderMsg() { return this.baseGetDataAndHeaderMsg; }

    @Override
    public String toString() {
        return "GetBlocksMsg(baseGetDataAndHeaderMsg=" + this.getBaseGetDataAndHeaderMsg() + ")";
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(baseGetDataAndHeaderMsg);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) { return false; }
        if (obj == this) { return true; }
        if (obj.getClass() != getClass()) { return false; }
        GetBlocksMsg other = (GetBlocksMsg) obj;
        return Objects.equal(this.baseGetDataAndHeaderMsg, other.baseGetDataAndHeaderMsg);
    }

    public static GetBlocksMsgBuilder builder() {
        return new GetBlocksMsgBuilder();
    }

    @Override
    public GetBlocksMsgBuilder toBuilder() {
        return new GetBlocksMsgBuilder()
                    .baseGetDataAndHeaderMsg(this.baseGetDataAndHeaderMsg);
    }

    /**
     * Builder
     */
    public static class GetBlocksMsgBuilder extends MessageBuilder{
        private BaseGetDataAndHeaderMsg baseGetDataAndHeaderMsg;

        GetBlocksMsgBuilder() {}

        public GetBlocksMsg.GetBlocksMsgBuilder baseGetDataAndHeaderMsg(BaseGetDataAndHeaderMsg baseGetDataAndHeaderMsg) {
            this.baseGetDataAndHeaderMsg = baseGetDataAndHeaderMsg;
            return this;
        }

        public GetBlocksMsg build() {
            return new GetBlocksMsg(baseGetDataAndHeaderMsg);
        }
    }
}
