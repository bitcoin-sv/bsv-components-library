package com.nchain.jcl.protocol.messages;

import com.nchain.jcl.protocol.messages.common.Message;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;


/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2019-07-16
 *
 * A VarStrMSg is not a fully Bitcoin Message itself, but it's a structure that is reused by different other
 *  messages in the Bitcoin Protocol. It represents a Variable Length String, so we only store as many bytes
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
@Value
@EqualsAndHashCode
public final class VarStrMsg extends Message {

    public static final String MESSAGE_TYPE = "varStr";

    private VarIntMsg strLength;
    private String str;

    @Builder
    protected VarStrMsg(String str) {
        this.str = str;
        this.strLength = VarIntMsg.builder().value(str.length()).build();
        init();
    }

    @Override
    public String getMessageType() {return MESSAGE_TYPE;}

    @Override
    protected long calculateLength() {
        long length =  strLength.getLengthInBytes() + str.length();;
        return length;
    }

    @Override
    protected void validateMessage() {}
}
