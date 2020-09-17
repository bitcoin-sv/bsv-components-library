package com.nchain.jcl.base.domain.api.base;

import com.nchain.jcl.base.domain.api.BitcoinObject;
import com.nchain.jcl.base.domain.api.BitcoinHashableObject;
import com.nchain.jcl.base.domain.bean.base.TxBean;

import java.util.List;

/**
 * @author Steve Shadders
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A Transaction (Tx) represents an exchange of coins between 2 or more parties. It specifies the inputs and outputs
 * of the transactions and other related data to the TX, according to the Bitcoin specification.
 */
public interface Tx extends BitcoinHashableObject {

    // TX Data:
    long getVersion();
    List<TxInput> getInputs();
    List<TxOutput> getOutputs();
    long getLockTime();
    boolean isCoinbase();

    // Convenience methods to get a reference to a Builder, so we can build instances of Tx.
    static TxBean.TxBeanBuilder builder() { return TxBean.builder();}
    static TxBean.TxBeanBuilder builder(byte[] bytes) { return TxBean.toBuilder(bytes); }
    static TxBean.TxBeanBuilder builder(String hex) { return TxBean.toBuilder(hex);}
    default TxBean.TxBeanBuilder toBuilder() { return ((TxBean) this).toBuilder();}
}
