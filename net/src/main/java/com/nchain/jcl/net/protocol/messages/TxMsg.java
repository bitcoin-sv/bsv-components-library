package com.nchain.jcl.net.protocol.messages;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.nchain.jcl.net.protocol.messages.common.Message;
import io.bitcoinj.bitcoin.api.base.*;
import io.bitcoinj.bitcoin.bean.base.TxBean;
import io.bitcoinj.bitcoin.bean.base.TxInputBean;
import io.bitcoinj.bitcoin.bean.base.TxOutPointBean;
import io.bitcoinj.bitcoin.bean.base.TxOutputBean;
import io.bitcoinj.core.Coin;
import io.bitcoinj.core.Sha256Hash;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * tx describes a bitcoin transaction, in reply to getdata.
 *
 * Structure of the BODY of Message:
 *
 *  - field: "version" (4 bytes) unit32_t
 *    Transaction data format version (note, this is signed)
 *
 * - field: "tx_in count" (1+ bytes) var_int
 *   Number of Transaction inputs (never zero)
 *
 * - field: "tx_in  (41+ bytes) TransactionInput
 *   A list of 1 or more transaction inputs or sources for coins
 *
 *  - field: "tx_out count" (1+ bytes) Transaction Output
 *   Number of Transaction outputs
 *
 *  - field: "tx_out" (4 bytes) var_int
 *  The block number or timestamp at which this transaction is unlocked:
 */
public class TxMsg extends Message implements Serializable {

    public static final String MESSAGE_TYPE = "tx";


    // TX HASH:
    // This field is NOT part of the specification of a Bitcoin Transaction Message, so its not either
    // Serialized or Deserialized. But it's ver conveniente to have it here, since this field is the most
    // commons thing to identify a TX.
    // The calculation of this Field is made during the Serialization/Deserialization. Ïn those cases where
    // performance is very important, the Hash calculation might be disabled, that0's why we are using an Optional.

    private final Optional<HashMsg> hash;

    private long version;
    private VarIntMsg tx_in_count;
    private List<TxInputMsg> tx_in;
    private VarIntMsg tx_out_count;
    private List<TxOutputMsg> tx_out;
    private long lockTime;

    protected TxMsg(Optional<HashMsg> hash, long version, List<TxInputMsg> tx_in, List<TxOutputMsg> tx_out, long lockTime) {
        this.hash = hash;
        this.version = version;
        this.tx_in = ImmutableList.copyOf(tx_in);
        this.tx_in_count = VarIntMsg.builder().value(tx_in.size()).build();
        this.tx_out = ImmutableList.copyOf(tx_out);;
        this.tx_out_count =VarIntMsg.builder().value(tx_out.size()).build();
        this.lockTime = lockTime;
        init();
    }

    public TxMsg(Optional<HashMsg> hash) {
        this.hash = hash;
    }




    @Override
    protected long calculateLength() {
        long length = 4 + this.tx_in_count.getLengthInBytes() +  this.tx_out_count.getLengthInBytes()+ 4 ;
        for(TxInputMsg txIn : this.tx_in)
            length += txIn.getLengthInBytes();

        for(TxOutputMsg tx_out_count : this.tx_out)
            length += tx_out_count.getLengthInBytes();
        return length;
    }

    @Override
    protected void validateMessage() {}

    @Override
    public String getMessageType()          { return MESSAGE_TYPE; }
    public Optional<HashMsg> getHash()      { return this.hash; }
    public long getVersion()                { return this.version; }
    public VarIntMsg getTx_in_count()       { return this.tx_in_count; }
    public List<TxInputMsg> getTx_in()      { return this.tx_in; }
    public VarIntMsg getTx_out_count()      { return this.tx_out_count; }
    public List<TxOutputMsg> getTx_out()    { return this.tx_out; }
    public long getLockTime()               { return this.lockTime; }
    public void setVersion(long version)    { this.version = version; }

