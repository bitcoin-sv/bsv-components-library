package com.nchain.jcl.base.domain.bean.base;

import com.nchain.jcl.base.domain.api.base.Tx;
import com.nchain.jcl.base.domain.api.base.TxInput;
import com.nchain.jcl.base.domain.api.base.TxOutput;
import com.nchain.jcl.base.domain.bean.BitcoinHashableImpl;

import com.nchain.jcl.base.serialization.BitcoinSerializerFactory;
import com.nchain.jcl.base.tools.bytes.HEX;
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.Collections;
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
// IMPORTANT:
// The annotation @EqualsAndHashCode(callSuper = false) is important. The value of "callSuper" must be SET to FALSE,
// so only the fields included in THIS class are included during the "equals" comparison. If we set this property
// to "true", then the "equals" method will use the values inherited from parent classes, which are NOT relevant in
// this case (these fields in the parent classes already have the @EqualsAndHashCode.Exclude annotation, but it does
// not seem to work as expected).
@Value
@EqualsAndHashCode(callSuper = false)
public class TxBean extends BitcoinHashableImpl implements Tx {
    private long version;
    private List<TxInput> inputs;
    private List<TxOutput> outputs;
    private long lockTime;

    @Builder(toBuilder = true)
    public TxBean(Long sizeInBytes, Sha256Wrapper hash, long version, List<TxInput> inputs, List<TxOutput> outputs,
                  long lockTime) {
            super(sizeInBytes, hash);
            this.version = version;
            // Defensive copy, to enforce immutability:
            this.inputs = (inputs == null)? null :  Collections.unmodifiableList(new ArrayList<>(inputs));
            this.outputs = (outputs == null)? null : Collections.unmodifiableList(new ArrayList<>(outputs));
            this.lockTime = lockTime;
    }

    @Override
    public boolean isCoinbase() {
        return (inputs.size() == 1) && ((inputs.get(0).getOutpoint().getHash() == null) || (inputs.get(0).getOutpoint().getHash().equals(Sha256Wrapper.ZERO_HASH)));
    }

    /** Adding a method to create a Builder after Deserializing an object from a source of bytes */
    public static TxBean.TxBeanBuilder toBuilder(byte[] bytes) {
        checkState(BitcoinSerializerFactory.hasFor(Tx.class), "No Serializer for " + Tx.class.getSimpleName());
        return ((TxBean) BitcoinSerializerFactory.getSerializer(Tx.class).deserialize(bytes)).toBuilder();
    }
    /** Adding a method to create a Builder after Deserialzing an object from a HEX */
    public static TxBean.TxBeanBuilder toBuilder(String hex) {
        return toBuilder(HEX.decode(hex));
    }
}
