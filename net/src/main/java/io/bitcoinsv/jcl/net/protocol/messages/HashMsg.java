package io.bitcoinsv.jcl.net.protocol.messages;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import io.bitcoinsv.bitcoinjsv.core.Sha256Hash;
import io.bitcoinsv.jcl.net.protocol.messages.common.Message;
import io.bitcoinsv.bitcoinjsv.core.Utils;

import java.io.Serializable;
import java.util.Arrays;

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A HashMsg is not a fully Bitcoin Message itself, but it's a structure that is reused by different other
 * messages in the Bitcoin P2P. It represents a char array that store as many bytes
 * to represent hash of the object.
 */
public final class HashMsg extends Message implements Serializable {
    public static final String MESSAGE_TYPE = "hash";
    public static final int HASH_LENGTH = 32;//32 bits

    private final byte[] hashBytes;

  protected HashMsg(byte[] hash) {
        this.hashBytes = hash;
        init();
    }

    @Override
    protected long calculateLength() {
        long length = this.hashBytes.length;
        return length;
    }

    @Override
    protected void validateMessage() {
        Preconditions.checkArgument( HASH_LENGTH == this.hashBytes.length,"Hash Length is not 32 bytes");
    }

    @Override
    public String toString() { return Utils.HEX.encode(hashBytes);}

    @Override
    public String getMessageType()  { return MESSAGE_TYPE; }
    public byte[] getHashBytes()    { return this.hashBytes; }


    public static HashMsgBuilder builder() {
        return new HashMsgBuilder();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), Arrays.hashCode(hashBytes));
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) { return false; }
        HashMsg other = (HashMsg) obj;
        return Arrays.equals(this.hashBytes, other.hashBytes);
    }

    public HashMsgBuilder toBuilder() {
      return new HashMsgBuilder().hash(this.hashBytes);
    }

    /**
     * Builder
     */
    public static class HashMsgBuilder {
        private byte[] hash;

        public HashMsgBuilder() {}

        public HashMsg.HashMsgBuilder hash(byte[] hash) {
            this.hash = hash;
            return this;
        }

        public HashMsg.HashMsgBuilder hash(Sha256Hash hash) {
            // TODO: fix this when JCL and BitcoinJ won't return human readable hashes
            this.hash = hash.getReversedBytes();
            return this;
        }

        public HashMsg build() {
            return new HashMsg(hash);
        }

    }
}
