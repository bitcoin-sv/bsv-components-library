package io.bitcoinsv.jcl.net.protocol.wrapper;


import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.network.events.*;
import io.bitcoinsv.jcl.net.network.events.PeerDisconnectedEvent.DisconnectedReason;
import io.bitcoinsv.jcl.net.protocol.events.control.*;
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsg;
import io.bitcoinsv.jcl.net.protocol.messages.common.BodyMessage;
import io.bitcoinsv.jcl.net.protocol.messages.common.StreamRequest;
import io.bitcoinsv.jcl.tools.events.Event;
import io.bitcoinsv.jcl.tools.events.EventBus;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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
        private InetAddress address;
        private PeersBlacklistedEvent.BlacklistReason reason;
        private Optional<Duration> duration;

        public BlacklistPeerRequestBuilder(InetAddress peerAddress, PeersBlacklistedEvent.BlacklistReason reason, Optional<Duration> duration) {
            this.address = peerAddress;
            this.reason = reason;
            this.duration = duration;
        }

        public BlacklistPeerRequestBuilder(InetAddress peerAddress, PeersBlacklistedEvent.BlacklistReason reason, Duration duration) {
            this (peerAddress, reason, Optional.ofNullable(duration));
        }

        public BlacklistPeerRequestBuilder(InetAddress peerAddress, Duration duration) {
            this(peerAddress, PeersBlacklistedEvent.BlacklistReason.CLIENT, duration);
        }

        public BlacklistPeerRequestBuilder(InetAddress peerAddress, PeersBlacklistedEvent.BlacklistReason reason) {
            this(peerAddress, reason, reason.getExpirationTime());
        }

        public BlacklistPeerRequestBuilder(InetAddress peerAddress) {
            this.address = peerAddress;
            this.reason = PeersBlacklistedEvent.BlacklistReason.CLIENT;
            this.duration = PeersBlacklistedEvent.BlacklistReason.CLIENT.getExpirationTime();
        }

        public BlacklistPeerRequest buildRequest() {
            return new BlacklistPeerRequest(address, reason, duration);
        }
    }

    /** A Builder for WhitelistPeerRequest */
    public class WhitelistPeerRequestBuilder extends RequestBuilder {
        private InetAddress address;

        public WhitelistPeerRequestBuilder(InetAddress peerAddress) {
            this.address = peerAddress;
        }

        public WhitelistPeerRequest buildRequest() {
            return new WhitelistPeerRequest(address);
        }
    }

    /**
     * A Builder for RemovePeerFromWhitelistRequest
     */
    public class RemovePeerFromWhitelistRequestBuilder extends RequestBuilder {
        private final InetAddress address;

        public RemovePeerFromWhitelistRequestBuilder(PeerAddress peerAddress) {
            this.address = peerAddress.getIp();
        }

        public RemovePeerFromWhitelistRequestBuilder(InetAddress address) {
            this.address = address;
        }

        public RemovePeerFromWhitelistRequest buildRequest() {
            return new RemovePeerFromWhitelistRequest(address);
        }
    }

    /**
     * A Builder for ClearWhitelistRequest
     */
    public class ClearWhitelistRequestBuilder extends RequestBuilder {
        public ClearWhitelistRequest buildRequest() {
            return new ClearWhitelistRequest();
        }
    }

    /**
     * A Builder for RemovePeerFromBlacklistRequest
     */
    public class RemovePeerFromBlacklistRequestBuilder extends RequestBuilder {
        private final InetAddress address;

        public RemovePeerFromBlacklistRequestBuilder(PeerAddress peerAddress) {
            this.address = peerAddress.getIp();
        }

        public RemovePeerFromBlacklistRequestBuilder(InetAddress address) {
            this.address = address;
        }

        public RemovePeerFromBlacklistRequest buildRequest() {
            return new RemovePeerFromBlacklistRequest(address);
        }
    }

    /**
     * A Builder for ClearBlacklistRequest
     */
    public class ClearBlacklistRequestBuilder extends RequestBuilder {
        public ClearBlacklistRequest buildRequest() {
            return new ClearBlacklistRequest();
        }
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
        public BlacklistPeerRequestBuilder blacklist(PeerAddress peerAddress, PeersBlacklistedEvent.BlacklistReason reason) {
            return new BlacklistPeerRequestBuilder(peerAddress.getIp(), reason);
        }
        public BlacklistPeerRequestBuilder blacklist(PeerAddress peerAddress, Duration duration) {
            return new BlacklistPeerRequestBuilder(peerAddress.getIp(), duration);
        }
        public BlacklistPeerRequestBuilder blacklist(PeerAddress peerAddress) {
            return new BlacklistPeerRequestBuilder(peerAddress.getIp());
        }
        public BlacklistPeerRequestBuilder blacklist(InetAddress address, PeersBlacklistedEvent.BlacklistReason reason) {
            return new BlacklistPeerRequestBuilder(address, reason);
        }
        public BlacklistPeerRequestBuilder blacklist(InetAddress address,Duration duration) {
            return new BlacklistPeerRequestBuilder(address, duration);
        }
        public BlacklistPeerRequestBuilder blacklist(InetAddress address) {
            return new BlacklistPeerRequestBuilder(address);
        }
        public RemovePeerFromBlacklistRequestBuilder removeFromBlacklist(InetAddress address) {
            return new RemovePeerFromBlacklistRequestBuilder(address);
        }
        public ClearBlacklistRequestBuilder clearBlacklist() {
            return new ClearBlacklistRequestBuilder();
        }

        public EnablePeerBigMessagesRequestBuilder enableBigMessages(PeerAddress peerAddress) {
            return new EnablePeerBigMessagesRequestBuilder(peerAddress);
        }

        public WhitelistPeerRequestBuilder whitelist(InetAddress address) {
            return new WhitelistPeerRequestBuilder(address);
        }

        public RemovePeerFromWhitelistRequestBuilder removeFromWhitelist(InetAddress address) {
            return new RemovePeerFromWhitelistRequestBuilder(address);
        }

        public ClearWhitelistRequestBuilder clearWhitelist() { return new ClearWhitelistRequestBuilder();}

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
        public SendMsgHandshakedRequest buildRequest() { return new SendMsgHandshakedRequest(peerAddress, btcMsg); }
    }

    /** A Builder for SendMsgBodyRequest */
    public class SendMsgBodyRequestBuilder extends RequestBuilder {
        private PeerAddress peerAddress;
        private BodyMessage msgBody;

        public SendMsgBodyRequestBuilder(PeerAddress peerAddress, BodyMessage msgBody) {
            this.peerAddress = peerAddress;
            this.msgBody = msgBody;
        }
        public SendMsgBodyHandshakedRequest buildRequest() { return new SendMsgBodyHandshakedRequest(peerAddress, msgBody); }
    }

    /** A Builder for SendMsgListRequest */
    public class SendMsgListRequestBuilder extends RequestBuilder {
        private PeerAddress peerAddress;
        private List<BitcoinMsg<?>> btcMsgs;

        public SendMsgListRequestBuilder(PeerAddress peerAddress, List<BitcoinMsg<?>> btcMsgs) {
            this.peerAddress = peerAddress;
            this.btcMsgs = btcMsgs;
        }

        public SendMsgListHandshakeRequest buildRequest() { return new SendMsgListHandshakeRequest(peerAddress, btcMsgs); }
    }

    /** A Builder for SendMsgListRequest */
    public class SendMsgStreamHandshakeRequestBuilder extends RequestBuilder {
        private PeerAddress peerAddress;
        private StreamRequest streamRequest;

        public SendMsgStreamHandshakeRequestBuilder(PeerAddress peerAddress, StreamRequest streamRequest) {
            this.peerAddress = peerAddress;
            this.streamRequest = streamRequest;
        }

        public SendMsgStreamHandshakeRequest buildRequest() { return new SendMsgStreamHandshakeRequest(peerAddress, streamRequest); }
    }


    /** A Builder for BroadcastMsgRequest */
    public class BroadcastMsgRequestBuilder extends RequestBuilder {
        private BitcoinMsg<?> btcMsg;

        public BroadcastMsgRequestBuilder(BitcoinMsg<?> btcMsg) { this.btcMsg = btcMsg; }
        public BroadcastMsgRequest buildRequest()               { return new BroadcastMsgRequest(btcMsg); }
    }

    /** A Builder for BroadcastMsgBodyRequest */
    public class BroadcastMsgBodyRequestBuilder extends RequestBuilder {
        private BodyMessage msgBody;

        public BroadcastMsgBodyRequestBuilder(BodyMessage msgBody)  { this.msgBody = msgBody; }
        public BroadcastMsgBodyRequest buildRequest()           { return new BroadcastMsgBodyRequest(msgBody); }
    }

    /**
     * A convenience Class for Requests related to Message Operations
     */
    public class MsgsRequestBuilder {
        public SendMsgRequestBuilder send(PeerAddress peerAddress, BitcoinMsg<?> btcMsg) {
            return new SendMsgRequestBuilder(peerAddress, btcMsg);
        }
        public SendMsgBodyRequestBuilder send(PeerAddress peerAddress, BodyMessage msgBody) {
            return new SendMsgBodyRequestBuilder(peerAddress, msgBody);
        }
        public SendMsgListRequestBuilder send(PeerAddress peerAddress, List<BitcoinMsg<?>> btcMsgs) {
            return new SendMsgListRequestBuilder(peerAddress, btcMsgs);
        }
        public SendMsgStreamHandshakeRequestBuilder stream(PeerAddress peerAddress, StreamRequest streamRequest) {
            return new SendMsgStreamHandshakeRequestBuilder(peerAddress, streamRequest);
        }
        public BroadcastMsgRequestBuilder broadcast(BitcoinMsg<?> btcMsg) {
            return new BroadcastMsgRequestBuilder(btcMsg);
        }
        public BroadcastMsgBodyRequestBuilder broadcast(BodyMessage msgBody) {
            return new BroadcastMsgBodyRequestBuilder(msgBody);
        }
    }

    /**
     * A convenience Class for Requests related to Blocks Downloading
     */
    public class BlocksDownloadRequestBuilder {
        public BlocksToDownloadRequestBuilder download(String blockHash) {
            return new BlocksToDownloadRequestBuilder(List.of(blockHash), false);
        }
        public BlocksToDownloadRequestBuilder download(List<String> blockHashes) {
            return new BlocksToDownloadRequestBuilder(blockHashes, false);
        }
        public BlocksToDownloadRequestBuilder downloadWithPriority(String blockHash) {
            return new BlocksToDownloadRequestBuilder(List.of(blockHash), true);
        }
        public BlocksToDownloadRequestBuilder downloadWithPriority(List<String> blockHashes) {
            return new BlocksToDownloadRequestBuilder(blockHashes, true);
        }

        public BlocksToDownloadRequestBuilder forceDownload(String blockHash) {
            return new BlocksToDownloadRequestBuilder(List.of(blockHash), false).forceDownload();
        }
        public BlocksToDownloadRequestBuilder forceDownload(List<String> blockHashes) {
            return new BlocksToDownloadRequestBuilder(blockHashes, false).forceDownload();
        }

        public BlocksToDownloadRequestBuilder forceDownloadWithPriority(String blockHash) {
            return new BlocksToDownloadRequestBuilder(List.of(blockHash), true).forceDownload();
        }
        public BlocksToDownloadRequestBuilder forceDownloadWithPriority(List<String> blockHashes) {
            return new BlocksToDownloadRequestBuilder(blockHashes, true).forceDownload();
        }

        public BlocksToCancelDownloadRequestBuilder cancelDownload(String blockHash) {
            return new BlocksToCancelDownloadRequestBuilder(List.of(blockHash));
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
        private boolean forceDownload;
        private PeerAddress fromThisPeerOnly;
        private PeerAddress fromThisPeerPreferably;

        public BlocksToDownloadRequestBuilder(List<String> blockHash, boolean withPriority)   {
            this.blockHash = blockHash;
            this.withPriority = withPriority;
        }

        public BlocksToDownloadRequestBuilder forceDownload() {
            this.forceDownload = true;
            return this;
        }

        public BlocksToDownloadRequestBuilder fromThisPeerOnly(PeerAddress fromThisPeerOnly) {
            this.fromThisPeerOnly = fromThisPeerOnly;
            return this;
        }

        public BlocksToDownloadRequestBuilder fromThisPeerOnly(String fromThisPeerOnly) throws Exception {
            this.fromThisPeerOnly = PeerAddress.fromIp(fromThisPeerOnly);
            return this;
        }

        public BlocksToDownloadRequestBuilder fromThisPeerPreferably(PeerAddress fromThisPeerPreferably) {
            this.fromThisPeerPreferably = fromThisPeerPreferably;
            return this;
        }

        public BlocksToDownloadRequestBuilder fromThisPeerPreferably(String fromThisPeerPreferably) throws Exception {
            this.fromThisPeerPreferably = PeerAddress.fromIp(fromThisPeerPreferably);
            return this;
        }

        public BlocksDownloadRequest buildRequest() {
            return new BlocksDownloadRequest(blockHash, withPriority, forceDownload, fromThisPeerOnly, fromThisPeerPreferably);
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