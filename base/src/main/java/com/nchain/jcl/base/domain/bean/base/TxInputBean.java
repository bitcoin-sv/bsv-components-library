package com.nchain.jcl.base.domain.bean.base;

import com.nchain.jcl.base.core.Coin;
import com.nchain.jcl.base.domain.api.base.TxInput;
import com.nchain.jcl.base.domain.api.base.TxOutPoint;
import com.nchain.jcl.base.domain.bean.BitcoinObjectImpl;
import com.nchain.jcl.base.domain.bean.BitcoinSerializableObjectImpl;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.SuperBuilder;

/**
 * @author Steve Shadders
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An implementation of a Tx.
 * This class is IMMUTABLE. Instances can be created by using a Lombok generated Builder.
 */

@Value
public class TxInputBean extends BitcoinSerializableObjectImpl implements TxInput {
    private long sequenceNumber;
    private TxOutPoint outpoint;
    private byte[] scriptBytes;
    private Coin value;

    @Builder(toBuilder = true)
    public TxInputBean(Long sizeInBytes, long sequenceNumber, TxOutPoint outpoint, byte[] scriptBytes, Coin value) {
        super(sizeInBytes);
        this.sequenceNumber = sequenceNumber;
        this.outpoint = outpoint;
        this.scriptBytes = scriptBytes;
        this.value = value;
    }
}
