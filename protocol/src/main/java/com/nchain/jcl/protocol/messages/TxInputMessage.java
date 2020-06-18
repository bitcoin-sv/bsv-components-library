package com.nchain.jcl.protocol.messages;

import com.google.common.base.Preconditions;
import com.nchain.jcl.protocol.messages.common.Message;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 *
 * @date 26/09/2019
 *
 * The TxInputMessage structure represent  transaction inputs , and consists of the following fields:
 *
 * -  field: "previous_output" (36 bytes) outpoint
 *    The previous output transaction reference, as an OutPoint structure.
 *
 *  - field: "script length"  VarInt
 *    The length of the signature script.
 *
 *   - field: "signature script"  uchar[]
 *    Computational Script for confirming transaction authorization.
 *
 *   - field: "sequence"  (4 bytes) int
 *   Transaction version as defined by the sender.
 */
@Value
@EqualsAndHashCode
public class TxInputMessage extends Message {
    private static final int sequence_len = 4;
    public static final String MESSAGE_TYPE = "TxIn";

    private TxOutPointMsg pre_outpoint;
    private VarIntMsg script_length;
    private byte[] signature_script;
    private long sequence;

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }

    @Builder
    protected TxInputMessage(TxOutPointMsg pre_outpoint, byte[] signature_script, long sequence) {
        this.pre_outpoint = pre_outpoint;
        this.signature_script = signature_script;
        this.script_length = VarIntMsg.builder().value(signature_script.length).build();
        this.sequence = sequence;
        init();
    }


    @Override
    protected long calculateLength() {
        long length = pre_outpoint.getLengthInBytes() + script_length.getLengthInBytes() + signature_script.length + sequence_len;
        return length;
    }

    @Override
    protected void validateMessage() {
        Preconditions.checkArgument(signature_script.length  ==  script_length.getValue(), "Signature script length and script length  are not same.");
    }

    @Override
    public String toString() {
        StringBuffer result = new StringBuffer();
        result.append(" - outpoint: \n");
        result.append(pre_outpoint);
        result.append("\n");
        result.append(" - scriptLength: " + script_length + "\n");
        result.append(" - sequence: " + sequence + "\n");
        return result.toString();
    }
}
