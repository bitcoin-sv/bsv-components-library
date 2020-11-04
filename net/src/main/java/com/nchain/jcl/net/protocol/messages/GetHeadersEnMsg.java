package com.nchain.jcl.net.protocol.messages;

import com.nchain.jcl.net.protocol.messages.common.Message;
import lombok.Builder;
import lombok.Value;

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * The getheadersen message is an upgrade to the existing getheaders message.This message returns same data as
 * getheaders message with the addition of fields for actual number of transactions that are included in the block
 * and proof of inclussion for coinbase transaction along with the whole coinbase transaction.
 */
@Value
public final class GetHeadersEnMsg extends Message {
    public static final String MESSAGE_TYPE = "getheadersen";
    private final long version;
    private final HashMsg blockLocatorHash;
    private final HashMsg hashStop;
    public static final int VERSION_LENGTH = 4;

    @Builder
    public GetHeadersEnMsg(long version, HashMsg blockLocatorHash, HashMsg hashStop) {
        this.version = version;
        this.blockLocatorHash = blockLocatorHash;
        this.hashStop = hashStop;
        init();
    }

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }

    @Override
    protected long calculateLength() {
        long length = VERSION_LENGTH +   HashMsg.HASH_LENGTH + HashMsg.HASH_LENGTH;
        return length;
    }

    @Override
    protected void validateMessage() {}



}
