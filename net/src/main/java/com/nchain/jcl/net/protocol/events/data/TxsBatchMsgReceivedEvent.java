package com.nchain.jcl.net.protocol.events.data;

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
}
