package com.nchain.jcl.protocol.messages;


import com.nchain.jcl.protocol.messages.common.Message;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import javax.print.attribute.standard.MediaSize;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-04-09 12:34
 *
 * The feeFilter message is sent when a Peer wants to announce that is only interested in those Transactions
 * which Fee is equals or higher than the one especified in this message
 * This message has been defined in BIP 0133:
 * @see <a href="https://github.com/bitcoin/bips/blob/master/bip-0133.mediawiki">BIP 0133</a>
 *
 * Structure of the Message:
 * - fee: A long value indicating the Fee (in Satoshis / KB)
 */
@Getter
@ToString
@EqualsAndHashCode(callSuper = true)
public class FeeFilterMsg extends Message {

    public static final String MESSAGE_TYPE = "feefilter";

    private Long fee;

    @Builder
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
}
