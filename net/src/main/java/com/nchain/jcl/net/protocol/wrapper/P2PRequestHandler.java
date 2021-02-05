package com.nchain.jcl.net.protocol.wrapper;


import com.nchain.jcl.net.network.events.BlacklistPeerRequest;
import com.nchain.jcl.net.protocol.events.*;
import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.net.network.events.ConnectPeerRequest;
import com.nchain.jcl.net.network.events.DisconnectPeerRequest;
import com.nchain.jcl.net.network.events.PeerDisconnectedEvent.DisconnectedReason;
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg;
import com.nchain.jcl.tools.events.Event;
import com.nchain.jcl.tools.events.EventBus;
import lombok.AllArgsConstructor;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
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
    public class ConnectPeerRequestBuilder extends Request {
        private PeerAddress peerAddress;
        public ConnectPeerRequest buildRequest() { return new ConnectPeerRequest(peerAddress); }
    }

    /** A Builder for DisconnectPeerRequest */
   @AllArgsConstructor
   public  class DisconnectPeerRequestBuilder extends Request {
        private PeerAddress peerAddress;
        private DisconnectedReason reason;
        public DisconnectPeerRequest buildRequest() { return new DisconnectPeerRequest(peerAddress, reason, null); }
    }

    /** A Builder for EnablePingPongRequest */
    @AllArgsConstructor
    public class EnablePingPongRequestBuilder extends Request {
        private PeerAddress peerAddress;
        public EnablePingPongRequest buildRequest() { return new EnablePingPongRequest(peerAddress);}
    }

    /** A Builder for DisablePingPongRequest */
    @AllArgsConstructor
    public class DisablePingPongRequestBuilder extends Request {
        private PeerAddress peerAddress;
        public DisablePingPongRequest buildRequest() { return new DisablePingPongRequest(peerAddress);}
    }

    /** A Builder for BlacklistPeerRequest */
    @AllArgsConstructor
    public class BlacklistPeerRequestBuilder extends Request {
        private PeerAddress peerAddress;
        public BlacklistPeerRequest buildRequest() { return new BlacklistPeerRequest(peerAddress);}
    }

    /**
     * A convenience Class for Requests related to Peer operations
     */
    public class PeersRequestBuilder  {
        public ConnectPeerRequestBuilder connect(String peerAddressStr) {
            try {
                return new ConnectPeerRequestBuilder(PeerAddress.fromIp(peerAddressStr));
            } catch (UnknownHostException e) { throw new RuntimeException(e); }
        }
        public ConnectPeerRequestBuilder connect(PeerAddress peerAddress) {
            return new ConnectPeerRequestBuilder(peerAddress);
        }
        public DisconnectPeerRequestBuilder disconnect(PeerAddress peerAddress) {
            return new DisconnectPeerRequestBuilder(peerAddress, null);
        }
        public DisconnectPeerRequestBuilder disconnect(String peerAddressStr) {
            try {
                return new DisconnectPeerRequestBuilder(PeerAddress.fromIp(peerAddressStr), null);
            } catch (UnknownHostException e) { throw new RuntimeException(e); }
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
        public BlacklistPeerRequestBuilder blacklist(PeerAddress peerAddress) {
            return new BlacklistPeerRequestBuilder(peerAddress);
        }
    }

    /** A Builder for SendMsgRequest */
    @AllArgsConstructor
    public class SendMsgRequestBuilder extends Request {
        private PeerAddress peerAddress;
        private BitcoinMsg<?> btcMsg;
        public SendMsgRequest buildRequest() { return new SendMsgRequest(peerAddress, btcMsg); }
    }
    /** A Builder for BroadcastMsgRequest */
    @AllArgsConstructor
    public class BroadcastMsgRequestBuilder extends Request {
        private BitcoinMsg<?> btcMsg;
        public BroadcastMsgRequest buildRequest() { return new BroadcastMsgRequest(btcMsg); }
    }

    /**
     * A convenience Class for Request related to Message Operations
     */
    public class MsgsRequestBuilder {
        public SendMsgRequestBuilder send(PeerAddress peerAddress, BitcoinMsg<?> btcMsg) {
            return new SendMsgRequestBuilder(peerAddress, btcMsg);
        }
        public BroadcastMsgRequestBuilder broadcast(BitcoinMsg<?> btcMsg) {
            return new BroadcastMsgRequestBuilder(btcMsg);
        }
    }


    @AllArgsConstructor
    public class BlockDownloadRequestBuilder extends Request {
        private List<String> blockHash;
        public BlocksDownloadRequest buildRequest() { return new BlocksDownloadRequest(blockHash); }
    }


    /**
     * A convenience Class for Request related to the Block Downloader Handler
     */
    public class BlockDownloaderRequestBuilder {
        public BlockDownloadRequestBuilder download(String blockHash) {
            return new BlockDownloadRequestBuilder(Arrays.asList(blockHash));
        }
        public BlockDownloadRequestBuilder download(List<String> blockHashes) {
            return new BlockDownloadRequestBuilder(blockHashes);
        }
    }

    // Definition of the built-in Request Handlers:
    public final PeersRequestBuilder            PEERS   = new PeersRequestBuilder();
    public final MsgsRequestBuilder             MSGS    = new MsgsRequestBuilder();
    public final BlockDownloaderRequestBuilder  BLOCKS  = new BlockDownloaderRequestBuilder();

}
