package io.bitcoinsv.bsvcl.net.protocol.events.data;

import com.google.common.base.Objects;

import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * @date 24/08/2021
 */
public final class RawTxsBatchMsgReceivedEvent extends MsgReceivedBatchEvent<RawTxMsgReceivedEvent> {
    public RawTxsBatchMsgReceivedEvent(List<RawTxMsgReceivedEvent> events) {
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
