package com.nchain.jcl.net.protocol.messages;

import com.google.common.base.Objects;
import com.nchain.jcl.net.protocol.messages.common.Message;

import java.io.Serializable;


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
 *
 *   -------------------------------------------------------------------------------------------------------------------
 *   UPDATE:
 *   In order to support messages larger than 4GB, new fields have been added to the Header, and they are only
 *   processed if the protocol is >= 70016:
 *
 *   - field: "command" (12 bytes) char[12]
 *     The extended message type identifier (NULL terminated). The real contained message type, for example "block"
 *     for a > 4GB block, or could also conceivably be "tx" if we decide in future to support > 4GB transactions, or
 *     any other message type in future we need to be large.
 *
 *   - field: "length" (8 bytes) uint64_t
 *     The extended payload length. The real length of the following message payload.
 *
 *   For more info check out the link below:
 *   @see <a href="https://confluence.stressedsharks.com/display/BSV/Supporting+Payloads+%3E+4GB">Supporting Payloads &gt; 4GB</a>
 *
 *   -------------------------------------------------------------------------------------------------------------------
 *
 */
public final class HeaderMsg extends Message implements Serializable {

    public static final String MESSAGE_TYPE = "header";

    // The HeaderMsg has a static length, but it depends on the protocol version:
    public static final long MESSAGE_LENGTH = 24;           // for protocol < 70016:
    public static final long MESSAGE_LENGTH_EXT = 44;       // for protocol >= 70016 (support for >6GB messages)

    private final long magic;
    private final String command;
    private final long length;
    private final long checksum;

    // Extra-fields to support >6GB messages (only for protocol >= 70016):
    public static final String EXT_COMMAND = "extmsg"; // value in "command" field for extended messages
    public static final long EXT_LENGTH = 0xFFFFFFFF;
    private final String extCommand;
    private final long extLength;

    // Constructor. to create instance  of this class, use the Builder
    protected HeaderMsg(long magic, String command,
                        long length, long checksum,
                        String extCommand, long extLength,
                        long payloadChecksum) {
        super(payloadChecksum);
        this.magic = magic;
        this.command = command;
        this.length = length;
        this.checksum = checksum;
        this.extCommand = extCommand;
        this.extLength = extLength;
        init();
    }

    protected long calculateLength() {
        long lengthInBytes  = (this.command.equalsIgnoreCase(EXT_COMMAND)) ? MESSAGE_LENGTH_EXT : MESSAGE_LENGTH;
        return lengthInBytes;
    }

    protected void validateMessage() {}

    @Override
    public String getMessageType()  { return MESSAGE_TYPE; }
    public long getMagic()          { return this.magic; }
    public String getCommand()      { return this.command; }
    public long getLength()         { return this.length; }
    public long getChecksum()       { return this.checksum; }
    public String getExtCommand()   { return this.extCommand;}
    public long getExtLength()      { return this.extLength;}

    // Convenience:
    public boolean isExtendedMsg() { return command.equalsIgnoreCase(EXT_COMMAND);}

    /**
     * It returns the type of message, which has been historically stored in the "command" field of the header.
     * After 70016, the type of the message might be stored in the "command" of "extCommand" field depending on its
     * size, this method retrieves its value in any case.
     */
    public String getMsgCommand() { return (command.equalsIgnoreCase(EXT_COMMAND)) ? extCommand : command;}

    /**
     * It returns the length of the payload, which has been historically stored in the "length" field of the header.
     * After 70016, the length might be stored in the "length" of "extLength" field depending on its
     * size, this method retrieves its value in any case.
     */
    public long getMsgLength() { return (command.equalsIgnoreCase(EXT_COMMAND)) ? extLength : length;}

    @Override
    public String toString() {
        return "HeaderMsg(magic=" + this.getMagic() + ", command=" + this.getCommand() + ", length=" + this.getLength() + ", checksum=" + this.getChecksum() + ",extCommand=" + this.getExtCommand() + ", extLength=" + getExtLength() +")";
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

    @Override
    public HeaderMsgBuilder toBuilder() {
        return new HeaderMsgBuilder(super.extraBytes, super.payloadChecksum)
                    .magic(this.magic)
                    .command(this.command)
                    .length(this.length)
                    .checksum(this.checksum)
                    .extCommand(this.extCommand)
                    .extLength(this.extLength);
    }

    /**
     * Builder
     */
    public static class HeaderMsgBuilder extends MessageBuilder {
        private long magic;
        private String command;
        private long length;
        private long checksum;
        private String extCommand;
        private long extLength;

        public HeaderMsgBuilder() {}
        public HeaderMsgBuilder(byte[] extraBytes, long payloadchecksum) { super(extraBytes, payloadchecksum);}

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

        public HeaderMsg.HeaderMsgBuilder extCommand(String extCommand) {
            this.extCommand = extCommand;
            return this;
        }

        public HeaderMsg.HeaderMsgBuilder extLength(long extLength) {
            this.extLength = extLength;
            return this;
        }

        public HeaderMsg build() {
            return new HeaderMsg(magic, command, length, checksum, extCommand, extLength, super.payloadChecksum);
        }
    }
}
