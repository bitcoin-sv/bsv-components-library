package com.nchain.jcl.net.protocol.messages;

import com.nchain.jcl.net.protocol.messages.common.Message;

import java.util.List;


/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2021 nChain Ltd
 * @date 24/08/2021
 *
 * This message is the main message to be submitted in the event of a double spend. Containing the transaction, block and history information needed in order to verify the double spend.
 */
public class DsDetectedMsg extends Message {

    public static final String MESSAGE_TYPE = "dsdetected";

    private int version = 0;
    private VarIntMsg blockCount;
    private List<BlockDetailsMsg> blockList;

    public DsDetectedMsg(int version, VarIntMsg blockCount, List<BlockDetailsMsg> blockList, long payloadChecksum) {
        super(payloadChecksum);
        this.version = version;
        this.blockCount = blockCount;
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

    public static DsDetectedMsg.DsDetectedMsgBuilder builder() { return new DsDetectedMsg.DsDetectedMsgBuilder(); }

    @Override public DsDetectedMsgBuilder toBuilder() {
        return new DsDetectedMsgBuilder(super.extraBytes, super.payloadChecksum)
                    .withVersion(this.version)
                    .withBlockCount(this.blockCount)
                    .withBlockList(this.blockList);
    }

    /**
     * Builder
     */
    public static final class DsDetectedMsgBuilder extends MessageBuilder {
        private int version;
        private VarIntMsg blockCount;
        private List<BlockDetailsMsg> blockList;

        private DsDetectedMsgBuilder() {}
        private DsDetectedMsgBuilder(byte[] extraBytes, long payloadChecksum) { super(extraBytes, payloadChecksum);}

        public DsDetectedMsgBuilder withVersion(int version) {
            this.version = version;
            return this;
        }

        public DsDetectedMsgBuilder withBlockCount(VarIntMsg blockCount) {
            this.blockCount = blockCount;
            return this;
        }

        public DsDetectedMsgBuilder withBlockList(List<BlockDetailsMsg> blockList) {
            this.blockList = blockList;
            return this;
        }

        public DsDetectedMsg build() {
            DsDetectedMsg dsDetectedMsg = new DsDetectedMsg(this.version, this.blockCount, this.blockList, super.payloadChecksum);
            return dsDetectedMsg;
        }
    }
}