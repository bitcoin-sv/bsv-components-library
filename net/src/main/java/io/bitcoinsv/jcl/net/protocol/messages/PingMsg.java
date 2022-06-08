package io.bitcoinsv.jcl.net.protocol.messages;

import com.google.common.base.Objects;
import io.bitcoinsv.jcl.net.protocol.messages.common.BodyMessage;

import java.io.Serializable;


/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 *
 * The ping message is sent primarily to confirm that the TCP/IP connection is still valid. An error in transmission is
 * presumed to be a closed connection and the address is removed as a current peer.The response to a ping message
 * is the pong message.
 *
 * * Structure of the BODY of Message:
 *
 * - field: "nonce" (8 bytes) uint64_t
 *   Random nonce assigned to this ping message. The responding pong message will include this nonce to
 *   identify the ping message to which it is replying.
 *
 */
public final class PingMsg extends BodyMessage implements Serializable {
    public static final String MESSAGE_TYPE = "ping";
    protected static final int FIXED_MESSAGE_LENGTH = 8;

    private final long nonce;

    protected PingMsg(long nonce, byte[] extraBytes, long checksum) {
        super(extraBytes, checksum);
        this.nonce = nonce;
        init();
    }

    @Override
    protected long calculateLength() {
        long length = FIXED_MESSAGE_LENGTH;
        return length;
    }

    @Override
    protected void validateMessage() {}

    @Override
    public String getMessageType()  { return MESSAGE_TYPE; }
    public long getNonce()          { return this.nonce; }

    @Override
    public String toString() {
        return "PingMsg(nonce=" + this.getNonce() + ")";
    }
    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), nonce);
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) { return false; }
        PingMsg other = (PingMsg) obj;
        return Objects.equal(this.nonce, other.nonce);
    }

    public static PingMsgBuilder builder() {
        return new PingMsgBuilder();
    }

    @Override
    public PingMsgBuilder toBuilder() {
        return new PingMsgBuilder(super.extraBytes, super.checksum).nonce(this.nonce);
    }

    /**
     * Builder
     */
    public static class PingMsgBuilder extends BodyMessageBuilder{
        private long nonce;

        public PingMsgBuilder() { }
        public PingMsgBuilder(byte[] extraBytes, long checksum) { super(extraBytes, checksum);}

        public PingMsg.PingMsgBuilder nonce(long nonce) {
            this.nonce = nonce;
            return this;
        }

        public PingMsg build() {
            return new PingMsg(nonce, super.extraBytes, super.checksum);
        }
    }
}
