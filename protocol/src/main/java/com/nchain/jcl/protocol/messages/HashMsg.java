package com.nchain.jcl.protocol.messages;

import com.google.common.base.Preconditions;
import com.nchain.jcl.protocol.messages.common.Message;
import com.nchain.jcl.tools.bytes.HEX;
import lombok.*;

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 18/09/2019
 *
 * A HashMsg is not a fully Bitcoin Message itself, but it's a structure that is reused by different other
 * messages in the Bitcoin Protocol. It represents a char array that store as many bytes
 * to represent hash of the object.
 */
@Data
@EqualsAndHashCode
public class HashMsg extends Message {
    public static final String MESSAGE_TYPE = "hash";
    public static final int HASH_LENGTH = 32;//32 bits

    private final byte[] hashBytes;

  @Builder
    protected HashMsg(byte[] hash) {
        this.hashBytes = hash;
        init();
    }

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }


    @Override
    protected long calculateLength() {
        long length = this.hashBytes.length;
        return length;
    }

    @Override
    protected void validateMessage() {
        Preconditions.checkArgument( HASH_LENGTH == this.hashBytes.length,"Hash Length is not 32 bits");
    }

    @Override
    public String toString() { return HEX.encode(hashBytes);}

}
