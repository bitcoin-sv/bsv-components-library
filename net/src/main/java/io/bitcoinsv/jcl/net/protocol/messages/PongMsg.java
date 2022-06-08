package io.bitcoinsv.jcl.net.protocol.messages;


import com.google.common.base.Objects;
import io.bitcoinsv.jcl.net.protocol.messages.common.BodyMessage;

import java.io.Serializable;

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * The ping message is sent primarily to confirm that the TCP/IP connection is still valid.
 * An error in transmission is presumed to be a closed connection and the address is removed as a current peer.
 *
 *  Structure of the BODY of Message:
 *
 * - field: "nonce" (8 bytes) uint64_t
 *   Random nonce assigned to this pong message.
 *   The pong message sends back the same nonce received in the ping message it is replying to.
 */
public final class PongMsg extends BodyMessage implements Serializable {
    protected static final int FIXED_MESSAGE_LENGTH = 8;
    public static final String MESSAGE_TYPE = "pong";

    private final long nonce;

    public PongMsg(long nonce, byte[] extraBytes, long checksum) {
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
        return "PongMsg(nonce=" + this.getNonce() + ")";
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), nonce);
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) { return false; }
        PongMsg other = (PongMsg) obj;
        return Objects.equal(this.nonce, other.nonce);
    }

    public static PongMsgBuilder builder() {
        return new PongMsgBuilder();
    }

    @Override
    public PongMsgBuilder toBuilder() {
        return new PongMsgBuilder(super.extraBytes, super.checksum).nonce(this.nonce);
    }

    /**
     * Builder
     */
    public static class PongMsgBuilder extends BodyMessageBuilder {
        private long nonce;

        public PongMsgBuilder() {}
        public PongMsgBuilder(byte[] extraBytes, long checksum) { super(extraBytes, checksum);}

        public PongMsg.PongMsgBuilder nonce(long nonce) {
            this.nonce = nonce;
            return this;
        }

        public PongMsg build() {
            return new PongMsg(nonce, super.extraBytes, super.checksum);
        }
    }
}