    @Override
    public int hashCode() {
        return Objects.hashCode(hash, version, tx_in_count, tx_in, tx_out_count, tx_out, lockTime, version);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) { return false; }
        if (obj == this) { return true; }
        if (obj.getClass() != getClass()) { return false; }
        TxMsg other = (TxMsg) obj;
        return Objects.equal(this.hash, other.hash)
                && Objects.equal(this.version, other.version)
                && Objects.equal(this.tx_in_count, other.tx_in_count)
                && Objects.equal(this.tx_in, other.tx_in)
                && Objects.equal(this.tx_out_count, other.tx_out_count)
                && Objects.equal(this.tx_out, other.tx_out)
                && Objects.equal(this.lockTime, other.lockTime)
                && Objects.equal(this.version, other.version);
    }

    @Override
    public String toString() {
        StringBuffer result = new StringBuffer();
        result.append("Transaction: \n");
        result.append(" - hash: " + (hash.isPresent()? hash.get().toString() : " (not calculated)"));
        result.append(" - version: " + version + "\n");
        result.append(" - locktime: " + lockTime + "\n");
        result.append(" - num inputs: " + tx_in_count);
        result.append(" - num Outputs: " + tx_out_count);
        result.append(" - Inputs: \n");
        for (int i = 0; i < tx_in_count.getValue(); i++) {
            result.append("Input " + i + "\n");
            result.append(tx_in.get(i) + "\n");
        }
        result.append(" - Outputs: \n");
        for (int i = 0; i < tx_out_count.getValue(); i++) {
            result.append("Output " + i + "\n");
            result.append(tx_out.get(i) + "\n");
        }
        return result.toString();
    }

    /** Returns a Domain Class */
    public Tx toBean() {
        Tx result = new TxBean((AbstractBlock) null);

        result.setVersion(this.version);
        result.setLockTime(this.lockTime);
        List<TxInput> inputs = new ArrayList<>();

        for (TxInputMsg txInputMsg : tx_in) {
            TxInput txInput = new TxInputBean(result);
            txInput.setSequenceNumber(txInputMsg.getSequence());
            txInput.setScriptBytes(txInputMsg.getSignature_script());
            TxOutPoint outpoint = new TxOutPointBean(txInput);
            outpoint.setIndex(txInputMsg.getPre_outpoint().getIndex());
            outpoint.setHash(Sha256Hash.wrapReversed(txInputMsg.getPre_outpoint().getHash().getHashBytes()));
            txInput.setOutpoint(outpoint);;
            inputs.add(txInput);
        }

        result.setInputs(inputs);

        List<TxOutput> outputs = new ArrayList<>();

        for (TxOutputMsg txOutputMsg: tx_out) {
            TxOutput txOutput = new TxOutputBean(result);
            txOutput.setScriptBytes(txOutputMsg.getPk_script());
            txOutput.setValue(Coin.valueOf(txOutputMsg.getTxValue()));
            outputs.add(txOutput);
        }

        result.setOutputs(outputs);
        result.makeImmutable();
        return result;
    }

    public static TxMsgBuilder builder() {
        return new TxMsgBuilder();
    }

    /**
     * Builder
     */
    public static class TxMsgBuilder {
        private Optional<HashMsg> hash;
        private long version;
        private List<TxInputMsg> tx_in;
        private List<TxOutputMsg> tx_out;
        private long lockTime;

        TxMsgBuilder() {
        }

        public TxMsg.TxMsgBuilder hash(Optional<HashMsg> hash) {
            this.hash = hash;
            return this;
        }

        public TxMsg.TxMsgBuilder version(long version) {
            this.version = version;
            return this;
        }

        public TxMsg.TxMsgBuilder tx_in(List<TxInputMsg> tx_in) {
            this.tx_in = tx_in;
            return this;
        }

        public TxMsg.TxMsgBuilder tx_out(List<TxOutputMsg> tx_out) {
            this.tx_out = tx_out;
            return this;
        }

        public TxMsg.TxMsgBuilder lockTime(long lockTime) {
            this.lockTime = lockTime;
            return this;
        }

        public TxMsg build() {
            return new TxMsg(hash, version, tx_in, tx_out, lockTime);
        }

        public String toString() {
            return "TxMsg.TxMsgBuilder(hash=" + this.hash + ", version=" + this.version + ", tx_in=" + this.tx_in + ", tx_out=" + this.tx_out + ", lockTime=" + this.lockTime + ")";
        }
    }
}
