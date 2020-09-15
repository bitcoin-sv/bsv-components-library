package com.nchain.jcl.base.domain.bean.base;

import com.nchain.jcl.base.core.Coin;
import com.nchain.jcl.base.domain.api.base.TxOutput;
import com.nchain.jcl.base.domain.bean.BitcoinObjectImpl;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

/**
 * @author Steve Shadders
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Implementation of a TxOutput
 * This class is IMMUTABLE. Instances can be created by using a Lombok generated Builder.
 */
@Builder(toBuilder = true)
@AllArgsConstructor
@Value
public class TxOutputBean extends BitcoinObjectImpl implements TxOutput {
    private Coin value;
    private byte[] scriptBytes;
}