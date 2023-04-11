package io.bitcoinsv.bsvcl.net.protocol.messages;


import com.google.common.base.Objects;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import io.bitcoinsv.bitcoinjsv.core.Sha256Hash;
import io.bitcoinsv.bitcoinjsv.core.Utils;

import java.io.Serializable;


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
public final class RawTxMsg extends RawMsg implements Serializable {

    public static final String MESSAGE_TYPE = "tx";

    private static HashFunction hashFunction = Hashing.sha256();

    // Tx Hash in readable format (reversed)
    private Sha256Hash hash;

    public RawTxMsg(byte[] content, Sha256Hash hash, byte[] extraBytes, long checksum) {
        super(content, extraBytes, checksum);
        this.hash = hash;
        init();
    }

    public RawTxMsg(byte[] content, long checksum) {
        this(content, null, Utils.EMPTY_BYTE_ARRAY,  checksum);
    }

    // Calculate the Hash...
    private void calculateHash() {
        this.hash = Sha256Hash.wrapReversed(hashFunction.hashBytes(hashFunction.hashBytes(content).asBytes()).asBytes());
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
        if (this.hash == null) {
            calculateHash();
        }
        return this.hash;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj))           { return false; }
        if (obj.getClass() != getClass()) { return false; }
        return true;
    }

    @Override
    public String toString() {
        return "RawTxMsg(hash=" + this.hash + ")";
    }

    @Override
    public RawTxMsgBuilder toBuilder() {
        return new RawTxMsgBuilder(super.extraBytes, super.checksum)
                    .content(this.content)
                    .hash(this.hash);
    }

    /**
     * Builder
     */
    public static class RawTxMsgBuilder extends BodyMessageBuilder {
        private byte[] content;
        private Sha256Hash hash;

        public RawTxMsgBuilder() {}
        public RawTxMsgBuilder(byte[] extraBytes, long checksum) { super(extraBytes, checksum);}
        public RawTxMsgBuilder content(byte[] content) {
            this.content = content;
            return this;
        }

        public RawTxMsgBuilder hash(Sha256Hash hash) {
            this.hash = hash;
            return this;
        }

        public RawMsg build() {
            return new RawTxMsg(content, hash, super.extraBytes, super.checksum);
        }
    }
}
