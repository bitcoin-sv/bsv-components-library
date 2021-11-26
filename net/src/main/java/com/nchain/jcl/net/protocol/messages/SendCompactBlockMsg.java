package com.nchain.jcl.net.protocol.messages;

import com.nchain.jcl.net.protocol.messages.common.BodyMessage;
import com.nchain.jcl.net.protocol.messages.common.Message;

import java.io.Serializable;

/**
 * @author j.pomer@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */
public class SendCompactBlockMsg extends BodyMessage implements Serializable {

    public static final String MESSAGE_TYPE = "sendcmpct";

    private static final long BYTE_SIZE = 9;

    private final boolean highBandwidthRelaying;
    private final long version;

    public SendCompactBlockMsg(boolean highBandwidthRelaying, long version,
                               byte[] extraBytes, long checksum) {
        super(extraBytes, checksum);
        this.highBandwidthRelaying = highBandwidthRelaying;
        this.version = version;
        init();
    }

    public static SendCompactBlockMsgBuilder builder() {
        return new SendCompactBlockMsgBuilder();
    }

    public boolean isHighBandwidthRelaying() {
        return highBandwidthRelaying;
    }

    public long getVersion() {
        return version;
    }

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }

    @Override
    protected long calculateLength() {
        return BYTE_SIZE;
    }

    @Override
    protected void validateMessage() {
    }

    @Override
    public SendCompactBlockMsgBuilder toBuilder() {
        return new SendCompactBlockMsgBuilder(super.extraBytes, super.checksum)
                        .highBandwidthRelaying(this.highBandwidthRelaying)
                        .version(this.version);
    }
    @Override
    public String toString() {
        return "SendCompactBlockMsg(highBandwidthRelaying=" + isHighBandwidthRelaying() +
            ", version=" + getVersion() + ")";
    }

    /**
     * Builder
     */
    public static class SendCompactBlockMsgBuilder extends BodyMessageBuilder {
        private boolean highBandwidthRelaying;
        private long version;

        public SendCompactBlockMsgBuilder() {}
        public SendCompactBlockMsgBuilder(byte[] extraBytes, long checksum) { super(extraBytes, checksum);}

        public SendCompactBlockMsgBuilder highBandwidthRelaying(boolean highBandwidthRelaying) {
            this.highBandwidthRelaying = highBandwidthRelaying;
            return this;
        }

        public SendCompactBlockMsgBuilder version(long version) {
            this.version = version;
            return this;
        }

        public SendCompactBlockMsg build() {
            return new SendCompactBlockMsg(highBandwidthRelaying, version, super.extraBytes, super.checksum);
        }
    }
}
