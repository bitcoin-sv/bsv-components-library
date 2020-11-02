package com.nchain.jcl.base.domain.bean.base;

import com.nchain.jcl.base.domain.api.base.TxOutPoint;
import com.nchain.jcl.base.domain.bean.BitcoinHashableImpl;
import com.nchain.jcl.base.domain.bean.BitcoinObjectImpl;
import com.nchain.jcl.base.domain.bean.BitcoinSerializableObjectImpl;
import com.nchain.jcl.base.tools.crypto.Sha256;
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.SuperBuilder;

/**
 * @author Steve Shadders
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This class is THREAD-SAFE.
 */
// IMPORTANT:
// The annotation @EqualsAndHashCode(callSuper = false) is important. The value of "callSuper" must be SET to FALSE,
// so only the fields included in THIS class are included during the "equals" comparison. If we set this property
// to "true", then the "equals" method will use the values inherited from parent classes, which are NOT relevant in
// this case (these fields in the parent classes already have the @EqualsAndHashCode.Exclude annotation, but it does
// not seem to work as expected).
@Value
@EqualsAndHashCode(callSuper = false)
public class TxOutPointBean extends BitcoinSerializableObjectImpl implements TxOutPoint {
    private Sha256Wrapper hash;
    private long index;

    /** Use "TxOutPoint.builder()" instead */
    @Builder(toBuilder = true)
    public TxOutPointBean(Long sizeInBytes, Sha256Wrapper hash, long index) {
        super(sizeInBytes);
        this.hash = hash;
        this.index = index;
    }
}
