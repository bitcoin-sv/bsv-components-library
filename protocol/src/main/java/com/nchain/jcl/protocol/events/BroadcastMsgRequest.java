package com.nchain.jcl.protocol.events;

import com.nchain.jcl.protocol.messages.common.BitcoinMsg;
import com.nchain.jcl.tools.events.Event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-25 17:04
 *
 * An Event representing a Request to broadcast a Message to all the Peers we are connected to.
 */
@Value
@AllArgsConstructor
@Builder
public class BroadcastMsgRequest extends Event {
    private BitcoinMsg<?> btcMsg;
}
