package io.bitcoinsv.jcl.net.protocol.messages;

import com.google.common.base.Objects;
import io.bitcoinsv.jcl.net.protocol.messages.common.Message;


/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A VarStrMSg is not a fully Bitcoin Message itself, but it's a structure that is reused by different other
 *  messages in the Bitcoin P2P. It represents a Variable Length String, so we only store as many bytes
 *  as we need o store it. Since it's a flexible structure (its size depends on the size of the String), we
 *  follow this consensus to store it:
 *  - variable length integer (VerIntMsg) followed by the string itself.
 *
 * Structure of the Message:
 *
 *  - field: "length" (? bytes) VarIntMSg
 *    Length of the string
 *
 *  - string: "length" (? bytes) char[]
 *    The string itself (can be empty)
 */
public final class VarStrMsg extends Message {

    public static final String MESSAGE_TYPE = "varStr";

    private final VarIntMsg strLength;
    private final String str;

    protected VarStrMsg(String str) {
        this.str = str;
        this.strLength = VarIntMsg.builder().value(str.length()).build();
        init();
    }



    @Override
    protected long calculateLength() {
        long length =  strLength.getLengthInBytes() + str.length();;
        return length;
    }

    @Override
    protected void validateMessage() {}

    @Override
    public String getMessageType()  { return MESSAGE_TYPE;}
    public VarIntMsg getStrLength() { return this.strLength; }
    public String getStr()          { return this.str; }

    @Override
    public String toString() {
        return "VarStrMsg(strLength=" + this.getStrLength() + ", str=" + this.getStr() + ")";
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(strLength, str);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) { return false; }
        if (obj == this) { return true; }
        if (obj.getClass() != getClass()) { return false; }
        VarStrMsg other = (VarStrMsg) obj;
        return Objects.equal(this.strLength, other.strLength)
                && Objects.equal(this.str, other.str);
    }

    public static VarStrMsgBuilder builder() {
        return new VarStrMsgBuilder();
    }

    /**
     * Builder
     */
    public static class VarStrMsgBuilder {
        private String str;

        VarStrMsgBuilder() {
        }

        public VarStrMsg.VarStrMsgBuilder str(String str) {
            this.str = str;
            return this;
        }

        public VarStrMsg build() {
            return new VarStrMsg(str);
        }

        public String toString() {
            return "VarStrMsg.VarStrMsgBuilder(str=" + this.str + ")";
        }
    }
}
