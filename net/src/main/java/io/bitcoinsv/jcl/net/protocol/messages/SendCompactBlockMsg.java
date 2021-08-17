/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.protocol.messages;

import io.bitcoinsv.jcl.net.protocol.messages.common.Message;

/**
 * @author j.pomer@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */
public class SendCompactBlockMsg extends Message {

    public static final String MESSAGE_TYPE = "sendcmpct";

    private static final long BYTE_SIZE = 9;

    private final boolean highBandwidthRelaying;
    private final long version;

    public SendCompactBlockMsg(boolean highBandwidthRelaying, long version) {
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

    public static class SendCompactBlockMsgBuilder {
        private boolean highBandwidthRelaying;
        private long version;

        public SendCompactBlockMsgBuilder highBandwidthRelaying(boolean highBandwidthRelaying) {
            this.highBandwidthRelaying = highBandwidthRelaying;
            return this;
        }

        public SendCompactBlockMsgBuilder version(long version) {
            this.version = version;
            return this;
        }

        public SendCompactBlockMsg build() {
            return new SendCompactBlockMsg(highBandwidthRelaying, version);
        }
    }

    @Override
    public String toString() {
        return "SendCompactBlockMsg(highBandwidthRelaying=" + isHighBandwidthRelaying() +
            ", version=" + getVersion() + ")";
    }
}
