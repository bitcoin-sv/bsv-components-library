package io.bitcoinsv.jcl.net.protocol.messages;


import com.google.common.base.Objects;
import io.bitcoinsv.jcl.net.protocol.messages.common.Message;


/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * The OutPoint structure repesent the previous output transaction reference, and consists of the following fields:
 *
 * -  field: "hash" (32 bytes) char[32]
 *    The hash of the referenced transaction.
 *
 *  - field: "index" (4 bytes) uint32_t
 *    The index of the specific output in the transaction. The first output is 0, etc.
 */
public final class TxOutPointMsg extends Message {

    public static final String MESSAGE_TYPE = "OutPoint";

    private final HashMsg hash;
    private final long index;

    protected TxOutPointMsg(HashMsg hash, long index) {
        this.hash = hash;
        this.index = index;
        init();
    }

    @Override
    protected long calculateLength() {
        long length =  hash.getLengthInBytes() + 4;
        return length;
    }

    @Override
    protected void validateMessage() {}

    @Override
    public String getMessageType()  { return MESSAGE_TYPE; }
    public HashMsg getHash()        { return this.hash; }
    public long getIndex()          { return this.index; }

    @Override
    public String toString() {
        return "hash: " + hash + ", index: " + index;
    }


    @Override
    public int hashCode() {
        return Objects.hashCode(hash, index);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) { return false; }
        if (obj == this) { return true; }
        if (obj.getClass() != getClass()) { return false; }
        TxOutPointMsg other = (TxOutPointMsg) obj;
        return Objects.equal(this.hash, other.hash)
                && Objects.equal(this.index, other.index);
    }

    public static TxOutPointMsgBuilder builder() {
        return new TxOutPointMsgBuilder();
    }

    /**
     * Builder
     */
    public static class TxOutPointMsgBuilder {
        private HashMsg hash;
        private long index;

        TxOutPointMsgBuilder() { }

        public TxOutPointMsg.TxOutPointMsgBuilder hash(HashMsg hash) {
            this.hash = hash;
            return this;
        }

        public TxOutPointMsg.TxOutPointMsgBuilder index(long index) {
            this.index = index;
            return this;
        }

        public TxOutPointMsg build() {
            return new TxOutPointMsg(hash, index);
        }

    }
}
