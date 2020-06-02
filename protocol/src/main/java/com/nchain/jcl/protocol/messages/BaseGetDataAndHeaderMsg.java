package com.nchain.jcl.protocol.messages;

import com.google.common.collect.ImmutableList;
import com.nchain.jcl.protocol.messages.common.Message;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.List;

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 *
 * @date 16/09/2019
 *
 * It holds structure of the the GetHeaders and GetBlocks body message
 *
 * Structure of the BODY of Message:
 *
 *  - field: "version" (4 bytes)
 *  Identifies connection version being used by the node
 *
 *  - field: "hash count" (1+ bytes) VarIntMsg
 *  number of block locator hash entries
 *
 *   - field: "block locator hashest"  (32+ bytes)
 *   block locator object; newest back to genesis block (dense to start, but then sparse)
 *
 *   - field: "hash_stop"  (32 bytes)
 *   hash of the last desired block; set to zero to get as many blocks as possible (500)
 */
@Value
@EqualsAndHashCode(callSuper = true)
public  class BaseGetDataAndHeaderMsg extends Message {
    private long version;
    private VarIntMsg hashCount;
    private ImmutableList<HashMsg> blockLocatorHash;
    private HashMsg hashStop;
    public static final String MESSAGE_TYPE = "baseGetDataAndHeaderMsg";
    public static final int VERSION_LENGTH = 4;

    @Builder
    protected BaseGetDataAndHeaderMsg( long version, VarIntMsg hashCount, List<HashMsg>  blockLocatorHash, HashMsg hashStop) {
        this.version = version;
        this.hashCount = hashCount;
        this.blockLocatorHash = ImmutableList.copyOf(blockLocatorHash);
        this.hashStop = hashStop;
        init();
    }
    @Override
    protected long calculateLength() {
        long lengthInBytes  = VERSION_LENGTH + hashCount.getLengthInBytes() + blockLocatorHash.size()* HashMsg.HASH_LENGTH + HashMsg.HASH_LENGTH;
        return lengthInBytes;
    }
    @Override
    protected void validateMessage() {
    }

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }


}
