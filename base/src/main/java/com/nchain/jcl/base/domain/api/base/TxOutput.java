package com.nchain.jcl.base.domain.api.base;

import com.nchain.jcl.base.core.Coin;
import com.nchain.jcl.base.domain.api.BitcoinObject;
import com.nchain.jcl.base.domain.api.BitcoinSerializableObject;
import com.nchain.jcl.base.domain.bean.base.TxOutputBean;

/**
 * @author Steve Shadders
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An outut of a Bitcoin Transaction
 */
public interface TxOutput extends BitcoinSerializableObject {
    Coin getValue();
    byte[] getScriptBytes();

    // Convenience methods to get a reference to a Builder, so we can build instances of TxInput.
    static TxOutputBean.TxOutputBeanBuilder builder()       { return TxOutputBean.builder(); }
    default TxOutputBean.TxOutputBeanBuilder toBuilder()    { return ((TxOutputBean) this).toBuilder();}
}
