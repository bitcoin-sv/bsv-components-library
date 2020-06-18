package com.nchain.jcl.protocol.messages;

import com.nchain.jcl.protocol.messages.common.Message;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;


/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2019-07-14 20:43
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
@Value
@EqualsAndHashCode
public final class HeaderMsg extends Message {

    public static final String MESSAGE_TYPE = "header";
    // The HeaderMsg always has a length of 24 Bytes
    public static final long MESSAGE_LENGTH = 24;

    private final long magic;
    private final String command;
    private final long length;
    private final long checksum;

    // Constructor. to create instance  of this class, use the Builder
    @Builder
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

    protected void validateMessage() {

    }

    @Override
    public String getMessageType() { return MESSAGE_TYPE; }
}
