package io.bitcoinsv.jcl.net.protocol.messages;

import com.google.common.base.Objects;
import io.bitcoinsv.jcl.net.protocol.messages.common.Message;


/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A HeaderMsg is a common structure used in all the Bitcoin Messages.
 *
 * Structure of the Message:
 *
 *  - field: "magic" (4 bytes) unit32_t
 *    Magic value indicating message origin network, and used to seek to next message when stream workingState is unknown
 *
 * - field: "command" (12 bytes) char[12]
 *   ASCII string identifying the packet content, NULL padded (non-NULL padding results in packet rejected)
 *
 * - field: "length" (4 bytes) uint32_t
 *   Length of payload in number of bytes
 *
 * - field: "checksum" (4 bytes) uint32_t
 *   First 4 bytes of sha256(sha256(payload)).
 *   payload = bytes serialized of the Body of the Bitcoin Message that goes with this Header
 */
public final class HeaderMsg extends Message {

    public static final String MESSAGE_TYPE = "header";
    // The HeaderMsg always has a length of 24 Bytes
    public static final long MESSAGE_LENGTH = 24;

    private final long magic;
    private final String command;
    private final long length;
    private final long checksum;

    // Constructor. to create instance  of this class, use the Builder
    protected HeaderMsg(long magic, String command,
                        long length, long checksum) {
        this.magic = magic;
        this.command = command;
        this.length = length;
        this.checksum = checksum;
        init();
    }

    protected long calculateLength() {
        long lengthInBytes  = MESSAGE_LENGTH;
        return lengthInBytes;
    }

    protected void validateMessage() {}

    @Override
    public String getMessageType()  { return MESSAGE_TYPE; }
    public long getMagic()          { return this.magic; }
    public String getCommand()      { return this.command; }
    public long getLength()         { return this.length; }
    public long getChecksum()       { return this.checksum; }

    public String toString() {
        return "HeaderMsg(magic=" + this.getMagic() + ", command=" + this.getCommand() + ", length=" + this.getLength() + ", checksum=" + this.getChecksum() + ")";
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(magic, command, length, checksum);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) { return false; }
        if (obj == this) { return true; }
        if (obj.getClass() != getClass()) { return false; }
        HeaderMsg other = (HeaderMsg) obj;
        return Objects.equal(this.magic, other.magic)
                && Objects.equal(this.command, other.command)
                && Objects.equal(this.length, other.length)
                && Objects.equal(this.checksum, other.checksum);
    }

    public static HeaderMsgBuilder builder() {
        return new HeaderMsgBuilder();
    }

    public HeaderMsgBuilder toBuilder() {
        return new HeaderMsgBuilder().magic(this.magic).command(this.command).length(this.length).checksum(this.checksum);
    }

    /**
     * Builder
     */
    public static class HeaderMsgBuilder {
        private long magic;
        private String command;
        private long length;
        private long checksum;

        HeaderMsgBuilder() {}

        public HeaderMsg.HeaderMsgBuilder magic(long magic) {
            this.magic = magic;
            return this;
        }

        public HeaderMsg.HeaderMsgBuilder command(String command) {
            this.command = command;
            return this;
        }

        public HeaderMsg.HeaderMsgBuilder length(long length) {
            this.length = length;
            return this;
        }

        public HeaderMsg.HeaderMsgBuilder checksum(long checksum) {
            this.checksum = checksum;
            return this;
        }

        public HeaderMsg build() {
            return new HeaderMsg(magic, command, length, checksum);
        }
    }
}
