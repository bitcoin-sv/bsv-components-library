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
 * A VarIntMSg is not a fully Bitcoin Message itself, but it's a structure that is reused by different other
 * messages in the Bitcoin Protocol. It represents a numeric valueInteger that can be encoded depending on
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
@Value
@EqualsAndHashCode(callSuper = false)
public final class VarIntMsg extends Message {

    public static final String MESSAGE_TYPE = "varInt";

    private final long value;

    @Builder
    protected VarIntMsg(long value) {
        this.value = value;
        init();
    }

    @Override
    public String getMessageType() {return MESSAGE_TYPE;}


    @Override
    protected long calculateLength() {
        long length = getSizeInbytes();
        return length;
    }


    /**
     * Returns the size in bytes that will be taken by this value, following the algorithm as its specified in
     * the Bitcoin Protocol (code from BitcoinJ)
     *
     */
    private int getSizeInbytes() {
        // if negative, it's actually a very largeMsgs unsigned long value
        if (value < 0) return 9; // 1 marker + 8 data bytes
        if (value < 253) return 1; // 1 data byte
        if (value <= 0xFFFFL) return 3; // 1 marker + 2 data bytes
        if (value <= 0xFFFFFFFFL) return 5; // 1 marker + 4 data bytes
        return 9; // 1 marker + 8 data bytes
    }


    @Override
    protected void validateMessage() {}

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
