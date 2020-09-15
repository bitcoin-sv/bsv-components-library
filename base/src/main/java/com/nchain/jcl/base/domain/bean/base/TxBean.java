package com.nchain.jcl.base.domain.bean.base;

import com.nchain.jcl.base.domain.api.base.Tx;
import com.nchain.jcl.base.domain.api.base.TxInput;
import com.nchain.jcl.base.domain.api.base.TxOutput;
import com.nchain.jcl.base.domain.bean.BitcoinHashableImpl;

import com.nchain.jcl.base.serialization.BitcoinSerializerFactory;
import com.nchain.jcl.base.tools.bytes.HEX;
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import static com.google.common.base.Preconditions.checkState;

/**
 * @author Steve Shadders
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An implementation of a Tx.
 * This class is IMMUTABLE. Instances can be created by using a Lombok generated Builder.
 */
@Value
public class TxBean extends BitcoinHashableImpl implements Tx {
    private long version;
    private List<TxInput> inputs;
    private List<TxOutput> outputs;
    private long lockTime;

    @Override
    public boolean isCoinbase() {
        return (inputs.size() == 1) && ((inputs.get(0).getOutpoint().getHash() == null) || (inputs.get(0).getOutpoint().getHash().equals(Sha256Wrapper.ZERO_HASH)));
    }


    /** A Constructor including the HASH from the parent (useful when using the lombok Builder) */
    @Builder(toBuilder = true)
    protected TxBean(Sha256Wrapper hash, long version, List<TxInput> inputs, List<TxOutput> outputs, long lockTime) {
        this.hash = hash;
        this.version = version;
        this.inputs = inputs;
        this.outputs = outputs;
        this.lockTime = lockTime;
    }

    /** Adding a method to create a Builder after Deserializing an object from a source of bytes */
    public static TxBean.TxBeanBuilder toBuilder(byte[] bytes) {
        checkState(BitcoinSerializerFactory.hasFor(Tx.class), "No Serializer for " + Tx.class.getSimpleName());
        return ((TxBean) BitcoinSerializerFactory.getSerializer(Tx.class).deserialize(bytes)).toBuilder();
    }
    /** Adding a method to create a Builder after Deserialzing an object from a HEX */
    public static TxBean.TxBeanBuilder toBuilder(String hex) {
        return ((TxBean) BitcoinSerializerFactory.getSerializer(Tx.class).deserialize(HEX.decode(hex))).toBuilder();
    }
}