package com.nchain.jcl.base.domain.api.base;

import com.nchain.jcl.base.domain.api.BitcoinSerializableObject;
import com.nchain.jcl.base.domain.bean.base.TxOutPointBean;
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper;

/**
 * @author Steve Shadders
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A TxoOutPoint is a sub-structure of a TxOuput. It stores a reference to the "Inut" the output is referring to.
 */
public interface TxOutPoint extends BitcoinSerializableObject {
    Sha256Wrapper getHash();
    long getIndex();

    // Convenience methods to get a reference to a Builder, so we can build instances of TxInput.
    static TxOutPointBean.TxOutPointBeanBuilder builder()       { return TxOutPointBean.builder(); }
    default TxOutPointBean.TxOutPointBeanBuilder toBuilder()    { return ((TxOutPointBean) this).toBuilder();}
}
