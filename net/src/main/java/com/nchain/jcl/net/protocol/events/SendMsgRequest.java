package com.nchain.jcl.net.protocol.events;

import com.nchain.jcl.base.tools.events.Event;
import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event representing a Request to send a Message to an specific Peer
 */
@Value
@AllArgsConstructor
@Builder
public class SendMsgRequest extends Event {
    private PeerAddress peerAddress;
    private BitcoinMsg<?> btcMsg;
}
