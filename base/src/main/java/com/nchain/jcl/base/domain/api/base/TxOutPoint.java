package com.nchain.jcl.base.domain.api.base;

import com.nchain.jcl.base.domain.api.BitcoinObject;
import com.nchain.jcl.base.domain.bean.base.TxOutPointBean;

/**
 * @author Steve Shadders
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A TxoOutPoint is a sub-structure of a TxOuput. It stores a reference to the "Inut" the output is referring to.
 */
public interface TxOutPoint extends BitcoinObject, HashableObject {
    long getIndex();

    // Convenience methods to get a reference to a Builder, so we can build instances of TxInput.
    static TxOutPointBean.TxOutPointBeanBuilder builder() { return TxOutPointBean.builder(); }
    default TxOutPointBean.TxOutPointBeanBuilder toBuilder() { return ((TxOutPointBean) this).toBuilder();}
}
