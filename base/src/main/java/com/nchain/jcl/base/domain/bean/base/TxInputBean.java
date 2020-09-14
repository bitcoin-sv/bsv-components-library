package com.nchain.jcl.base.domain.bean.base;

import com.nchain.jcl.base.core.Coin;
import com.nchain.jcl.base.domain.api.base.TxInput;
import com.nchain.jcl.base.domain.api.base.TxOutPoint;
import com.nchain.jcl.base.domain.bean.BitcoinObjectImpl;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

/**
 * @author Steve Shadders
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An implementation of a Tx.
 * This class is IMMUTABLE. Instances can be created by using a Lombok generated Builder.
 */
@Builder(toBuilder = true)
@AllArgsConstructor
@Value
public class TxInputBean extends BitcoinObjectImpl implements TxInput {
    private long sequenceNumber;
    private TxOutPoint outpoint;
    private byte[] scriptBytes;
    private Coin value;
}
