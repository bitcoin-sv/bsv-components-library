package com.nchain.jcl.net.protocol.messages;


import com.google.common.base.Objects;
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
public class FeeFilterMsg extends Message implements Serializable {

    public static final String MESSAGE_TYPE = "feefilter";

    private Long fee;

    public FeeFilterMsg(Long fee) {
        this.fee = fee;
        init();
    }

    @Override
    public String getMessageType() { return MESSAGE_TYPE; }

    @Override
    protected long calculateLength() {
        return 8;
    }

    @Override
    protected void validateMessage() {}

    public Long getFee() {
        return this.fee;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(fee);
    }

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

    /**
     * Builder
     */
    public static class FeeFilterMsgBuilder {
        private Long fee;

        FeeFilterMsgBuilder() {
        }

        public FeeFilterMsg.FeeFilterMsgBuilder fee(Long fee) {
            this.fee = fee;
            return this;
        }

        public FeeFilterMsg build() {
            return new FeeFilterMsg(fee);
        }
    }
}
