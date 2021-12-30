package io.bitcoinsv.jcl.net.protocol.messages;

import com.google.common.base.Objects;
import io.bitcoinsv.jcl.net.protocol.messages.common.Message;

import java.io.Serializable;


/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A VarIntMSg is not a fully Bitcoin Message itself, but it's a structure that is reused by different other
 * messages in the Bitcoin P2P. It represents a numeric valueInteger that can be encoded depending on
 * the represented value to save space. Variable length integers always precede an array/vector of a type of
 * data that may vary in length. Longer numbers are encoded in little endian.
 *
 * - value: < 0xFD
 *   storage length: 1
 *   format: uint8_t
 *
 * - value: <= 0xFFFF
 *   storage length: 3
 *   format: 0xFD followed by the length as uint16_t
 *
 * - value: <= 0xFFFF FFFF
 *   storage length: 5
 *   format: 0xFE followed by the length as uint32_t
 *
 * - value: -
 *   storage length: 9
 *   format: 0xFF followed by the length as uint64_t
 *
 */
public final class VarIntMsg extends Message implements Serializable {

    public static final String MESSAGE_TYPE = "varInt";

    private final long value;

    protected VarIntMsg(long value) {
        this.value = value;
        init();
    }

    @Override
    protected long calculateLength() {
        long length = getSizeInbytes();
        return length;
    }


    /**
     * Returns the size in bytes that will be taken by this value, following the algorithm as its specified in
     * the Bitcoin P2P (code from BitcoinJ)
     *
     */
    private int getSizeInbytes() {
        // if negative, it's actually a very largeMsgs unsigned long value
        if (value < 0) return 9; // 1 marker + 8 data bytes
        if (value < 253) return 1; // 1 data byte
        if (value <= 0xFFFFL) return 3; // 1 marker + 2 data bytes
        if (value <= 0xFFFFFFFFL) return 5; // 1 marker + 4 data bytes
        if (value <= 0xFFFFFFFFFFL) return 6; // 1 marker + 5 data bytes
        return 9; // 1 marker + 8 data bytes
    }

    @Override
    protected void validateMessage() {}

    @Override
    public String getMessageType()  { return MESSAGE_TYPE;}
    public long getValue()          { return this.value; }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) { return false; }
        if (obj == this) { return true; }
        if (obj.getClass() != getClass()) { return false; }
        VarIntMsg other = (VarIntMsg) obj;
        return Objects.equal(this.value, other.value);
    }

    public static VarIntMsgBuilder builder() {
        return new VarIntMsgBuilder();
    }

    public VarIntMsgBuilder toBuilder() {
        return new VarIntMsgBuilder().value(this.value);
    }

    /**
     * Builder
     */
    public static class VarIntMsgBuilder {
        private long value;

        public VarIntMsgBuilder() {}

        public VarIntMsg.VarIntMsgBuilder value(long value) {
            this.value = value;
            return this;
        }

        public VarIntMsg build() {
            return new VarIntMsg(value);
        }
    }
}
