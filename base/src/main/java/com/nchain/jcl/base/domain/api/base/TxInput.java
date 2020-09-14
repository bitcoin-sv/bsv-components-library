package com.nchain.jcl.base.domain.api.base;

import com.nchain.jcl.base.core.Coin;
import com.nchain.jcl.base.domain.api.BitcoinObject;
import com.nchain.jcl.base.domain.bean.base.TxInputBean;

import javax.annotation.Nullable;

/**
 * @author Steve Shadders
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Input of a Bitcoin Transaction
 */
public interface TxInput extends BitcoinObject {
    static long NO_SEQUENCE = 0xFFFFFFFFL;

    // Tx Input Data:
    long getSequenceNumber();
    TxOutPoint getOutpoint();
    byte[] getScriptBytes();
    default boolean hasSequence() {
        return getSequenceNumber() != NO_SEQUENCE;
    }

    @Nullable
    Coin getValue();

    // Convenience methods to get a reference to a Builder, so we can build instances of TxInput.
    static TxInputBean.TxInputBeanBuilder builder() { return TxInputBean.builder(); }
    default TxInputBean.TxInputBeanBuilder toBuilder() { return ((TxInputBean) this).toBuilder();}
}
