package io.bitcoinsv.jcl.net.protocol.messages;

import com.google.common.base.Objects;
import io.bitcoinsv.bitcoinjsv.core.Sha256Hash;
import io.bitcoinsv.jcl.net.protocol.messages.common.BodyMessage;

import java.io.Serializable;

public final class GetCompactBlockMsg extends BodyMessage implements Serializable {
    public static final String MESSAGE_TYPE = "getcmpctblck";
    private final HashMsg blockHash;

    protected GetCompactBlockMsg(HashMsg blockHash, byte[] extraBytes, long checksum) {
        super(extraBytes, checksum);
        this.blockHash = blockHash;
        init();
    }

    @Override
    protected long calculateLength() { return blockHash.getLengthInBytes(); }

    @Override
    protected void validateMessage() { }

    @Override
    public String getMessageType() { return MESSAGE_TYPE; }
    public HashMsg getblockHash() { return blockHash; }

    @Override
    public String toString() {
        return "GetCompactBlockMsg(blockHash=" + blockHash.toString() + ")";
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), blockHash);
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) { return false; }
        GetCompactBlockMsg other = (GetCompactBlockMsg) obj;
        return Objects.equal(blockHash, other.blockHash);
    }


    public static GetCompactBlockMsgBuilder builder() {
        return new GetCompactBlockMsgBuilder();
    }

    @Override
    public GetCompactBlockMsgBuilder toBuilder() {
        return new GetCompactBlockMsgBuilder(extraBytes, checksum).blockHash(blockHash);
    }

    /**
     * Builder
     */
    public static class GetCompactBlockMsgBuilder extends BodyMessageBuilder {
        private HashMsg blockHash;

        public GetCompactBlockMsgBuilder() {}
        public GetCompactBlockMsgBuilder(byte[] extraBytes, long checksum) { super(extraBytes, checksum);}

        public GetCompactBlockMsgBuilder blockHash(HashMsg blockHash) {
            this.blockHash = blockHash;
            return this;
        }

        public GetCompactBlockMsgBuilder blockHash(Sha256Hash blockHash) {
            this.blockHash = HashMsg.builder().hash(blockHash.getBytes()).build();
            return this;
        }

        public GetCompactBlockMsg build() {
            return new GetCompactBlockMsg(blockHash, extraBytes, checksum);
        }
    }
}
