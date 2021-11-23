package com.nchain.jcl.net.protocol.messages;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.primitives.Bytes;
import com.nchain.jcl.net.protocol.messages.common.Message;
import io.bitcoinj.core.Utils;

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
public class HashMsg extends Message implements Serializable {
    public static final String MESSAGE_TYPE = "hash";
    public static final int HASH_LENGTH = 32;//32 bits

    private final byte[] hashBytes;

  protected HashMsg(byte[] hash, long payloadChecksum) {
      super(payloadChecksum);
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
        return Objects.hashCode(hashBytes);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) { return false; }
        if (obj == this) { return true; }
        if (obj.getClass() != getClass()) { return false; }
        HashMsg other = (HashMsg) obj;
        // IMPORTANT: For array comparison, use "Arrays.equals()" instead of "Objects.equals()"
        return Arrays.equals(this.hashBytes, other.hashBytes);
    }

    @Override
    public HashMsgBuilder toBuilder() {
      return new HashMsgBuilder(super.extraBytes, super.payloadChecksum).hash(this.hashBytes);
    }

    /**
     * Builder
     */
    public static class HashMsgBuilder extends MessageBuilder{
        private byte[] hash;

        public HashMsgBuilder() {}
        public HashMsgBuilder(byte[] extraBytes, long payloadChecksum) { super(extraBytes, payloadChecksum);}

        public HashMsg.HashMsgBuilder hash(byte[] hash) {
            this.hash = hash;
            return this;
        }

        public HashMsg build() {
            return new HashMsg(hash, super.payloadChecksum);
        }

    }
}
