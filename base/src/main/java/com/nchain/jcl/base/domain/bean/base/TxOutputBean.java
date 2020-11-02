package com.nchain.jcl.base.domain.bean.base;

import com.nchain.jcl.base.core.Coin;
import com.nchain.jcl.base.domain.api.base.TxOutput;
import com.nchain.jcl.base.domain.bean.BitcoinSerializableObjectImpl;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.Arrays;


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
public class TxOutputBean extends BitcoinSerializableObjectImpl implements TxOutput {
    private Coin value;
    private byte[] scriptBytes;

    /** Use "TxOutput.builder()" instead */
    @Builder(toBuilder = true)
    public TxOutputBean(Long sizeInBytes, Coin value, byte[] scriptBytes) {
        super(sizeInBytes);
        this.value = value;
        this.scriptBytes = (scriptBytes == null) ? null : Arrays.copyOf(scriptBytes, scriptBytes.length);
    }

    // Overwriting the default getter, to enforce immutability
    public byte[] getScriptBytes() {
        return (scriptBytes == null) ? null : Arrays.copyOf(scriptBytes, scriptBytes.length);
    }
}
