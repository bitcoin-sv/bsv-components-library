package com.nchain.jcl.net.protocol.messages;


import com.google.common.base.Objects;
import io.bitcoinj.core.Sha256Hash;


/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A RawTx is a Message containing a Tx in Raw format, containing a Byte Array without applying any
 * Serialization/Deserialization on it. It also includes the TxHash as a convenience field.
 *
 * Structure of the Message:
 * - hash: Hash of the Tx
 */
public class RawTxMsg extends RawMsg {

    public static final String MESSAGE_TYPE = "tx";

    // Tx Hash in readable format (reversed)
    private Sha256Hash hash;

    public RawTxMsg(byte[] content, Sha256Hash hash) {
        super(content);
        this.hash = hash;
        init();
    }

    @Override
    public String getMessageType() { return MESSAGE_TYPE; }

    @Override
    protected long calculateLength() {
        return super.content.length;
    }

    @Override
    protected void validateMessage() {}

    public Sha256Hash getHash() {
        return this.hash;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(hash);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) { return false; }
        if (obj == this) { return true; }
        if (obj.getClass() != getClass()) { return false; }
        RawTxMsg other = (RawTxMsg) obj;
        return Objects.equal(this.hash, other.hash);
    }

    @Override
    public String toString() {
        return "RawTxMsg(hash=" + this.getHash() + ")";
    }
}
