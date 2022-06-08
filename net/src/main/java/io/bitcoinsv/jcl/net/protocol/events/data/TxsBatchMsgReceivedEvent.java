package io.bitcoinsv.jcl.net.protocol.events.data;

import com.google.common.base.Objects;

import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * @date 24/08/2021
 */
public class TxsBatchMsgReceivedEvent extends MsgReceivedBatchEvent<TxMsgReceivedEvent> {
    public TxsBatchMsgReceivedEvent(List<TxMsgReceivedEvent> events) {
        super(events);
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode());
    }
}
