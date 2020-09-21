package com.nchain.jcl.net.protocol.messages;

import com.google.common.base.Preconditions;
import com.nchain.jcl.base.core.Coin;
import com.nchain.jcl.base.domain.api.base.TxOutput;
import com.nchain.jcl.base.domain.bean.base.TxOutputBean;
import com.nchain.jcl.net.protocol.messages.common.Message;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 *
 * @date 01/10/2019
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
@Value
@EqualsAndHashCode
public class TxOutputMsg extends Message {
    public static final String MESSAGE_TYPE = "TxOut";
    private static final int txValue_length = 8;

    private long txValue;
    private VarIntMsg pk_script_length;
    private byte[] pk_script;

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }

    @Builder
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

    /** Returns a BitcoinObject containing the same information */
    public TxOutput toBean() {
        return new TxOutputBean(lengthInBytes, Coin.valueOf(txValue), pk_script);
    }
}
