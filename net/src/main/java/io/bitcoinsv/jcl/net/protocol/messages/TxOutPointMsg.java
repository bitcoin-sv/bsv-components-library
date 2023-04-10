package io.bitcoinsv.jcl.net.protocol.messages;


import com.google.common.base.Objects;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.Tx;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.TxInput;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.TxOutPoint;
import io.bitcoinsv.bitcoinjsv.bitcoin.bean.base.TxOutPointBean;
import io.bitcoinsv.bitcoinjsv.bitcoin.bean.base.TxOutputBean;
import io.bitcoinsv.bitcoinjsv.core.Sha256Hash;
import io.bitcoinsv.jcl.net.protocol.messages.common.Message;

import java.io.Serializable;


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
public final class TxOutPointMsg extends Message implements Serializable {

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
        return Objects.hashCode(super.hashCode(), hash, index);
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) { return false; }
        TxOutPointMsg other = (TxOutPointMsg) obj;
        return Objects.equal(this.hash, other.hash)
                && Objects.equal(this.index, other.index);
    }

    public static TxOutPointMsgBuilder builder() {
        return new TxOutPointMsgBuilder();
    }

    public TxOutPointMsgBuilder toBuilder() {
        return new TxOutPointMsgBuilder()
                        .hash(this.hash)
                        .index(this.index);
    }

    /** Returns a Domain Class */
    public TxOutPointBean toBean() {
        TxOutPointBean result = new TxOutPointBean((TxInput) null);
        result.setHash(Sha256Hash.wrapReversed(this.hash.getHashBytes()));
        result.setIndex(this.index);
        return result;
    }

    /** Returns a Msg object out of a Bean */
    public static TxOutPointMsg fromBean(TxOutPoint bean) {
        return TxOutPointMsg.builder()
                .hash(HashMsg.builder().hash(bean.getHash().getReversedBytes()).build())
                .index(bean.getIndex())
                .build();
    }

    /**
     * Builder
     */
    public static class TxOutPointMsgBuilder{
        private HashMsg hash;
        private long index;

        public TxOutPointMsgBuilder() { }

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
