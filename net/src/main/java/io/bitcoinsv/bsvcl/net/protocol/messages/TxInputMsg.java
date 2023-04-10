package io.bitcoinsv.bsvcl.net.protocol.messages;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.Tx;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.TxInput;
import io.bitcoinsv.bitcoinjsv.bitcoin.bean.base.TxInputBean;
import io.bitcoinsv.bitcoinjsv.bitcoin.bean.base.TxOutPointBean;
import io.bitcoinsv.bsvcl.net.protocol.messages.common.Message;

import java.io.Serializable;

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
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
public final class TxInputMsg extends Message implements Serializable {
    private static final int sequence_len = 4;
    public static final String MESSAGE_TYPE = "TxIn";

    private final TxOutPointMsg pre_outpoint;
    private final VarIntMsg script_length;
    private final byte[] signature_script;
    private final long sequence;


    protected TxInputMsg(TxOutPointMsg pre_outpoint, byte[] signature_script, long sequence) {
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
    public String getMessageType()          { return MESSAGE_TYPE; }
    public TxOutPointMsg getPre_outpoint()  { return this.pre_outpoint; }
    public VarIntMsg getScript_length()     { return this.script_length; }
    public byte[] getSignature_script()     { return this.signature_script; }
    public long getSequence()               { return this.sequence; }

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

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), pre_outpoint, script_length, signature_script, sequence);
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) { return false; }
        TxInputMsg other = (TxInputMsg) obj;
        return Objects.equal(this.pre_outpoint, other.pre_outpoint)
                && Objects.equal(this.script_length, other.script_length)
                && Objects.equal(this.signature_script, other.signature_script)
                && Objects.equal(this.sequence, other.sequence);
    }

    public static TxInputMsgBuilder builder() {
        return new TxInputMsgBuilder();
    }

    public TxInputMsgBuilder toBuilder() {
        return new TxInputMsgBuilder()
                    .pre_outpoint(this.pre_outpoint)
                    .signature_script(this.signature_script)
                    .sequence(this.sequence);
    }

    /** Returns a Domain Class */
    public TxInput toBean() {
        TxInput result = new TxInputBean((Tx) null);
        TxOutPointBean txOutPointBean = this.getPre_outpoint().toBean();
        result.setOutpoint(txOutPointBean);
        result.setScriptBytes(this.getSignature_script());
        result.setSequenceNumber(this.sequence);
        return result;
    }

    /** Returns a Msg class out of a Bean */
    public static TxInputMsg fromBean(TxInput bean) {
        TxInputMsg result = TxInputMsg.builder()
                .sequence(bean.getSequenceNumber())
                .signature_script(bean.getScriptBytes())
                .pre_outpoint(TxOutPointMsg.fromBean(bean.getOutpoint()))
                .build();
        return result;
    }

    /**
     * Builder
     */
    public static class TxInputMsgBuilder {
        private TxOutPointMsg pre_outpoint;
        private byte[] signature_script;
        private long sequence;

        public TxInputMsgBuilder() { }

        public TxInputMsg.TxInputMsgBuilder pre_outpoint(TxOutPointMsg pre_outpoint) {
            this.pre_outpoint = pre_outpoint;
            return this;
        }

        public TxInputMsg.TxInputMsgBuilder signature_script(byte[] signature_script) {
            this.signature_script = signature_script;
            return this;
        }

        public TxInputMsg.TxInputMsgBuilder sequence(long sequence) {
            this.sequence = sequence;
            return this;
        }

        public TxInputMsg build() {
            return new TxInputMsg(pre_outpoint, signature_script, sequence);
        }
    }
}
