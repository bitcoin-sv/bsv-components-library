package com.nchain.jcl.net.protocol.messages;

import com.nchain.jcl.net.protocol.messages.common.Message;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-18 12:44
 */
@Value
@EqualsAndHashCode
public class PartialBlockTXsMsg extends Message {

    public static final String MESSAGE_TYPE = "PartialBlockTxs";
    private BlockHeaderMsg blockHeader;
    private List<TxMsg> txs;

    @Builder
    public PartialBlockTXsMsg(BlockHeaderMsg blockHeader, List<TxMsg> txs) {
        this.blockHeader = blockHeader;
        this.txs = txs;
        init();
    }

    @Override
    protected long calculateLength() {
         return txs.stream().mapToLong(tx -> tx.getLengthInBytes()).sum();
    }

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }

    @Override
    protected void validateMessage() {
        if (txs == null || txs.size() == 0) throw new RuntimeException("The List of TXs is empty or null");
    }

}
