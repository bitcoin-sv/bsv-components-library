package com.nchain.jcl.net.protocol.messages;


import com.google.common.base.Objects;
import com.nchain.jcl.net.protocol.messages.common.BodyMessage;
import com.nchain.jcl.net.protocol.messages.common.Message;

import java.io.Serializable;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * The feeFilter message is sent when a Peer wants to announce that is only interested in those Transactions
 * which Fee is equals or higher than the one especified in this message
 * This message has been defined in BIP 0133:
 * @see <a href="https://github.com/bitcoin/bips/blob/master/bip-0133.mediawiki">BIP 0133</a>
 *
 * Structure of the Message:
 * - fee: A long value indicating the Fee (in Satoshis / KB)
 */
public class FeeFilterMsg extends BodyMessage implements Serializable {

    public static final String MESSAGE_TYPE = "feefilter";

    private Long fee;

    public FeeFilterMsg(Long fee,
                        byte[] extraBytes, long checksum) {
        super(extraBytes, checksum);
        this.fee = fee;
        init();
    }

    public Long getFee()                          { return this.fee; }

    @Override public String getMessageType()      { return MESSAGE_TYPE; }
    @Override protected long calculateLength()    { return 8; }
    @Override protected void validateMessage()    {}
    @Override public int hashCode()               { return Objects.hashCode(fee); }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) { return false; }
        if (obj == this) { return true; }
        if (obj.getClass() != getClass()) { return false; }
        FeeFilterMsg other = (FeeFilterMsg) obj;
        return Objects.equal(this.fee, other.fee);
    }

    @Override
    public String toString() {
        return "FeeFilterMsg(fee=" + this.getFee() + ")";
    }

    public static FeeFilterMsgBuilder builder() {
        return new FeeFilterMsgBuilder();
    }

    @Override
    public FeeFilterMsgBuilder toBuilder() {
        return new FeeFilterMsgBuilder(super.extraBytes, super.checksum).fee(this.fee);
    }

    /**
     * Builder
     */
    public static class FeeFilterMsgBuilder extends BodyMessageBuilder {
        private Long fee;

        FeeFilterMsgBuilder() {}
        FeeFilterMsgBuilder(byte[] extraBytes, long checksum) { super(extraBytes, checksum);}

        public FeeFilterMsg.FeeFilterMsgBuilder fee(Long fee) {
            this.fee = fee;
            return this;
        }

        public FeeFilterMsg build() {
            return new FeeFilterMsg(fee, super.extraBytes, super.checksum);
        }
    }
}
