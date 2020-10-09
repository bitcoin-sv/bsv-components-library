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

@Value
@EqualsAndHashCode(callSuper = true)
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
