package com.nchain.jcl.protocol.handlers.message;

import com.nchain.jcl.network.PeerAddress;
import com.nchain.jcl.protocol.messages.common.BitcoinMsg;
import com.nchain.jcl.tools.handlers.Handler;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-25 15:47
 *
 * A Message Handler is responsible for the Deserialization and publishing of the incoming Messages from
 * remote Peers, and also for the Serialization and Sending of Messages to them.
 *
 * Since the reception of Messages is notified through callbacks in the EventBus, this interface only
 * provides methods to send out messages.
 */
public interface MessageHandler extends Handler {

    String HANDLER_ID = "Message-Handler";

    @Override
    default String getId() { return HANDLER_ID; }

    /** Sends a Message to an specific Peer */
    void send(PeerAddress peerAddress, BitcoinMsg<?> btcMessage);
    /** Broadcasts a Message to all connected Peers */
    void broadcast(BitcoinMsg<?> btcMessage);
}
