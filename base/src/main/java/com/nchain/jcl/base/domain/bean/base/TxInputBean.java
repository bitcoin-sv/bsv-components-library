package com.nchain.jcl.base.domain.bean.base;

import com.nchain.jcl.base.core.Coin;
import com.nchain.jcl.base.domain.api.base.TxInput;
import com.nchain.jcl.base.domain.api.base.TxOutPoint;
import com.nchain.jcl.base.domain.bean.BitcoinObjectImpl;
import com.nchain.jcl.base.domain.bean.BitcoinSerializableObjectImpl;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.SuperBuilder;

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
public class TxInputBean extends BitcoinSerializableObjectImpl implements TxInput {
    private long sequenceNumber;
    private TxOutPoint outpoint;
    private byte[] scriptBytes;
    private Coin value;

    /** Use "TxInput.builder()" instead */
    @Builder(toBuilder = true)
    public TxInputBean(Long sizeInBytes, long sequenceNumber, TxOutPoint outpoint, byte[] scriptBytes, Coin value) {
        super(sizeInBytes);
        this.sequenceNumber = sequenceNumber;
        this.outpoint = outpoint;
        // Defensize copy, to enforce immutability...
        this.scriptBytes = (scriptBytes == null)? null : Arrays.copyOf(scriptBytes, scriptBytes.length);

        this.value = value;
    }

    // Overwritting the default getter, to enforce immutability
    public byte[] getScriptBytes() {
        return (scriptBytes == null) ? null : Arrays.copyOf(scriptBytes, scriptBytes.length);
    }
}
