package com.nchain.jcl.protocol.wrapper;

import com.nchain.jcl.network.PeerAddress;
import com.nchain.jcl.network.events.ConnectPeerRequest;
import com.nchain.jcl.network.events.DisconnectPeerRequest;
import com.nchain.jcl.protocol.events.BroadcastMsgRequest;
import com.nchain.jcl.protocol.events.DisablePingPongRequest;
import com.nchain.jcl.protocol.events.EnablePingPongRequest;
import com.nchain.jcl.protocol.events.SendMsgRequest;
import com.nchain.jcl.protocol.messages.VersionMsg;
import com.nchain.jcl.protocol.messages.common.BitcoinMsg;
import com.nchain.jcl.tools.events.Event;
import com.nchain.jcl.tools.events.EventBus;
import lombok.AllArgsConstructor;

import com.nchain.jcl.network.events.PeerDisconnectedEvent.DisconnectedReason;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-07-01 11:34
 *
 * A "Request" in this context is an Order that we want to execute. It could be to connect to a Peer, to
 * whitelist a Peer, or to send a message. Creating REQUESt is not a common operation, since all the
 * underlying protocol communications are handled automatically by the P2P class and the different Handlers
 * within. But in case you want to perform some fine-tuning and low-level operations yourself you can use them.
 *
 * Basically, issuing a Request means that a "Request" object is created and published to the EventBus, the same
 * bus used by the protocol. Those Requests will be picked up by those Internal handlers subscribed to them, and
 * they will do their job.
 */
@AllArgsConstructor
public class P2PRequestHandler {

    // The same EventBus that is used by the underlying P2P
    private EventBus eventBus;

    /**
     * A base class for a Request. Any Request will extend this class.
     */
    abstract class Request {
        // Any subclass must return an specific Request class in this method
        public abstract Event buildRequest();
        // This method publishes the Request to the Bus
        public void submit() {
            eventBus.publish(buildRequest());
        }
    }

    /** A Builder for ConnectPeerRequest */
    @AllArgsConstructor
    class ConnectPeerRequestBuilder extends Request {
        private PeerAddress peerAddress;
        public ConnectPeerRequest buildRequest() { return new ConnectPeerRequest(peerAddress); }
    }

    /** A Builder for DisconnectPeerRequest */
   @AllArgsConstructor
    class DisconnectPeerRequestBuilder extends Request {
        private PeerAddress peerAddress;
        private DisconnectedReason reason;
        public DisconnectPeerRequest buildRequest() { return new DisconnectPeerRequest(peerAddress, reason, null); }
    }

    /** A Builder for EnablePingPongRequest */
    @AllArgsConstructor
    class EnablePingPongRequestBuilder extends Request {
        private PeerAddress peerAddress;
        public EnablePingPongRequest buildRequest() { return new EnablePingPongRequest(peerAddress);}
    }

    /** A Builder for DisablePingPongRequest */
    @AllArgsConstructor
    class DisablePingPongRequestBuilder extends Request {
        private PeerAddress peerAddress;
        public DisablePingPongRequest buildRequest() { return new DisablePingPongRequest(peerAddress);}
    }

    /**
     * A convenience Class for Requests related to Peer operations
     */
    class PeersRequestBuilder  {
        public ConnectPeerRequestBuilder connect(PeerAddress peerAddress) {
            return new ConnectPeerRequestBuilder(peerAddress);
        }
        public DisconnectPeerRequestBuilder disconnect(PeerAddress peerAddress) {
            return new DisconnectPeerRequestBuilder(peerAddress, null);
        }
        public DisconnectPeerRequestBuilder disconnect(PeerAddress peerAddress, DisconnectedReason reason) {
            return new DisconnectPeerRequestBuilder(peerAddress, reason);
        }
        public EnablePingPongRequestBuilder enablePingPong(PeerAddress peerAddress) {
            return new EnablePingPongRequestBuilder(peerAddress);
        }
        public DisablePingPongRequestBuilder disablePingPong(PeerAddress peerAddress) {
            return new DisablePingPongRequestBuilder(peerAddress);
        }
    }

    /** A Builder for SendMsgRequest */
    @AllArgsConstructor
    class SendMsgRequestBuilder extends Request {
        private PeerAddress peerAddress;
        private BitcoinMsg<?> btcMsg;
        public SendMsgRequest buildRequest() { return new SendMsgRequest(peerAddress, btcMsg); }
    }
    /** A Builder for BroadcastMsgRequest */
    @AllArgsConstructor
    class BroadcastMsgRequestBuilder extends Request {
        private BitcoinMsg<?> btcMsg;
        public BroadcastMsgRequest buildRequest() { return new BroadcastMsgRequest(btcMsg); }
    }

    /**
     * A convenience Class for Request related to Message Operations
     */
    class MsgsRequestBuilder {
        public SendMsgRequestBuilder send(PeerAddress peerAddress, BitcoinMsg<?> btcMsg) {
            return new SendMsgRequestBuilder(peerAddress, btcMsg);
        }
        public BroadcastMsgRequestBuilder broadcast(BitcoinMsg<?> btcMsg) {
            return new BroadcastMsgRequestBuilder(btcMsg);
        }
    }

    // Definition of the built-in Request Handlers:
    public final PeersRequestBuilder PEERS = new PeersRequestBuilder();
    public final MsgsRequestBuilder MSGS = new MsgsRequestBuilder();

}
