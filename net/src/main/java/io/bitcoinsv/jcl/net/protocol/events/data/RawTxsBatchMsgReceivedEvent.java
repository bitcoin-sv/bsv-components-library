package io.bitcoinsv.jcl.net.protocol.events.data;

import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * @date 24/08/2021
 */
public class RawTxsBatchMsgReceivedEvent extends MsgReceivedBatchEvent<RawTxMsgReceivedEvent> {
    public RawTxsBatchMsgReceivedEvent(List<RawTxMsgReceivedEvent> events) {
        super(events);
    }
}
