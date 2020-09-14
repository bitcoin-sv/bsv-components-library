package com.nchain.jcl.net.protocol.messages;

import com.nchain.jcl.base.domain.api.base.TxOutPoint;
import com.nchain.jcl.base.domain.bean.base.TxOutPointBean;
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper;
import com.nchain.jcl.net.protocol.messages.common.Message;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;


/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 *
 * @date 23/09/2019
 *
 * The OutPoint structure repesent the previous output transaction reference, and consists of the following fields:
 *
 * -  field: "hash" (32 bytes) char[32]
 *    The hash of the referenced transaction.
 *
 *  - field: "index" (4 bytes) uint32_t
 *    The index of the specific output in the transaction. The first output is 0, etc.
 */
@Value
@EqualsAndHashCode
public class TxOutPointMsg extends Message {

    public static final String MESSAGE_TYPE = "OutPoint";

    private HashMsg hash;
    private long index;

    @Builder
    protected TxOutPointMsg(HashMsg hash, long index) {
        this.hash = hash;
        this.index = index;
        init();
    }
    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }


    @Override
    protected long calculateLength() {
        long length =  hash.getLengthInBytes() + 4;
        return length;
    }

    @Override
    protected void validateMessage() {}

    @Override
    public String toString() {
        return "hash: " + hash + ", index: " + index;
    }

    /** Returns a BitcoinObject containing the same information */
    public TxOutPoint toBean() {
        return new TxOutPointBean(Sha256Wrapper.wrap(hash.getHashBytes()), index);
    }
}
