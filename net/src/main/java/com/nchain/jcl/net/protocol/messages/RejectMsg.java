package com.nchain.jcl.net.protocol.messages;


import com.nchain.jcl.base.tools.crypto.Sha256Wrapper;
import com.nchain.jcl.net.protocol.messages.common.Message;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * The reject message is sent when messages are rejected.
 * This message has been defined in BIP 61:
 * @see <a href="https://github.com/bitcoin/bips/blob/master/bip-0061.mediawiki">BIP 61</a>
 *
 * Structure of the Message:
 *
 *  - field: "message" (1+ bytes) var_str
 *    type of message rejected
 *
 *  - field: "ccode" (1 bytes) char
 *    code relating to rejected message
 *
 *  - field: "reason" (1+ bytes) var_str
 *    text version of reason for rejection
 *
 *  - field: "data" (0+ bytes) var_str
 *    Optional extra data provided by some errors. Currently, all errors which provide this field fill it
 *    with the TXID or block header hash of the object being rejected, so the field is 32 bytes.
 *
 */
@Getter
@ToString
@EqualsAndHashCode
public class RejectMsg extends Message {

    /**
     * Reference of all the possible values for the MESSAGE field:
     */
    public enum MessageType {
        VERSION("version"),
        TX("tx"),
        BLOCK("block");

        private String messageType;
        MessageType(String messageType) { this.messageType = messageType; }
        public String getMessageType() { return messageType; }
    }

    /**
     * Reference of all the possible values for the CCODE field:
     */
    public enum RejectCode {

        /** The message was not able to be parsed */
        MALFORMED((byte) 0x01),

        /** The message described an invalid object */
        INVALID((byte) 0x10),

        /** The message was obsolete or described an object which is obsolete (eg unsupported, old version, v1 block) */
        OBSOLETE((byte) 0x11),

        /**
         * The message was relayed multiple times or described an object which is in conflict with another.
         * This message can describe errors in connection implementation or the presence of an attempt to DOUBLE SPEND.
         */
        DUPLICATE((byte) 0x12),

        /**
         * The message described an object was not standard and was thus not accepted.
         * Bitcoin Core has a concept of standard transaction forms, which describe scripts and encodings which
         * it is willing to isHandshakeUsingRelay further. Other transactions are neither relayed nor mined, though they are considered
         * valid if they appear in a block.
         */
        NONSTANDARD((byte) 0x40),

        /**
         * This refers to a specific form of NONSTANDARD transactions, which have an output smaller than some constant
         * defining them as dust (this is no longer used).
         */
        DUST((byte) 0x41),

        /** The messages described an object which did not have sufficient fee to be relayed further. */
        INSUFFICIENTFEE((byte) 0x42),

        /** The message described a block which was invalid according to hard-coded checkpoint blocks. */
        CHECKPOINT((byte) 0x43),

        /** Any other Reason Code that does NOt matched the previous ones */
        OTHER((byte) 0xff);

        byte code;
        RejectCode(byte code) { this.code = code;}
        public byte getValue() { return code;}
        public static RejectCode fromCode(byte code) {
            for (RejectCode rejectCode : RejectCode.values())
                if (rejectCode.code == code)
                    return rejectCode;
            return OTHER;
        }
    }

    public static final String MESSAGE_TYPE = "reject";

    // Predefined values of the "message" field that indicate that the content if the "data" field is a
    // HASH (otherwise is a generic byte array)
    public static final String MESSAGE_BLOCK = "block";
    public static final String MESSAGE_TX = "tx";
    private static int FIXED_CCODE_LENGTH = 1;


    private VarStrMsg message;
    private RejectCode ccode;
    private VarStrMsg reason;
    private byte[] data;

    // Convenience field: At this moment, the only application for the "data" field is to store the HASH of
    // either a TX/Block Header Hash, so we keep the general case "byte[]" but we use the "dataHash" in those
    // specific cases (which are the only cases right now):
    private Sha256Wrapper dataHash;



    @Builder
    protected RejectMsg(VarStrMsg message, RejectCode ccode, VarStrMsg reason ,Sha256Wrapper dataHash, byte[] data) {
        this.message = message;
        this.ccode = ccode;
        this.reason = reason;
        this.dataHash = dataHash;
        this.data = data;
        init();
    }

    @Override
    public String getMessageType() { return MESSAGE_TYPE; }


    @Override
    protected long calculateLength() {
        long length  = message.getLengthInBytes()
                + FIXED_CCODE_LENGTH
                + reason.getLengthInBytes();
        if (dataHash != null) lengthInBytes += 32;
        if (data != null) lengthInBytes += data.length;
        return length;
    }

    @Override
    protected void validateMessage() {}
}
