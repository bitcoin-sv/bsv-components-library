package com.nchain.jcl.net.protocol.messages;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.nchain.jcl.net.protocol.messages.common.Message;

import java.io.Serializable;
import java.util.List;

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * @date 16/09/2019
 *
 * It holds structure of the the GetHeaders and GetBlocks body message
 *
 * Structure of the BODY of Message:
 *
 *  - field: "version" (4 bytes)
 *  Identifies connection version being used by the node
 *
 *  - field: "hash count" (1+ bytes) VarIntMsg
 *  number of block locator hash entries
 *
 *   - field: "block locator hashest"  (32+ bytes)
 *   block locator object; newest back to genesis block (dense to start, but then sparse)
 *
 *   - field: "hash_stop"  (32 bytes)
 *   hash of the last desired block; set to zero to get as many blocks as possible (500)
 */
public final class BaseGetDataAndHeaderMsg extends Message implements Serializable {

    private final long version;
    private final VarIntMsg hashCount;
    private final ImmutableList<HashMsg> blockLocatorHash;
    private final HashMsg hashStop;
    public static final String MESSAGE_TYPE = "baseGetDataAndHeaderMsg";
    public static final int VERSION_LENGTH = 4;

    protected BaseGetDataAndHeaderMsg( long version, VarIntMsg hashCount, List<HashMsg>  blockLocatorHash, HashMsg hashStop, long payloadChecksum) {
        super(payloadChecksum);
        this.version = version;
        this.hashCount = hashCount;
        this.blockLocatorHash = ImmutableList.copyOf(blockLocatorHash);
        this.hashStop = hashStop;
        init();
    }

    @Override
    protected long calculateLength() {
        long lengthInBytes  = VERSION_LENGTH + hashCount.getLengthInBytes() + blockLocatorHash.size()* HashMsg.HASH_LENGTH + HashMsg.HASH_LENGTH;
        return lengthInBytes;
    }
    @Override
    protected void validateMessage() {}

    @Override
    public String getMessageType()                      { return MESSAGE_TYPE; }
    public long getVersion()                            { return this.version; }
    public VarIntMsg getHashCount()                     { return this.hashCount; }
    public ImmutableList<HashMsg> getBlockLocatorHash() { return this.blockLocatorHash; }
    public HashMsg getHashStop()                        { return this.hashStop; }

    @Override
    public String toString() {
        return "BaseGetDataAndHeaderMsg(version=" + this.getVersion() + ", hashCount=" + this.getHashCount() + ", blockLocatorHash=" + this.getBlockLocatorHash() + ", hashStop=" + this.getHashStop() + ")";
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(version, blockLocatorHash, hashStop);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) { return false; }
        if (obj == this) { return true; }
        if (obj.getClass() != getClass()) { return false; }
        BaseGetDataAndHeaderMsg other = (BaseGetDataAndHeaderMsg) obj;
        return Objects.equal(this.version, other.version)
                && Objects.equal(this.blockLocatorHash, other.blockLocatorHash)
                && Objects.equal(this.hashStop, other.hashStop);
    }

    @Override
    public BaseGetDataAndHeaderMsgBuilder toBuilder() {
        return new BaseGetDataAndHeaderMsgBuilder(super.extraBytes, super.payloadChecksum)
                    .version(this.version)
                    .hashCount(this.hashCount)
                    .hashStop(this.hashStop)
                    .blockLocatorHash(this.blockLocatorHash);
    }

    public static BaseGetDataAndHeaderMsgBuilder builder() {
        return new BaseGetDataAndHeaderMsgBuilder();
    }

    /**
     * Builder
     */
    public static class BaseGetDataAndHeaderMsgBuilder extends MessageBuilder {
        private long version;
        private VarIntMsg hashCount;
        private List<HashMsg> blockLocatorHash;
        private HashMsg hashStop;

        BaseGetDataAndHeaderMsgBuilder() {}
        BaseGetDataAndHeaderMsgBuilder(byte[] extraBytes, long payloadChecksum) { super(extraBytes, payloadChecksum);}

        public BaseGetDataAndHeaderMsg.BaseGetDataAndHeaderMsgBuilder version(long version) {
            this.version = version;
            return this;
        }

        public BaseGetDataAndHeaderMsg.BaseGetDataAndHeaderMsgBuilder hashCount(VarIntMsg hashCount) {
            this.hashCount = hashCount;
            return this;
        }

        public BaseGetDataAndHeaderMsg.BaseGetDataAndHeaderMsgBuilder blockLocatorHash(List<HashMsg> blockLocatorHash) {
            this.blockLocatorHash = blockLocatorHash;
            return this;
        }

        public BaseGetDataAndHeaderMsg.BaseGetDataAndHeaderMsgBuilder hashStop(HashMsg hashStop) {
            this.hashStop = hashStop;
            return this;
        }

        public BaseGetDataAndHeaderMsg build() {
            return new BaseGetDataAndHeaderMsg(version, hashCount, blockLocatorHash, hashStop, super.payloadChecksum);
        }
    }
}
