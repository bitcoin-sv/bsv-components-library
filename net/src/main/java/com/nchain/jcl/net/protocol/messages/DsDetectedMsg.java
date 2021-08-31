package com.nchain.jcl.net.protocol.messages;

import com.nchain.jcl.net.protocol.messages.common.Message;

import java.util.List;


/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2021 nChain Ltd
 * @date 24/08/2021
 */
public class DsDetectedMsg extends Message {

    public static final String MESSAGE_TYPE = "dsdetected";

    private int version = 0;
    private VarIntMsg blockCount;
    private List<BlockDetailsMsg> blockList;

    public DsDetectedMsg() {
        init();
    }

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }

    @Override
    protected long calculateLength() {
        return 0;
    }

    @Override
    protected void validateMessage() {
    }


    public VarIntMsg getBlockCount() {
        return blockCount;
    }

    public void setBlockCount(VarIntMsg blockCount) {
        this.blockCount = blockCount;
    }

    public List<BlockDetailsMsg> getBlockList() {
        return blockList;
    }

    public void setBlockList(List<BlockDetailsMsg> blockList) {
        this.blockList = blockList;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public static DsDetectedMsg.DsDetectedMsgBuilder builder() {
        return new DsDetectedMsg.DsDetectedMsgBuilder();
    }

    public static final class DsDetectedMsgBuilder {
        private int version;
        private VarIntMsg blockCount;
        private List<BlockDetailsMsg> blockList;

        private DsDetectedMsgBuilder() {
        }

        public static DsDetectedMsgBuilder DsDetectedMsgBuilder() {
            return new DsDetectedMsgBuilder();
        }

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
            DsDetectedMsg dsDetectedMsg = new DsDetectedMsg();
            dsDetectedMsg.setVersion(version);
            dsDetectedMsg.setBlockCount(blockCount);
            dsDetectedMsg.setBlockList(blockList);
            return dsDetectedMsg;
        }
    }
}