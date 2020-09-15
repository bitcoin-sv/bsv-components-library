package com.nchain.jcl.net.protocol.messages;

import com.google.common.collect.ImmutableList;
import com.nchain.jcl.base.domain.api.base.Tx;
import com.nchain.jcl.base.domain.bean.base.TxBean;
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper;
import com.nchain.jcl.net.protocol.messages.common.Message;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 *
 * @date 02/10/2019
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
@Data
@EqualsAndHashCode
public class TxMsg extends Message {

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

    @Builder
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
    public String getMessageType() { return MESSAGE_TYPE; }

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

    /** Returns a BitcoinObject containing the same information */
    public Tx toBean() {
        TxBean.TxBeanBuilder txBuilder = Tx.builder()
                .lockTime(lockTime)
                .version(version)
                .inputs(tx_in.stream().map(i -> i.toBean()).collect(Collectors.toList()))
                .outputs(tx_out.stream().map(o -> o.toBean()).collect(Collectors.toList()));
        if (hash.isPresent())
            txBuilder.hash(Sha256Wrapper.wrap(hash.get().getHashBytes()));
        return txBuilder.build();
    }
}