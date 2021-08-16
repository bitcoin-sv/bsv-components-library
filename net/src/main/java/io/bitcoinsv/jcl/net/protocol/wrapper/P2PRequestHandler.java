package io.bitcoinsv.jcl.net.protocol.wrapper;


import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.network.events.*;
import io.bitcoinsv.jcl.net.network.events.PeerDisconnectedEvent.DisconnectedReason;
import io.bitcoinsv.jcl.net.protocol.events.control.*;
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsg;
import io.bitcoinsv.jcl.net.protocol.messages.common.Message;
import io.bitcoinsv.jcl.tools.events.Event;
import io.bitcoinsv.jcl.tools.events.EventBus;

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
public class P2PRequestHandler {

    // The same EventBus that is used by the underlying P2P
    private EventBus eventBus;


    public P2PRequestHandler(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    /**
     * A base class for a Request Builder. Any Request Builder will extend this class.
     */
    abstract class RequestBuilder {
        // Any subclass must return an specific Request class in this method
        public abstract Event buildRequest();
        // This method publishes the Request to the Bus
        public void submit() {
             eventBus.publish(buildRequest());
        }
    }

    /** A Builder for ConnectPeerRequest */
    public class ConnectPeerRequestBuilder extends RequestBuilder {
        private PeerAddress peerAddress;

        public ConnectPeerRequestBuilder(PeerAddress peerAddress)   { this.peerAddress = peerAddress; }
        public ConnectPeerRequest buildRequest()                    { return new ConnectPeerRequest(peerAddress); }
    }

    /** A Builder for DisconnectPeerRequest */
   public  class DisconnectPeerRequestBuilder extends RequestBuilder {
        private PeerAddress peerAddress;
        private DisconnectedReason reason;

        public DisconnectPeerRequestBuilder(PeerAddress peerAddress, DisconnectedReason reason) {
            this.peerAddress = peerAddress;
            this.reason = reason;
        }
        public DisconnectPeerRequest buildRequest() { return new DisconnectPeerRequest(peerAddress, reason, null); }
    }

    /** A Builder for EnablePingPongRequest */
    public class EnablePingPongRequestBuilder extends RequestBuilder {
        private PeerAddress peerAddress;

        public EnablePingPongRequestBuilder(PeerAddress peerAddress) { this.peerAddress = peerAddress; }
        public EnablePingPongRequest buildRequest()                  { return new EnablePingPongRequest(peerAddress);}
    }

    /** A Builder for DisablePingPongRequest */
    public class DisablePingPongRequestBuilder extends RequestBuilder {
        private PeerAddress peerAddress;

        public DisablePingPongRequestBuilder(PeerAddress peerAddress)   { this.peerAddress = peerAddress; }
        public DisablePingPongRequest buildRequest()                    { return new DisablePingPongRequest(peerAddress);}
    }

    /** A Builder for BlacklistPeerRequest */
    public class BlacklistPeerRequestBuilder extends RequestBuilder {
        private PeerAddress peerAddress;

        public BlacklistPeerRequestBuilder(PeerAddress peerAddress) { this.peerAddress = peerAddress; }
        public BlacklistPeerRequest buildRequest()                  { return new BlacklistPeerRequest(peerAddress);}
    }

    /** A builder for EnablePeerForBigMessagesRequest */
    public class EnablePeerBigMessagesRequestBuilder extends RequestBuilder {
        private PeerAddress peerAddress;

        public EnablePeerBigMessagesRequestBuilder(PeerAddress peerAddress)  { this.peerAddress = peerAddress;}
        public EnablePeerBigMessagesRequest buildRequest()                   { return new EnablePeerBigMessagesRequest(peerAddress);}
    }

    /** A builder for DisablePeerForBigMessagesRequest */
    public class DisablePeerBigMessagesRequestBuilder extends RequestBuilder {
        private PeerAddress peerAddress;

        public DisablePeerBigMessagesRequestBuilder(PeerAddress peerAddress)  { this.peerAddress = peerAddress;}
        public DisablePeerBigMessagesRequest buildRequest()                   { return new DisablePeerBigMessagesRequest(peerAddress);}
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
        public EnablePeerBigMessagesRequestBuilder enableBigMessages(PeerAddress peerAddress) {
            return new EnablePeerBigMessagesRequestBuilder(peerAddress);
        }
        public EnablePeerBigMessagesRequestBuilder enableBigMessages(String peerAddressStr) {
            try {
                return new EnablePeerBigMessagesRequestBuilder(PeerAddress.fromIp(peerAddressStr));
            } catch (UnknownHostException e) { throw new RuntimeException(e); }
        }
        public DisablePeerBigMessagesRequestBuilder disableBigMessages(PeerAddress peerAddress) {
            return new DisablePeerBigMessagesRequestBuilder(peerAddress);
        }
        public DisablePeerBigMessagesRequestBuilder disableBigMessages(String peerAddressStr) {
            try {
                return new DisablePeerBigMessagesRequestBuilder(PeerAddress.fromIp(peerAddressStr));
            } catch (UnknownHostException e) { throw new RuntimeException(e); }
        }
    }

    /** A Builder for SendMsgRequest */
    public class SendMsgRequestBuilder extends RequestBuilder {
        private PeerAddress peerAddress;
        private BitcoinMsg<?> btcMsg;

        public SendMsgRequestBuilder(PeerAddress peerAddress, BitcoinMsg<?> btcMsg) {
            this.peerAddress = peerAddress;
            this.btcMsg = btcMsg;
        }
        public SendMsgRequest buildRequest() { return new SendMsgRequest(peerAddress, btcMsg); }
    }

    /** A Builder for SendMsgBodyRequest */
    public class SendMsgBodyRequestBuilder extends RequestBuilder {
        private PeerAddress peerAddress;
        private Message msgBody;

        public SendMsgBodyRequestBuilder(PeerAddress peerAddress, Message msgBody) {
            this.peerAddress = peerAddress;
            this.msgBody = msgBody;
        }
        public SendMsgBodyRequest buildRequest() { return new SendMsgBodyRequest(peerAddress, msgBody); }
    }

    /** A Builder for SendMsgListRequest */
    public class SendMsgListRequestBuilder extends RequestBuilder {
        private PeerAddress peerAddress;
        private List<BitcoinMsg<?>> btcMsgs;

        public SendMsgListRequestBuilder(PeerAddress peerAddress, List<BitcoinMsg<?>> btcMsgs) {
            this.peerAddress = peerAddress;
            this.btcMsgs = btcMsgs;
        }

        public SendMsgListRequest buildRequest() { return new SendMsgListRequest(peerAddress, btcMsgs); }
    }


    /** A Builder for BroadcastMsgRequest */
    public class BroadcastMsgRequestBuilder extends RequestBuilder {
        private BitcoinMsg<?> btcMsg;

        public BroadcastMsgRequestBuilder(BitcoinMsg<?> btcMsg) { this.btcMsg = btcMsg; }
        public BroadcastMsgRequest buildRequest()               { return new BroadcastMsgRequest(btcMsg); }
    }

    /** A Builder for BroadcastMsgBodyRequest */
    public class BroadcastMsgBodyRequestBuilder extends RequestBuilder {
        private Message msgBody;

        public BroadcastMsgBodyRequestBuilder(Message msgBody)  { this.msgBody = msgBody; }
        public BroadcastMsgBodyRequest buildRequest()           { return new BroadcastMsgBodyRequest(msgBody); }
    }

    /**
     * A convenience Class for Requests related to Message Operations
     */
    public class MsgsRequestBuilder {
        public SendMsgRequestBuilder send(PeerAddress peerAddress, BitcoinMsg<?> btcMsg) {
            return new SendMsgRequestBuilder(peerAddress, btcMsg);
        }
        public SendMsgBodyRequestBuilder send(PeerAddress peerAddress, Message msgBody) {
            return new SendMsgBodyRequestBuilder(peerAddress, msgBody);
        }
        public SendMsgListRequestBuilder send(PeerAddress peerAddress, List<BitcoinMsg<?>> btcMsgs) {
            return new SendMsgListRequestBuilder(peerAddress, btcMsgs);
        }
        public BroadcastMsgRequestBuilder broadcast(BitcoinMsg<?> btcMsg) {
            return new BroadcastMsgRequestBuilder(btcMsg);
        }
        public BroadcastMsgBodyRequestBuilder broadcast(Message msgBody) {
            return new BroadcastMsgBodyRequestBuilder(msgBody);
        }
    }

    /**
     * A convenience Class for Requests related to Blocks Downloading
     */
    public class BlocksDownloadRequestBuilder {
        public BlocksToDownloadRequestBuilder download(String blockHash) {
            return new BlocksToDownloadRequestBuilder(Arrays.asList(blockHash), false);
        }
        public BlocksToDownloadRequestBuilder download(List<String> blockHashes) {
            return new BlocksToDownloadRequestBuilder(blockHashes, false);
        }
        public BlocksToDownloadRequestBuilder downloadWithPriority(String blockHash) {
            return new BlocksToDownloadRequestBuilder(Arrays.asList(blockHash), true);
        }
        public BlocksToDownloadRequestBuilder downloadWithPriority(List<String> blockHashes) {
            return new BlocksToDownloadRequestBuilder(blockHashes, true);
        }
        public BlocksToCancelDownloadRequestBuilder cancelDownload(String blockHash) {
            return new BlocksToCancelDownloadRequestBuilder(Arrays.asList(blockHash));
        }
        public BlocksToCancelDownloadRequestBuilder cancelDownload(List<String> blockHashes) {
            return new BlocksToCancelDownloadRequestBuilder(blockHashes);
        }
        public BlocksDownloadStartRequestBuilder resume() {
            return new BlocksDownloadStartRequestBuilder();
        }
        public BlocksDownloadPauseRequestBuilder pause() {
            return new BlocksDownloadPauseRequestBuilder();
        }
    }

    /**
     * A Builder for Requests to Download Blocks
     */
    public class BlocksToDownloadRequestBuilder extends RequestBuilder {
        private List<String> blockHash;
        private boolean withPriority;

        public BlocksToDownloadRequestBuilder(List<String> blockHash, boolean withPriority)   {
            this.blockHash = blockHash;
            this.withPriority = withPriority;
        }
        public BlocksDownloadRequest buildRequest() {
            return new BlocksDownloadRequest(blockHash, withPriority);
        }
    }


    /**
     * A Builder for Requests to Cancel the Download of Blocks
     */
    public class BlocksToCancelDownloadRequestBuilder extends RequestBuilder {
        private List<String> blockHash;

        public BlocksToCancelDownloadRequestBuilder(List<String> blockHash)   { this.blockHash = blockHash; }
        public BlocksCancelDownloadRequest buildRequest()                     { return new BlocksCancelDownloadRequest(blockHash); }
    }

    /**
     * A Builder for Requests to Resume the Download process
     */
    public class BlocksDownloadStartRequestBuilder extends RequestBuilder {
        public BlocksDownloadStartRequestBuilder() {}
        public BlocksDownloadStartRequest buildRequest() { return new BlocksDownloadStartRequest();}
    }

    /**
     * A Builder for Requests to Resume the Download process
     */
    public class BlocksDownloadPauseRequestBuilder extends RequestBuilder {
        public BlocksDownloadPauseRequestBuilder() {}
        public BlocksDownloadPauseRequest buildRequest() { return new BlocksDownloadPauseRequest();}
    }


    // Definition of the built-in Request Handlers:
    public final PeersRequestBuilder            PEERS   = new PeersRequestBuilder();
    public final MsgsRequestBuilder             MSGS    = new MsgsRequestBuilder();
    public final BlocksDownloadRequestBuilder   BLOCKS  = new BlocksDownloadRequestBuilder();

}
