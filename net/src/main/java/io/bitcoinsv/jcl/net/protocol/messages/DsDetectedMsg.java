package io.bitcoinsv.jcl.net.protocol.messages;

import com.google.common.base.Objects;
import io.bitcoinsv.jcl.net.protocol.messages.common.BodyMessage;

import java.util.List;


/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2021 nChain Ltd
 * @date 24/08/2021
 *
 * This message is the main message to be submitted in the event of a double spend. Containing the transaction, block and history information needed in order to verify the double spend.
 */
public final class DsDetectedMsg extends BodyMessage {

    public static final String MESSAGE_TYPE = "dsdetected";

    private int version = 0;
    private VarIntMsg blockCount;
    private List<BlockDetailsMsg> blockList;

    public DsDetectedMsg(int version, List<BlockDetailsMsg> blockList,
                         byte[] extraBytes, long checksum) {
        super(extraBytes, checksum);
        this.version = version;
        this.blockCount = VarIntMsg.builder().value(blockList.size()).build();
        this.blockList = blockList;
        init();
    }

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }

    @Override
    protected long calculateLength() {
        return 2 + blockCount.getLengthInBytes() + blockList.stream().mapToLong(b -> b.getLengthInBytes()).sum();
    }

    @Override
    protected void validateMessage() {
    }

    public VarIntMsg getBlockCount()            { return blockCount; }
    public List<BlockDetailsMsg> getBlockList() { return blockList; }
    public int getVersion()                     { return version; }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) { return false; }
        DsDetectedMsg other = (DsDetectedMsg) obj;
        return Objects.equal(this.blockCount, other.blockCount)
                && Objects.equal(this.blockList, other.blockList)
                && Objects.equal(this.version, other.version);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), this.blockCount, this.blockList, this.version);
    }

    public static DsDetectedMsg.DsDetectedMsgBuilder builder() { return new DsDetectedMsg.DsDetectedMsgBuilder(); }

    @Override public DsDetectedMsgBuilder toBuilder() {
        return new DsDetectedMsgBuilder(super.extraBytes, super.checksum)
                    .version(this.version)
                    .blockList(this.blockList);
    }

    /**
     * Builder
     */
    public static final class DsDetectedMsgBuilder extends BodyMessageBuilder {
        private int version;
        private List<BlockDetailsMsg> blockList;

        private DsDetectedMsgBuilder() {}
        private DsDetectedMsgBuilder(byte[] extraBytes, long checksum) { super(extraBytes, checksum);}

        public DsDetectedMsgBuilder version(int version) {
            this.version = version;
            return this;
        }

        public DsDetectedMsgBuilder blockList(List<BlockDetailsMsg> blockList) {
            this.blockList = blockList;
            return this;
        }

        public DsDetectedMsg build() {
            DsDetectedMsg dsDetectedMsg = new DsDetectedMsg(this.version, this.blockList, super.extraBytes, super.checksum);
            return dsDetectedMsg;
        }
    }
}