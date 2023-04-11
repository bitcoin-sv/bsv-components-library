package io.bitcoinsv.bsvcl.net.protocol.messages;

import com.google.common.base.Objects;
import io.bitcoinsv.bsvcl.net.protocol.messages.common.BodyMessage;

import java.io.Serializable;

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * The getheadersen message is an upgrade to the existing getheaders message.This message returns same data as
 * getheaders message with the addition of fields for actual number of transactions that are included in the block
 * and proof of inclussion for coinbase transaction along with the whole coinbase transaction.
 */
public final class GetHeadersEnMsg extends BodyMessage implements Serializable {
    public static final String MESSAGE_TYPE = "getheadersen";
    private final long version;
    private final HashMsg blockLocatorHash;
    private final HashMsg hashStop;
    public static final int VERSION_LENGTH = 4;

    public GetHeadersEnMsg(long version, HashMsg blockLocatorHash, HashMsg hashStop,
                           byte[] extraBytes, long checksum) {
        super(extraBytes, checksum);
        this.version = version;
        this.blockLocatorHash = blockLocatorHash;
        this.hashStop = hashStop;
        init();
    }

    @Override
    protected long calculateLength() {
        long length = VERSION_LENGTH +   HashMsg.HASH_LENGTH + HashMsg.HASH_LENGTH;
        return length;
    }

    @Override
    protected void validateMessage() {}

    @Override
    public String getMessageType()          { return MESSAGE_TYPE; }
    public long getVersion()                { return this.version; }
    public HashMsg getBlockLocatorHash()    { return this.blockLocatorHash; }
    public HashMsg getHashStop()            { return this.hashStop; }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), version, blockLocatorHash, hashStop);
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) { return false; }
        GetHeadersEnMsg other = (GetHeadersEnMsg) obj;
        return Objects.equal(this.version, other.version)
                && Objects.equal(this.blockLocatorHash, other.blockLocatorHash)
                && Objects.equal(this.hashStop, other.hashStop);
    }

    @Override
    public String toString() {
        return "GetHeadersEnMsg(version=" + this.getVersion() + ", blockLocatorHash=" + this.getBlockLocatorHash() + ", hashStop=" + this.getHashStop() + ")";
    }

    public static GetHeadersEnMsgBuilder builder() {
        return new GetHeadersEnMsgBuilder();
    }

    @Override
    public GetHeadersEnMsgBuilder toBuilder() {
        return new GetHeadersEnMsgBuilder(super.extraBytes, super.checksum)
                        .version(this.version)
                        .blockLocatorHash(this.blockLocatorHash)
                        .hashStop(this.hashStop);
    }

    /**
     * Builder
     */
    public static class GetHeadersEnMsgBuilder extends BodyMessageBuilder {
        private long version;
        private HashMsg blockLocatorHash;
        private HashMsg hashStop;

        public GetHeadersEnMsgBuilder() {}
        public GetHeadersEnMsgBuilder(byte[] extraBytes, long checksum) { super(extraBytes, checksum);}

        public GetHeadersEnMsg.GetHeadersEnMsgBuilder version(long version) {
            this.version = version;
            return this;
        }

        public GetHeadersEnMsg.GetHeadersEnMsgBuilder blockLocatorHash(HashMsg blockLocatorHash) {
            this.blockLocatorHash = blockLocatorHash;
            return this;
        }

        public GetHeadersEnMsg.GetHeadersEnMsgBuilder hashStop(HashMsg hashStop) {
            this.hashStop = hashStop;
            return this;
        }

        public GetHeadersEnMsg build() {
            return new GetHeadersEnMsg(version, blockLocatorHash, hashStop, super.extraBytes, super.checksum);
        }
    }
}
