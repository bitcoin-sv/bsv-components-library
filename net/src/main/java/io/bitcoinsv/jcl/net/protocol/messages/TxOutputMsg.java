package io.bitcoinsv.jcl.net.protocol.messages;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.Tx;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.TxOutPoint;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.TxOutput;
import io.bitcoinsv.bitcoinjsv.bitcoin.bean.base.TxOutPointBean;
import io.bitcoinsv.bitcoinjsv.bitcoin.bean.base.TxOutputBean;
import io.bitcoinsv.bitcoinjsv.core.Coin;
import io.bitcoinsv.jcl.net.protocol.messages.common.Message;

import java.io.Serializable;
import java.util.Arrays;

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Transaction Output represent  outputs or destinations for coins and consists of the following fields:
 *
 * -  field: "value" (8 bytes)
 *    Transaction Value.
 *
 * - field: "pk_script length"  VarInt
 *    The length of the pk script.
 *
 * - field: "pk_script"  uchar[]
 *   Usually contains the public key as a Bitcoin script setting up conditions to claim this output.
 */
public final class TxOutputMsg extends Message implements Serializable {
    public static final String MESSAGE_TYPE = "TxOut";
    private static final int txValue_length = 8;

    private final long txValue;
    private final VarIntMsg pk_script_length;
    private final byte[] pk_script;

    protected TxOutputMsg(long txValue, byte[] pk_script) {
        this.txValue = txValue;
        this.pk_script = pk_script;
        this.pk_script_length = VarIntMsg.builder().value(pk_script.length).build();
        init();
    }

    @Override
    protected long calculateLength() {
        long length = txValue_length + pk_script_length.getLengthInBytes() + pk_script.length;;
        return length;
    }

    @Override
    protected void validateMessage() {
        Preconditions.checkArgument(pk_script.length  ==  pk_script_length.getValue(), "Script lengths are not same.");
    }

    @Override
    public String toString() {
        return "value: " + txValue + ", scriptLength: " + pk_script_length;
    }

    @Override
    public String getMessageType()          { return MESSAGE_TYPE; }
    public long getTxValue()                { return this.txValue; }
    public VarIntMsg getPk_script_length()  { return this.pk_script_length; }
    public byte[] getPk_script()            { return this.pk_script; }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), txValue, pk_script_length, Arrays.hashCode(pk_script));
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) { return false; }
        TxOutputMsg other = (TxOutputMsg) obj;
        return Objects.equal(this.txValue, other.txValue)
                && Objects.equal(this.pk_script_length, other.pk_script_length)
                && Arrays.equals(this.pk_script, other.pk_script);
    }


    public static TxOutputMsgBuilder builder() {
        return new TxOutputMsgBuilder();
    }

    public TxOutputMsgBuilder toBuilder() {
        return new TxOutputMsgBuilder()
                        .txValue(this.txValue)
                        .pk_script(this.pk_script);
    }

    /** Returns a Bean class */
    public TxOutput toBean() {
        TxOutputBean result = new TxOutputBean((Tx) null);
        result.setValue(Coin.valueOf(this.txValue));
        result.setScriptBytes(this.pk_script);
        return result;
    }

    /** Returns a Msg class out of a Bean */
    public static TxOutputMsg fromBean(TxOutput bean) {
        TxOutputMsg result = TxOutputMsg.builder()
                .pk_script(bean.getScriptBytes())
                .txValue(bean.getValue().value)
                .build();
        return result;
    }

    /**
     * Builder
     */
    public static class TxOutputMsgBuilder {
        private long txValue;
        private byte[] pk_script;

        public TxOutputMsgBuilder() {}

        public TxOutputMsg.TxOutputMsgBuilder txValue(long txValue) {
            this.txValue = txValue;
            return this;
        }

        public TxOutputMsg.TxOutputMsgBuilder pk_script(byte[] pk_script) {
            this.pk_script = pk_script;
            return this;
        }

        public TxOutputMsg build() {
            return new TxOutputMsg(txValue, pk_script);
        }
    }
}
