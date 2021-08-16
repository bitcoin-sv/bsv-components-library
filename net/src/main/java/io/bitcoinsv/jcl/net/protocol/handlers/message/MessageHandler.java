package io.bitcoinsv.jcl.net.protocol.handlers.message;

import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsg;
import io.bitcoinsv.jcl.net.protocol.messages.common.Message;
import io.bitcoinsv.jcl.tools.handlers.Handler;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
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

    /** Sends a Message to an specific Peer */
    void send(PeerAddress peerAddress, Message msgBody);

    /** Broadcasts a Message to all connected Peers */
    void broadcast(BitcoinMsg<?> btcMessage);

    /** Broadcasts a Message to all connected Peers */
    void broadcast(Message msgBody);
}
