/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.protocol.messages;

import com.google.common.base.Objects;
import io.bitcoinsv.jcl.net.protocol.messages.common.Message;


/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Return a headers packet containing the headers of blocks starting right after the last known hash in the block
 * locator object, up to hash_stop or 2000 blocks, whichever comes first.
 *
 */
public final class GetHeadersMsg extends Message {
    public static final String MESSAGE_TYPE = "getheaders";
    private final BaseGetDataAndHeaderMsg baseGetDataAndHeaderMsg;

    protected GetHeadersMsg(BaseGetDataAndHeaderMsg baseGetDataAndHeaderMsg) {
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

    public String toString() {
        return "GetHeadersMsg(baseGetDataAndHeaderMsg=" + this.getBaseGetDataAndHeaderMsg() + ")";
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
        GetHeadersMsg other = (GetHeadersMsg) obj;
        return Objects.equal(this.baseGetDataAndHeaderMsg, other.baseGetDataAndHeaderMsg);
    }


    public static GetHeadersMsgBuilder builder() {
        return new GetHeadersMsgBuilder();
    }

    /**
     * Builder
     */
    public static class GetHeadersMsgBuilder {
        private BaseGetDataAndHeaderMsg baseGetDataAndHeaderMsg;

        GetHeadersMsgBuilder() {}

        public GetHeadersMsg.GetHeadersMsgBuilder baseGetDataAndHeaderMsg(BaseGetDataAndHeaderMsg baseGetDataAndHeaderMsg) {
            this.baseGetDataAndHeaderMsg = baseGetDataAndHeaderMsg;
            return this;
        }

        public GetHeadersMsg build() {
            return new GetHeadersMsg(baseGetDataAndHeaderMsg);
        }
    }
}
